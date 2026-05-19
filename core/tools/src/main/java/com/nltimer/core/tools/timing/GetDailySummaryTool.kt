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
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * 工具：获取指定日期的行为统计汇总
 *
 * 按活动分组计算总时长，返回结构化 JSON 供 AI Agent 一次性回答
 * "今天各活动花了多少时间"类问题，无需多轮查询。
 *
 * 返回示例：
 * ```json
 * {
 *   "date": "2026-05-19",
 *   "totalDurationMinutes": 320,
 *   "activities": [
 *     {"activityId":7, "activityName":"本职工作", "iconKey":"💼", "durationMinutes":240, "behaviorCount":3}
 *   ],
 *   "activeBehavior": { ... }
 * }
 * ```
 */
@Singleton
class GetDailySummaryTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "getDailySummary"
    override val description: String =
        "获取指定日期的行为统计汇总，按活动分组计算总时长；默认今天"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "date",
            description = "日期，格式 YYYY-MM-DD；默认今天",
            type = ParameterType.STRING,
            required = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return runCatching {
            val dateStr = (args["date"] as? String)?.takeIf { it.isNotBlank() }
            val date = if (dateStr != null) {
                LocalDate.parse(dateStr)
            } else {
                LocalDate.now()
            }

            val zone = ZoneId.systemDefault()
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            // 查询当天所有行为（含详情）
            val behaviors = behaviorRepository.getBehaviorsWithDetailsByTimeRangeSync(dayStart, dayEnd)

            // 查询当前活跃行为
            val activeBehavior = behaviorRepository.getCurrentBehavior().first()
            val activeWithDetails = activeBehavior?.let { ab ->
                behaviorRepository.getBehaviorWithDetails(ab.id)
            }

            // 按活动分组统计
            val now = System.currentTimeMillis()
            val groupedStats = mutableMapOf<Long, ActivityStats>()

            for (bwd in behaviors) {
                val b = bwd.behavior
                if (b.status == BehaviorNature.PENDING) continue

                val end = b.endTime
                val durationMs = if (b.status == BehaviorNature.COMPLETED && end != null) {
                    end - b.startTime
                } else {
                    now - b.startTime
                }
                val durationMinutes = (durationMs / 60_000).toInt().coerceAtLeast(0)

                val existing = groupedStats[b.activityId]
                if (existing != null) {
                    groupedStats[b.activityId] = existing.copy(
                        durationMinutes = existing.durationMinutes + durationMinutes,
                        behaviorCount = existing.behaviorCount + 1,
                    )
                } else {
                    groupedStats[b.activityId] = ActivityStats(
                        activityId = b.activityId,
                        activityName = bwd.activity.name,
                        iconKey = bwd.activity.iconKey,
                        durationMinutes = durationMinutes,
                        behaviorCount = 1,
                    )
                }
            }

            // 构建 JSON
            val activitiesArr = JSONArray()
            val sortedStats = groupedStats.values.sortedByDescending { it.durationMinutes }
            var totalDurationMinutes = 0

            for (stat in sortedStats) {
                val obj = JSONObject()
                obj.put("activityId", stat.activityId)
                obj.put("activityName", stat.activityName)
                obj.put("iconKey", stat.iconKey ?: JSONObject.NULL)
                obj.put("durationMinutes", stat.durationMinutes)
                obj.put("behaviorCount", stat.behaviorCount)
                activitiesArr.put(obj)
                totalDurationMinutes += stat.durationMinutes
            }

            val result = JSONObject()
            result.put("date", date.toString())
            result.put("totalDurationMinutes", totalDurationMinutes)
            result.put("activities", activitiesArr)

            // 添加当前活跃行为
            if (activeWithDetails != null) {
                val ab = activeWithDetails.behavior
                val activeDurationMinutes = ((now - ab.startTime) / 60_000).toInt().coerceAtLeast(0)
                val activeObj = JSONObject()
                activeObj.put("id", ab.id)
                activeObj.put("activityId", ab.activityId)
                activeObj.put("activityName", activeWithDetails.activity.name)
                activeObj.put("startTime", TimeUtils.formatIso(ab.startTime))
                activeObj.put("durationMinutes", activeDurationMinutes)
                activeObj.put("note", ab.note ?: JSONObject.NULL)
                val tagsArr = JSONArray()
                activeWithDetails.tags.forEach { tag -> tagsArr.put(tag.name) }
                activeObj.put("tags", tagsArr)
                result.put("activeBehavior", activeObj)
            }

            ToolResult.Success(name, result.toString())
        }.getOrElse { e ->
            ToolResult.Error(
                name = name,
                error = ToolError.InternalError(e.message ?: "获取日结统计失败"),
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
            {
              "date": "2026-05-19",
              "totalDurationMinutes": 320,
              "activities": [
                {"activityId": 7, "activityName": "本职工作", "iconKey": "💼", "durationMinutes": 240, "behaviorCount": 3},
                {"activityId": 4, "activityName": "主动学习", "iconKey": "📖", "durationMinutes": 80, "behaviorCount": 2}
              ],
              "activeBehavior": {
                "id": 42, "activityId": 7, "activityName": "本职工作",
                "startTime": "2026-05-19T14:00:00+08:00", "durationMinutes": 30, "note": null, "tags": []
              }
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample(
                code = "INTERNAL_ERROR",
                message = "获取日结统计失败",
                scenario = "数据库读取异常",
            ),
        ),
        usageExamples = listOf(
            "getDailySummary()  // 默认今天",
            "getDailySummary(date=\"2026-05-19\")",
        ),
    )

    private data class ActivityStats(
        val activityId: Long,
        val activityName: String,
        val iconKey: String?,
        val durationMinutes: Int,
        val behaviorCount: Int,
    )
}
