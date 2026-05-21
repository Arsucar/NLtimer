package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class StatsResult(
    val timeRangeStartMs: Long,
    val timeRangeEndMs: Long,
    val totalMinutes: Int,
    val totalHours: String,
    val completedCount: Int,
    val activeCount: Int,
    val pendingCount: Int,
    val plannedCount: Int,
    val completionRate: String,
    val totalEstimatedMinutes: Int,
    val totalActualMinutes: Int,
    val planAdherenceRate: String?,
    val activityStats: ImmutableList<ActivityStat>,
    val dailyBreakdown: ImmutableList<DailyBreakdown>,
    val tagStats: ImmutableList<TagStat>,
)

@Immutable
data class ActivityStat(
    val activityId: Long,
    val activityName: String,
    val iconKey: String?,
    val durationMinutes: Int,
    val count: Int,
    val plannedCount: Int,
    val avgAchievement: String?,
)

@Immutable
data class DailyBreakdown(
    val date: String,
    val dayOfWeek: String,
    val totalMinutes: Int,
    val activities: ImmutableList<ActivityDailyItem>,
)

@Immutable
data class ActivityDailyItem(
    val activityId: Long,
    val activityName: String,
    val iconKey: String?,
    val durationMinutes: Int,
)

@Immutable
data class TagStat(
    val tagId: Long,
    val tagName: String,
    val iconKey: String?,
    val durationMinutes: Int,
    val count: Int,
)
