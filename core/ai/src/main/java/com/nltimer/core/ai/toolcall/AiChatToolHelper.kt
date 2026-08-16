package com.nltimer.core.ai.toolcall

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
- 颜色缺失时工具内自动生成莫奈中和色，**不要手动计算 ARGB 值，不传 color 即可**
- 标签图标缺失时默认 "#"

## 3. 上下文推断（重要）
- **分组推断**：如果用户刚创建了分组（如"生活"、"次要工作"），紧接着创建活动时，应自动推断该活动属于刚创建的分组，使用 groupName 参数指定
- **示例**：用户说"创建活动分组：生活"然后说"创建休息活动" → createActivity(name="休息", groupName="生活")
- 不要把活动放到默认分组，除非用户明确指定或上下文无法推断

## 4. 多步序列分支
当用户说"先做 X，再做 Y"或一连串任务时：
1. 先 queryCurrentBehavior 看是否有 ACTIVE
2. 有 ACTIVE → 所有任务都走 createGoal（按顺序排队）
3. 无 ACTIVE → 第 1 个用 startBehavior 立即开始，剩下走 createGoal

## 5. 冲突处理
当 recordBehavior 返回 ValidationError 且 message 是 JSON 含 "code":"CONFLICT" 时：
1. 解析 message 里的 conflicts 数组（每项含 id / activityName / startTime / endTime）
2. 向用户复述冲突区间，给出三个选项：
   - 覆盖：对每个 conflict.id 调 deleteBehavior(id)，然后重发 recordBehavior
   - 取消：不做任何动作，告诉用户已取消
   - 调整时间：让用户给新时间，再发 recordBehavior

## 6. "查看所有标签"
- 默认 listTags() 返回未归档标签
- 用户明确说"包括归档"时才传 includeArchived=true

## 7. 统计与总结场景
- 用户问"今天/昨天做了什么" → getDailySummary()，不要用 listBehaviors
- 用户问"这周总结/周报" → getWeeklySummary()
- 用户问"最近 N 天/本月/某段时间" → getTimeRangeSummary(startTime, endTime)
- 用户问某个行为详情 → getBehaviorDetail(id)

## 8. 目标管理
- 用户说"看看待办/目标列表" → listGoals()
- 用户说"开始第 X 个任务" → activateGoal(id)（id 来自 listGoals 返回值）
- 用户说"取消某个目标" → deleteGoal(id)
- 用户说"调换顺序" → reorderGoals(orderedIds)
- 激活目标会自动结束当前 ACTIVE 行为，无需先调 endBehavior

## 9. 修改已有记录
- 用户说"改备注/改时间/加标签" → updateBehavior(id, ...)
- 注意：tagIds 是整体替换，不是追加；如需保留原有标签，先通过 listBehaviors 或 getBehaviorDetail 查出原有 tagIds 再合并

## 10. 查询决策优先级
- 状态查询：queryCurrentBehavior → listGoals
- 记录查询：listBehaviors（列表） / getBehaviorDetail（单条详情）
- 统计查询：getDailySummary（单日） / getWeeklySummary（周） / getTimeRangeSummary（自定义区间）

## 11. 图标选择（重要）
- **创建活动/标签时，autoIcon 默认为 true，工具会自动根据名称匹配图标，无需手动调 searchIcons**
- 只有不传 iconKey 且 autoIcon=true 时才自动匹配；如果手动指定了 iconKey 则使用指定的
- **颜色也一样，不传 color 会自动生成莫奈中和色，不要手动传 ARGB 值**
- 批量创建的标准流程：
  1. batchCreateActivityCategories(names=[...])
  2. batchCreateActivities(activities=[...])  ← autoIcon=true 自动匹配图标和颜色
  3. batchCreateTags(tags=[...])              ← autoIcon=true 自动匹配图标和颜色
- 单个创建也支持自动图标：createActivity(name="休息") 会自动匹配休息相关图标
- 只在用户明确要求"换图标"或需要精确控制图标时，才调 searchIcons / batchSearchIcons
- batchSearchIcons 可一次搜索多个关键词：batchSearchIcons(queries=["code","review","sleep"])

## 12. 活动更新
- 使用 bulkUpdateActivities(updates=[...]) 可批量修改活动的 groupId、groupName、iconKey、color、name
- **支持 groupName 参数**：可以直接传 groupName 而不需要先查 groupId，系统会自动查找或创建分组
- 示例：bulkUpdateActivities(updates=[{"id":1,"groupName":"生活"}]) 会将活动移动到"生活"分组

## 13. 标签更新
- 使用 bulkUpdateTags(updates=[...]) 可批量修改标签的 iconKey、color、name、category
- 与 bulkUpdateActivities 对称，仅更新传入的字段
"""

    fun buildAssistantToolMessage(
        content: String,
        toolBuffers: Map<Int, ToolCallBuffer>,
    ): JsonObject = buildJsonObject {
        put("role", "assistant")
        put("content", content)
        putJsonArray("tool_calls") {
            toolBuffers.toSortedMap().forEach { (_, buf) ->
                // 模型未下发 id 时，在此统一分配并写回 buffer，
                // 保证 assistant tool_calls 的 id 与后续 tool 回执的 tool_call_id 一致。
                val callId = buf.id ?: "call_${System.nanoTime()}".also { buf.id = it }
                add(buildJsonObject {
                    put("id", callId)
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
