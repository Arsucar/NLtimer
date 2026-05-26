package com.nltimer.feature.stats.model

import com.nltimer.core.data.model.ActivityStat
import com.nltimer.core.data.model.StatsDashboardConfig
import com.nltimer.core.data.model.StatsResult
import com.nltimer.core.data.model.StatsTimeRange
import com.nltimer.core.data.model.StatsTimeRangeType
import com.nltimer.core.data.model.defaultStatsDashboardConfig

data class StatsUiState(
    val dashboardConfig: StatsDashboardConfig = defaultStatsDashboardConfig(),
    val statsResult: StatsResult? = null,
    val isLoading: Boolean = false,
    val currentTimeRange: StatsTimeRange = StatsTimeRange(StatsTimeRangeType.WEEK),
    val isEditMode: Boolean = false,
    val selectedActivity: ActivityStat? = null,
)
