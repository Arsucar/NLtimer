package com.nltimer.feature.stats.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.ActivityStat
import com.nltimer.core.data.model.StatsDashboardConfig
import com.nltimer.core.data.model.StatsPanelConfig
import com.nltimer.core.data.model.StatsPanelType
import com.nltimer.core.data.model.StatsResult
import com.nltimer.core.data.model.StatsTimeRange
import com.nltimer.core.data.model.StatsTimeRangeType
import com.nltimer.core.data.model.defaultStatsDashboardConfig
import com.nltimer.core.data.usecase.StatsQueryUseCase
import com.nltimer.feature.stats.model.StatsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsQueryUseCase: StatsQueryUseCase,
    private val settingsPrefs: SettingsPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _currentTimeRange = MutableStateFlow(StatsTimeRange(StatsTimeRangeType.WEEK))
    val currentTimeRange: StateFlow<StatsTimeRange> = _currentTimeRange.asStateFlow()

    val dashboardConfig: StateFlow<StatsDashboardConfig> = settingsPrefs.getStatsDashboardConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultStatsDashboardConfig())

    @OptIn(ExperimentalCoroutinesApi::class)
    val statsResult: StateFlow<StatsResult?> = _currentTimeRange
        .flatMapLatest { range ->
            val (startMs, endMs) = computeTimeRangeMs(range)
            if (startMs != null && endMs != null) {
                kotlinx.coroutines.flow.flow<StatsResult?> {
                    _uiState.update { it.copy(isLoading = true) }
                    val result = try {
                        statsQueryUseCase.query(startMs, endMs)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    emit(result)
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            dashboardConfig.collect { config ->
                _uiState.update { it.copy(dashboardConfig = config) }
            }
        }
        viewModelScope.launch {
            _currentTimeRange.collect { range ->
                _uiState.update { it.copy(currentTimeRange = range) }
            }
        }
        viewModelScope.launch {
            statsResult.collect { result ->
                _uiState.update { it.copy(statsResult = result) }
            }
        }
    }

    fun updateTimeRange(range: StatsTimeRange) {
        _currentTimeRange.value = range
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun movePanel(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.dashboardConfig.panels
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val mutable = current.toMutableList()
        val panel = mutable.removeAt(fromIndex)
        mutable.add(toIndex, panel)
        viewModelScope.launch {
            settingsPrefs.updateStatsDashboardConfig(
                _uiState.value.dashboardConfig.copy(panels = mutable.toImmutableList()),
            )
        }
    }

    fun removePanel(panelId: String) {
        val current = _uiState.value.dashboardConfig.panels
        val updated = current.filter { it.id != panelId }.toImmutableList()
        viewModelScope.launch {
            settingsPrefs.updateStatsDashboardConfig(
                _uiState.value.dashboardConfig.copy(panels = updated),
            )
        }
    }

    fun addPanel(type: StatsPanelType) {
        val newPanel = StatsPanelConfig(
            id = UUID.randomUUID().toString(),
            type = type,
            title = type.defaultTitle(),
        )
        val updated = (_uiState.value.dashboardConfig.panels + newPanel).toImmutableList()
        viewModelScope.launch {
            settingsPrefs.updateStatsDashboardConfig(
                _uiState.value.dashboardConfig.copy(panels = updated),
            )
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            settingsPrefs.updateStatsDashboardConfig(defaultStatsDashboardConfig())
        }
    }

    fun selectActivity(activity: ActivityStat?) {
        _uiState.update { it.copy(selectedActivity = activity) }
    }

    private fun computeTimeRangeMs(range: StatsTimeRange): Pair<Long?, Long?> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        return when (range.type) {
            StatsTimeRangeType.DAY -> {
                val target = today.plusDays(range.offset.toLong())
                target.atStartOfDay(zone).toInstant().toEpochMilli() to
                    target.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            StatsTimeRangeType.WEEK -> {
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(range.offset.toLong())
                val weekEnd = weekStart.plusDays(7)
                weekStart.atStartOfDay(zone).toInstant().toEpochMilli() to
                    weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()
            }
            StatsTimeRangeType.MONTH -> {
                val monthBase = today.withDayOfMonth(1).plusMonths(range.offset.toLong())
                val monthEnd = monthBase.plusMonths(1)
                monthBase.atStartOfDay(zone).toInstant().toEpochMilli() to
                    monthEnd.atStartOfDay(zone).toInstant().toEpochMilli()
            }
            StatsTimeRangeType.CUSTOM -> {
                range.customStart to range.customEnd
            }
        }
    }
}

internal fun StatsPanelType.defaultTitle(): String = when (this) {
    StatsPanelType.SUMMARY_CARD -> "概览"
    StatsPanelType.METRIC_CARD -> "指标卡片"
    StatsPanelType.PIE_CHART -> "活动占比"
    StatsPanelType.RANKING_LIST -> "活动排行"
    StatsPanelType.TREND_LINE -> "每日趋势"
    StatsPanelType.BAR_CHART -> "柱状图"
    StatsPanelType.TIMELINE_HEATMAP -> "时间热力图"
    StatsPanelType.COMPARISON -> "对比"
    StatsPanelType.AI_INSIGHT -> "AI 洞察"
}
