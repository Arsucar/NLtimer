package com.nltimer.app.experimental.ai_inter.viewmodel

import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolRegistry
import com.nltimer.core.tools.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ToolCallBuffer {
    var id: String? = null
    var name: String? = null
    val arguments: StringBuilder = StringBuilder()
}

object AiChatToolHelper {

    private val json = Json { ignoreUnknownKeys = true }

    const val MAX_TOOL_ROUNDS = 5

    const val TOOLS_SYSTEM_PROMPT = """# 工具调用规则（强制）

## 1. 时间解析
- "10 点到 14 点" → 当天本地时区 10:00-14:00，ISO 8601 含时区偏移
- "15 点午休 30 分钟" → 起 15:00 止 15:30
- "现在结束计时" / "结束" → 直接调 endBehavior()，不要传时间参数

## 2. 默认值兜底（不要主动追问）
- 用户没指定活动分类时，createActivity 默认 groupName="预制菜"
- 用户没指定标签分类时，createTag 默认 category="预制菜"
- 颜色缺失时工具内自动生成莫奈中和色，无需问用户
- 标签图标缺失时默认 "#"

## 3. 多步序列分支
当用户说"先做 X，再做 Y"或一连串任务时：
1. 先 queryCurrentBehavior 看是否有 ACTIVE
2. 有 ACTIVE → 所有任务都走 createGoal（按顺序排队）
3. 无 ACTIVE → 第 1 个用 startBehavior 立即开始，剩下走 createGoal

## 4. 冲突处理
当 recordBehavior 返回 ValidationError 且 message 是 JSON 含 "code":"CONFLICT" 时：
1. 解析 message 里的 conflicts 数组（每项含 id / activityName / startTime / endTime）
2. 向用户复述冲突区间，给出三个选项：
   - 覆盖：对每个 conflict.id 调 deleteBehavior(id)，然后重发 recordBehavior
   - 取消：不做任何动作，告诉用户已取消
   - 调整时间：让用户给新时间，再发 recordBehavior

## 5. "查看所有标签"
- 默认 listTags() 返回未归档标签
- 用户明确说"包括归档"时才传 includeArchived=true

## 6. 统计与总结场景
- 用户问"今天/昨天做了什么" → getDailySummary()，不要用 listBehaviors
- 用户问"这周总结/周报" → getWeeklySummary()
- 用户问"最近 N 天/本月/某段时间" → getTimeRangeSummary(startTime, endTime)
- 用户问某个行为详情 → getBehaviorDetail(id)

## 7. 目标管理
- 用户说"看看待办/目标列表" → listGoals()
- 用户说"开始第 X 个任务" → activateGoal(id)（id 来自 listGoals 返回值）
- 用户说"取消某个目标" → deleteGoal(id)
- 用户说"调换顺序" → reorderGoals(orderedIds)
- 激活目标会自动结束当前 ACTIVE 行为，无需先调 endBehavior

## 8. 修改已有记录
- 用户说"改备注/改时间/加标签" → updateBehavior(id, ...)
- 注意：tagIds 是整体替换，不是追加；如需保留原有标签，先通过 listBehaviors 或 getBehaviorDetail 查出原有 tagIds 再合并

## 9. 查询决策优先级
- 状态查询：queryCurrentBehavior → listGoals
- 记录查询：listBehaviors（列表） / getBehaviorDetail（单条详情）
- 统计查询：getDailySummary（单日） / getWeeklySummary（周） / getTimeRangeSummary（自定义区间）

## 10. 图标选择
- 创建/更新活动或标签需要 iconKey 时，先调 searchIcons(query="关键词") 搜索可用图标
- iconKey 格式：hi:xxx（HugeIcons 推荐） / mi:filled:xxx（Material Icons） / emoji 原始字符
- 不要瞎猜 iconKey，必须通过 searchIcons 搜索确认后再使用
- 示例：用户说"睡觉图标" → searchIcons(query="睡觉") → 从结果中选合适的 iconKey
"""

    fun buildAssistantToolMessage(
        content: String,
        toolBuffers: Map<Int, ToolCallBuffer>,
    ): JsonObject = buildJsonObject {
        put("role", "assistant")
        put("content", content)
        putJsonArray("tool_calls") {
            toolBuffers.toSortedMap().forEach { (_, buf) ->
                add(buildJsonObject {
                    put("id", buf.id ?: "")
                    put("type", "function")
                    putJsonObject("function") {
                        put("name", buf.name ?: "")
                        put("arguments", buf.arguments.toString())
                    }
                })
            }
        }
    }

    fun buildToolResultMessage(record: ToolCallRecord): JsonObject = buildJsonObject {
        put("role", "tool")
        put("tool_call_id", record.id)
        put("name", record.name)
        put("content", record.result)
    }

    suspend fun executeToolCall(
        buf: ToolCallBuffer,
        toolRegistry: ToolRegistry,
    ): ToolCallRecord {
        val name = buf.name.orEmpty()
        val argsStr = buf.arguments.toString()
        val id = buf.id ?: "call_${System.nanoTime()}"
        val started = System.currentTimeMillis()
        val argsMap = parseToolArguments(argsStr)
        val result = if (name.isBlank()) {
            ToolResult.Error(
                name = name,
                error = ToolError.ValidationError("模型未提供工具名"),
            )
        } else {
            toolRegistry.executeTool(name, argsMap)
        }
        val durationMs = System.currentTimeMillis() - started
        val (success, resultStr) = when (result) {
            is ToolResult.Success -> true to (result.data?.toString() ?: "null")
            is ToolResult.Error -> false to "[${result.error::class.simpleName}] ${result.error.message}"
        }
        return ToolCallRecord(
            id = id,
            name = name,
            arguments = argsStr,
            result = resultStr,
            success = success,
            durationMs = durationMs,
        )
    }

    fun parseToolArguments(argsStr: String): Map<String, Any?> {
        if (argsStr.isBlank()) return emptyMap()
        return try {
            val element = json.parseToJsonElement(argsStr)
            if (element !is JsonObject) return emptyMap()
            element.mapValues { (_, v) -> convertJsonElement(v) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun convertJsonElement(element: Any?): Any? = when (element) {
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.content == "true" -> true
            element.content == "false" -> false
            else -> element.content.toLongOrNull() ?: element.content.toDoubleOrNull() ?: element.content
        }
        is JsonArray -> element.map { convertJsonElement(it) }
        is JsonObject -> element.mapValues { (_, v) -> convertJsonElement(v) }
        else -> element?.toString()
    }

    fun serializeToolCalls(records: List<ToolCallRecord>): String {
        if (records.isEmpty()) return ""
        val array = buildJsonArray {
            records.forEach { rec ->
                add(buildJsonObject {
                    put("id", rec.id)
                    put("name", rec.name)
                    put("arguments", rec.arguments)
                    put("result", rec.result)
                    put("success", rec.success)
                    put("durationMs", rec.durationMs)
                })
            }
        }
        return json.encodeToString(JsonArray.serializer(), array)
    }
}
