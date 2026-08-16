package com.nltimer.core.tools.timing

import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.tools.AccessLevel
import com.nltimer.core.tools.ErrorExample
import com.nltimer.core.tools.ParameterType
import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolDocumentation
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolParameter
import com.nltimer.core.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import org.json.JSONArray
import org.json.JSONObject

/**
 * 工具：按时间范围查询行为记录
 *
 * 返回精简摘要列表，不暴露 pomodoroCount / sequence / estimatedDuration 等内部字段，
 * 减少 AI token 消耗。默认查询今天。
 */
@Singleton
class ListBehaviorsTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "listBehaviors"
    override val description: String =
        "按时间范围查询行为记录（默认今天），返回精简摘要列表"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "startTime",
            description = "起始时间 ISO 8601（缺省今天 00:00）",
            type = ParameterType.STRING,
            required = false,
        ),
        ToolParameter(
            name = "endTime",
            description = "结束时间 ISO 8601（缺省今天 23:59:59）",
            type = ParameterType.STRING,
            required = false,
        ),
    )

    override val returnType: KClass<*> = Any::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val startStr = args["startTime"] as? String
        val endStr = args["endTime"] as? String

        val startMs = if (startStr != null) {
            TimeUtils.parseIsoToMillis(startStr)
                ?: return ToolResult.Error(
                    name,
                    ToolError.ValidationError("startTime 格式错误: $startStr"),
                )
        } else {
            TimeUtils.todayStartMillis()
        }

        val endMs = if (endStr != null) {
            TimeUtils.parseIsoToMillis(endStr)
                ?: return ToolResult.Error(
                    name,
                    ToolError.ValidationError("endTime 格式错误: $endStr"),
                )
        } else {
            TimeUtils.todayEndMillis()
        }

        // 查询范围上限 31 天
        if (endMs <= startMs) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("endTime 必须晚于 startTime"),
            )
        }
        val rangeDays = ((endMs - startMs) / (24 * 60 * 60 * 1000))
        if (rangeDays > 31) {
            return ToolResult.Error(
                name,
                ToolError.ValidationError("查询范围不能超过 31 天（当前约 ${rangeDays}天）"),
            )
        }

        return runCatching {
            val behaviors = behaviorRepository.getBehaviorsWithDetailsByTimeRangeSync(startMs, endMs)
            val arr = JSONArray()
            val now = System.currentTimeMillis()

            for (bd in behaviors) {
                val b = bd.behavior
                val a = bd.activity
                val end = b.endTime
                val durationMs = if (end != null) {
                    end - b.startTime
                } else {
                    now - b.startTime
                }
                val durationMinutes = (durationMs / 60_000).toInt().coerceAtLeast(0)

                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("activityId", a.id)
                    put("activityName", a.name)
                    put("iconKey", a.iconKey ?: "")
                    put("startTime", TimeUtils.formatIso(b.startTime))
                    put("endTime", b.endTime?.let { TimeUtils.formatIso(it) } ?: JSONObject.NULL)
                    put("durationMinutes", durationMinutes)
                    put("status", b.status.name)
                    put("note", b.note ?: "")
                }

                val tagsArr = JSONArray()
                bd.tags.forEach { tag -> tagsArr.put(tag.name) }
                obj.put("tags", tagsArr)

                arr.put(obj)
            }

            ToolResult.Success(name, arr.toString())
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "查询行为列表失败"),
            )
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            [
              {
                "id": 42,
                "activityId": 7,
                "activityName": "本职工作",
                "iconKey": "💼",
                "startTime": "2026-05-19T09:00:00+08:00",
                "endTime": "2026-05-19T12:30:00+08:00",
                "durationMinutes": 210,
                "status": "COMPLETED",
                "note": "需求评审",
                "tags": ["后端", "重点"]
              }
            ]
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "VALIDATION_ERROR",
                message = "查询范围不能超过 31 天",
                scenario = "时间范围超出限制",
            ),
        ),
        usageExamples = listOf(
            "listBehaviors()  // 查询今天",
            "listBehaviors(startTime=\"2026-05-19T00:00:00+08:00\", endTime=\"2026-05-19T23:59:59+08:00\")",
        ),
    )
}
