package com.nltimer.core.tools.timing

import com.nltimer.core.data.model.BehaviorNature
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
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class GetTimeRangeSummaryTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "getTimeRangeSummary"
    override val description: String = "查询任意时间区间的行为统计：总时长、按活动分组、完成率、计划vs实际对比"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "startTime",
            description = "起始时间 ISO 8601",
            type = ParameterType.STRING,
            required = true,
        ),
        ToolParameter(
            name = "endTime",
            description = "结束时间 ISO 8601",
            type = ParameterType.STRING,
            required = true,
        ),
    )

    override val returnType: KClass<*> = String::class

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val startStr = args["startTime"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("startTime 必填"))
        val endStr = args["endTime"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("endTime 必填"))

        val startMs = TimeUtils.parseIsoToMillis(startStr)
            ?: return ToolResult.Error(name, ToolError.ValidationError("startTime 格式错误: $startStr"))
        val endMs = TimeUtils.parseIsoToMillis(endStr)
            ?: return ToolResult.Error(name, ToolError.ValidationError("endTime 格式错误: $endStr"))

        if (endMs <= startMs) {
            return ToolResult.Error(name, ToolError.ValidationError("endTime 必须晚于 startTime"))
        }

        val rangeDays = ((endMs - startMs) / (24 * 60 * 60 * 1000))
        if (rangeDays > 93) {
            return ToolResult.Error(name, ToolError.ValidationError("查询范围不能超过 93 天"))
        }

        return runCatching {
            val behaviors = behaviorRepository.getBehaviorsWithDetailsByTimeRangeSync(startMs, endMs)
            val now = System.currentTimeMillis()

            var totalMinutes = 0
            var completedCount = 0
            var activeCount = 0
            var pendingCount = 0
            var plannedCount = 0
            var totalEstimatedMinutes = 0
            var totalActualMinutes = 0
            val activityMap = mutableMapOf<Long, ActivityAgg>()

            for (bwd in behaviors) {
                val b = bwd.behavior
                when (b.status) {
                    BehaviorNature.PENDING -> { pendingCount++; continue }
                    BehaviorNature.ACTIVE -> activeCount++
                    BehaviorNature.COMPLETED -> completedCount++
                }

                val end = b.endTime ?: now
                val durationMinutes = ((end - b.startTime) / 60_000).toInt().coerceAtLeast(0)
                totalMinutes += durationMinutes

                if (b.wasPlanned) plannedCount++
                b.estimatedDuration?.let { totalEstimatedMinutes += (it / 60_000).toInt() }
                b.actualDuration?.let { totalActualMinutes += (it / 60_000).toInt() }

                val agg = activityMap.getOrPut(b.activityId) {
                    ActivityAgg(
                        activityId = b.activityId,
                        activityName = bwd.activity.name,
                        iconKey = bwd.activity.iconKey,
                    )
                }
                agg.durationMinutes += durationMinutes
                agg.count++
                if (b.wasPlanned) agg.plannedCount++
                b.achievementLevel?.let { agg.achievementSum += it; agg.achievementCount++ }
            }

            val activityArr = JSONArray()
            for (agg in activityMap.values.sortedByDescending { it.durationMinutes }) {
                val avgAchievement = if (agg.achievementCount > 0) {
                    String.format(Locale.US, "%.1f", agg.achievementSum.toDouble() / agg.achievementCount)
                } else null
                activityArr.put(JSONObject().apply {
                    put("activityId", agg.activityId)
                    put("activityName", agg.activityName)
                    put("iconKey", agg.iconKey ?: JSONObject.NULL)
                    put("durationMinutes", agg.durationMinutes)
                    put("count", agg.count)
                    put("plannedCount", agg.plannedCount)
                    put("avgAchievement", avgAchievement ?: JSONObject.NULL)
                })
            }

            val completionRate = if (completedCount + activeCount > 0) {
                String.format(Locale.US, "%.0f%%", completedCount.toDouble() / (completedCount + activeCount) * 100)
            } else "0%"

            val result = JSONObject().apply {
                put("startTime", TimeUtils.formatIso(startMs))
                put("endTime", TimeUtils.formatIso(endMs))
                put("totalMinutes", totalMinutes)
                put("totalHours", String.format(Locale.US, "%.1f", totalMinutes / 60.0))
                put("completedCount", completedCount)
                put("activeCount", activeCount)
                put("pendingCount", pendingCount)
                put("plannedCount", plannedCount)
                put("completionRate", completionRate)
                put("totalEstimatedMinutes", totalEstimatedMinutes)
                put("totalActualMinutes", totalActualMinutes)
                put("planAdherenceRate", if (totalEstimatedMinutes > 0) {
                    String.format(Locale.US, "%.0f%%", totalActualMinutes.toDouble() / totalEstimatedMinutes * 100)
                } else JSONObject.NULL)
                put("activities", activityArr)
            }

            ToolResult.Success(name, result.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "获取时间区间统计失败"))
        }
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """
            {
              "startTime": "2026-05-12T00:00:00+08:00",
              "endTime": "2026-05-19T23:59:59+08:00",
              "totalMinutes": 1920, "totalHours": "32.0",
              "completedCount": 24, "activeCount": 1, "pendingCount": 3, "plannedCount": 15,
              "completionRate": "96%",
              "totalEstimatedMinutes": 2100, "totalActualMinutes": 1920,
              "planAdherenceRate": "91%",
              "activities": [
                {"activityId":7,"activityName":"本职工作","iconKey":"💼","durationMinutes":1200,"count":8,"plannedCount":6,"avgAchievement":"4.2"}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("VALIDATION_ERROR", "查询范围不能超过 93 天", "时间区间过大"),
        ),
        usageExamples = listOf(
            """getTimeRangeSummary(startTime="2026-05-12T00:00:00+08:00", endTime="2026-05-19T23:59:59+08:00")""",
        ),
    )

    private class ActivityAgg(
        val activityId: Long,
        val activityName: String,
        val iconKey: String?,
        var durationMinutes: Int = 0,
        var count: Int = 0,
        var plannedCount: Int = 0,
        var achievementSum: Int = 0,
        var achievementCount: Int = 0,
    )
}
