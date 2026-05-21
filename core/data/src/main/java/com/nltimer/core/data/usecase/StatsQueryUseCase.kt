package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.ActivityDailyItem
import com.nltimer.core.data.model.ActivityStat
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.DailyBreakdown
import com.nltimer.core.data.model.StatsResult
import com.nltimer.core.data.model.TagStat
import com.nltimer.core.data.repository.BehaviorRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsQueryUseCase @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
) {

    suspend fun query(startTimeMs: Long, endTimeMs: Long): StatsResult {
        val behaviors = behaviorRepository.getBehaviorsWithDetailsByTimeRangeSync(startTimeMs, endTimeMs)
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()

        var totalMinutes = 0
        var completedCount = 0
        var activeCount = 0
        var pendingCount = 0
        var plannedCount = 0
        var totalEstimatedMinutes = 0
        var totalActualMinutes = 0

        val activityAggMap = mutableMapOf<Long, ActivityAgg>()
        val dailyMap = mutableMapOf<String, MutableList<DailyItem>>()
        val tagAggMap = mutableMapOf<Long, TagAgg>()

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

            val actAgg = activityAggMap.getOrPut(b.activityId) {
                ActivityAgg(
                    activityId = b.activityId,
                    activityName = bwd.activity.name,
                    iconKey = bwd.activity.iconKey,
                )
            }
            actAgg.durationMinutes += durationMinutes
            actAgg.count++
            if (b.wasPlanned) actAgg.plannedCount++
            b.achievementLevel?.let {
                actAgg.achievementSum += it
                actAgg.achievementCount++
            }

            val dayDate = Instant.ofEpochMilli(b.startTime).atZone(zone).toLocalDate()
            val dateKey = dayDate.toString()
            dailyMap.getOrPut(dateKey) { mutableListOf() }
                .add(DailyItem(b.activityId, bwd.activity.name, bwd.activity.iconKey, durationMinutes, dayDate.dayOfWeek.name))

            for (tag in bwd.tags) {
                val tagAgg = tagAggMap.getOrPut(tag.id) {
                    TagAgg(tagId = tag.id, tagName = tag.name, iconKey = tag.iconKey)
                }
                tagAgg.durationMinutes += durationMinutes
                tagAgg.count++
            }
        }

        val activityStats = activityAggMap.values
            .sortedByDescending { it.durationMinutes }
            .map { agg ->
                ActivityStat(
                    activityId = agg.activityId,
                    activityName = agg.activityName,
                    iconKey = agg.iconKey,
                    durationMinutes = agg.durationMinutes,
                    count = agg.count,
                    plannedCount = agg.plannedCount,
                    avgAchievement = if (agg.achievementCount > 0) {
                        String.format("%.1f", agg.achievementSum.toDouble() / agg.achievementCount)
                    } else null,
                )
            }.toImmutableList()

        val dailyBreakdown = buildDailyBreakdown(startTimeMs, endTimeMs, dailyMap, zone)

        val tagStats = tagAggMap.values
            .sortedByDescending { it.durationMinutes }
            .map { agg ->
                TagStat(
                    tagId = agg.tagId,
                    tagName = agg.tagName,
                    iconKey = agg.iconKey,
                    durationMinutes = agg.durationMinutes,
                    count = agg.count,
                )
            }.toImmutableList()

        val completionRate = if (completedCount + activeCount > 0) {
            String.format("%.0f%%", completedCount.toDouble() / (completedCount + activeCount) * 100)
        } else "0%"

        val planAdherenceRate = if (totalEstimatedMinutes > 0) {
            String.format("%.0f%%", totalActualMinutes.toDouble() / totalEstimatedMinutes * 100)
        } else null

        return StatsResult(
            timeRangeStartMs = startTimeMs,
            timeRangeEndMs = endTimeMs,
            totalMinutes = totalMinutes,
            totalHours = String.format("%.1f", totalMinutes / 60.0),
            completedCount = completedCount,
            activeCount = activeCount,
            pendingCount = pendingCount,
            plannedCount = plannedCount,
            completionRate = completionRate,
            totalEstimatedMinutes = totalEstimatedMinutes,
            totalActualMinutes = totalActualMinutes,
            planAdherenceRate = planAdherenceRate,
            activityStats = activityStats,
            dailyBreakdown = dailyBreakdown,
            tagStats = tagStats,
        )
    }

    private fun buildDailyBreakdown(
        startTimeMs: Long,
        endTimeMs: Long,
        dailyMap: Map<String, MutableList<DailyItem>>,
        zone: ZoneId,
    ): ImmutableList<DailyBreakdown> {
        val startDate = Instant.ofEpochMilli(startTimeMs).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTimeMs).atZone(zone).toLocalDate()
        val result = mutableListOf<DailyBreakdown>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            val dateStr = current.toString()
            val items = dailyMap[dateStr] ?: emptyList()
            val activityItems = items
                .groupBy { it.activityId }
                .map { group ->
                    ActivityDailyItem(
                        activityId = group.value.first().activityId,
                        activityName = group.value.first().activityName,
                        iconKey = group.value.first().iconKey,
                        durationMinutes = group.value.sumOf { it.durationMinutes },
                    )
                }
                .sortedByDescending { it.durationMinutes }
                .toImmutableList()

            result.add(
                DailyBreakdown(
                    date = dateStr,
                    dayOfWeek = current.dayOfWeek.name,
                    totalMinutes = items.sumOf { it.durationMinutes },
                    activities = activityItems,
                )
            )
            current = current.plusDays(1)
        }
        return result.toImmutableList()
    }

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

    private class TagAgg(
        val tagId: Long,
        val tagName: String,
        val iconKey: String?,
        var durationMinutes: Int = 0,
        var count: Int = 0,
    )

    private class DailyItem(
        val activityId: Long,
        val activityName: String,
        val iconKey: String?,
        val durationMinutes: Int,
        val dayOfWeek: String,
    )
}
