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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class GetWeeklySummaryTool @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) : ToolDefinition {

    override val name: String = "getWeeklySummary"
    override val description: String = "获取指定周的行为统计汇总（按天+按活动分组），含每日明细与周总计；默认本周"
    override val category: ToolCategory = ToolCategory.TIMING
    override val accessLevel: AccessLevel = AccessLevel.READ
    override val returnType: KClass<*> = String::class

    override val parameters: List<ToolParameter> = listOf(
        ToolParameter(
            name = "date",
            description = "周内任意日期，格式 YYYY-MM-DD；默认本周",
            type = ParameterType.STRING,
            required = false,
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val dateStr = (args["date"] as? String)?.takeIf { it.isNotBlank() }
        val refDate = if (dateStr != null) {
            try {
                LocalDate.parse(dateStr)
            } catch (_: java.time.format.DateTimeParseException) {
                return ToolResult.Error(name, ToolError.ValidationError("date 格式错误，应为 YYYY-MM-DD: $dateStr"))
            }
        } else {
            LocalDate.now()
        }
        val weekStart = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)

        return runCatching {
            val zone = ZoneId.systemDefault()
            val startMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMs = weekEnd.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            val behaviors = behaviorRepository.getBehaviorsWithDetailsByTimeRangeSync(startMs, endMs)
            val now = System.currentTimeMillis()

            val dailyMap = mutableMapOf<LocalDate, MutableList<DayBehavior>>()
            val activityTotals = mutableMapOf<Long, ActivityTotal>()

            for (bwd in behaviors) {
                val b = bwd.behavior
                if (b.status == com.nltimer.core.data.model.BehaviorNature.PENDING) continue
                val end = b.endTime
                val durationMs = if (end != null) end - b.startTime else now - b.startTime
                val durationMinutes = (durationMs / 60_000).toInt().coerceAtLeast(0)
                val day = java.time.Instant.ofEpochMilli(b.startTime)
                    .atZone(zone).toLocalDate()

                dailyMap.getOrPut(day) { mutableListOf() }
                    .add(DayBehavior(b.activityId, bwd.activity.name, bwd.activity.iconKey, durationMinutes))

                activityTotals[b.activityId] = activityTotals[b.activityId]?.let {
                    it.copy(
                        durationMinutes = it.durationMinutes + durationMinutes,
                        count = it.count + 1,
                    )
                } ?: ActivityTotal(
                    activityId = b.activityId,
                    activityName = bwd.activity.name,
                    iconKey = bwd.activity.iconKey,
                    durationMinutes = durationMinutes,
                    count = 1,
                )
            }

            val daysArr = JSONArray()
            var weekTotalMinutes = 0
            for (d in (0..6)) {
                val day = weekStart.plusDays(d.toLong())
                val dayItems = dailyMap[day] ?: emptyList()
                var dayTotal = 0
                val itemsArr = JSONArray()
                for (item in dayItems) {
                    itemsArr.put(JSONObject().apply {
                        put("activityId", item.activityId)
                        put("activityName", item.activityName)
                        put("iconKey", item.iconKey ?: JSONObject.NULL)
                        put("durationMinutes", item.durationMinutes)
                    })
                    dayTotal += item.durationMinutes
                }
                daysArr.put(JSONObject().apply {
                    put("date", day.toString())
                    put("dayOfWeek", day.dayOfWeek.name)
                    put("totalMinutes", dayTotal)
                    put("activities", itemsArr)
                })
                weekTotalMinutes += dayTotal
            }

            val activityArr = JSONArray()
            for (at in activityTotals.values.sortedByDescending { it.durationMinutes }) {
                activityArr.put(JSONObject().apply {
                    put("activityId", at.activityId)
                    put("activityName", at.activityName)
                    put("iconKey", at.iconKey ?: JSONObject.NULL)
                    put("durationMinutes", at.durationMinutes)
                    put("count", at.count)
                })
            }

            val result = JSONObject().apply {
                put("weekStart", weekStart.toString())
                put("weekEnd", weekEnd.toString())
                put("totalMinutes", weekTotalMinutes)
                put("totalHours", String.format(Locale.US, "%.1f", weekTotalMinutes / 60.0))
                put("dailyBreakdown", daysArr)
                put("activityBreakdown", activityArr)
            }

            ToolResult.Success(name, result.toString())
        }.getOrElse { e ->
            ToolResult.Error(name, ToolError.InternalError(e.message ?: "获取周统计失败"))
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
              "weekStart": "2026-05-18", "weekEnd": "2026-05-24",
              "totalMinutes": 1920, "totalHours": "32.0",
              "dailyBreakdown": [
                {"date":"2026-05-18","dayOfWeek":"MONDAY","totalMinutes":320,"activities":[...]},
                {"date":"2026-05-19","dayOfWeek":"TUESDAY","totalMinutes":280,"activities":[...]}
              ],
              "activityBreakdown": [
                {"activityId":7,"activityName":"本职工作","iconKey":"💼","durationMinutes":1200,"count":8}
              ]
            }
        """.trimIndent(),
        errorExamples = listOf(
            ErrorExample("INTERNAL_ERROR", "获取周统计失败", "数据库读取异常"),
        ),
        usageExamples = listOf(
            "getWeeklySummary()",
            "getWeeklySummary(date=\"2026-05-19\")",
        ),
    )

    private data class DayBehavior(
        val activityId: Long,
        val activityName: String,
        val iconKey: String?,
        val durationMinutes: Int,
    )

    private data class ActivityTotal(
        val activityId: Long,
        val activityName: String,
        val iconKey: String?,
        val durationMinutes: Int,
        val count: Int,
    )
}
