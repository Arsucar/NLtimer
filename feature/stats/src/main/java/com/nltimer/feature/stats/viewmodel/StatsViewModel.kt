package com.nltimer.feature.stats.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.ActivityStat
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventQueryScope
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.model.StatsDashboardConfig
import com.nltimer.core.data.model.StatsPanelConfig
import com.nltimer.core.data.model.StatsPanelType
import com.nltimer.core.data.model.EventsContentMode
import com.nltimer.core.data.model.EventsViewMode
import com.nltimer.core.data.model.StatsResult
import com.nltimer.core.data.model.StatsTimeRange
import com.nltimer.core.data.model.StatsTimeRangeType
import com.nltimer.core.data.model.defaultStatsDashboardConfig
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.usecase.ObserveEventsUseCase
import com.nltimer.core.data.usecase.StatsQueryUseCase
import com.nltimer.feature.stats.model.EventPanelFilters
import com.nltimer.feature.stats.model.EventsPanelUiState
import com.nltimer.feature.stats.model.StatsUiState
import com.nltimer.feature.stats.model.applyEventFilters
import com.nltimer.feature.stats.model.computeNumberDomains
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val observeEventsUseCase: ObserveEventsUseCase,
    private val eventTemplateRepository: EventTemplateRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
    }

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _currentTimeRange = MutableStateFlow(StatsTimeRange(StatsTimeRangeType.WEEK))
    val currentTimeRange: StateFlow<StatsTimeRange> = _currentTimeRange.asStateFlow()

    val dashboardConfig: StateFlow<StatsDashboardConfig> = settingsPrefs.getStatsDashboardConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultStatsDashboardConfig())

    @Volatile
    private var dashboardPersistInFlight = false

    private var persistGeneration = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    val statsResult: StateFlow<StatsResult?> = _currentTimeRange
        .flatMapLatest { range ->
            val (startMs, endMs) = computeTimeRangeMs(range)
            if (startMs != null && endMs != null) {
                kotlinx.coroutines.flow.flow<StatsResult?> {
                    _uiState.update { it.copy(isLoading = true) }
                    val result = try {
                        statsQueryUseCase.query(startMs, endMs)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "stats query failed", e)
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
                if (dashboardPersistInFlight) return@collect
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
        if (fromIndex !in current.indices) return
        val insertBefore = toIndex.coerceIn(0, current.size)
        if (fromIndex == insertBefore || fromIndex + 1 == insertBefore) return
        val mutable = current.toMutableList()
        val panel = mutable.removeAt(fromIndex)
        val dest = if (insertBefore > fromIndex) insertBefore - 1 else insertBefore
        mutable.add(dest, panel)
        applyDashboard(
            _uiState.value.dashboardConfig.copy(panels = mutable.toImmutableList()),
            persist = false,
        )
    }

    fun persistPanels() {
        applyDashboard(_uiState.value.dashboardConfig, persist = true)
    }

    fun removePanel(panelId: String) {
        val updated = _uiState.value.dashboardConfig.panels
            .filter { it.id != panelId }
            .toImmutableList()
        applyDashboard(
            _uiState.value.dashboardConfig.copy(panels = updated),
            persist = true,
        )
    }

    fun addPanel(type: StatsPanelType) {
        val newPanel = StatsPanelConfig(
            id = UUID.randomUUID().toString(),
            type = type,
            title = type.defaultTitle(),
        )
        val updated = (_uiState.value.dashboardConfig.panels + newPanel).toImmutableList()
        applyDashboard(
            _uiState.value.dashboardConfig.copy(panels = updated),
            persist = true,
        )
    }

    fun resetToDefault() {
        applyDashboard(defaultStatsDashboardConfig(), persist = true)
    }

    fun selectActivity(activity: ActivityStat?) {
        _uiState.update { it.copy(selectedActivity = activity) }
    }

    // ---- 复盘事件面板（StatsPanelType.EVENTS）----

    /** 字段值筛选状态（内存态：SELECT 多选 / RATING·NUMBER 值域 / 文本 LIKE） */
    private val _eventFilters = MutableStateFlow(EventPanelFilters())

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventsPanel: StateFlow<EventsPanelUiState> =
        dashboardConfig
            .map { config ->
                RawEventsKey(
                    hasPanel = config.panels.any { it.type == StatsPanelType.EVENTS },
                    focusTemplateId = config.eventsFocusTemplateId,
                    viewMode = config.eventsViewMode,
                    contentMode = config.eventsContentMode,
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                if (!key.hasPanel) {
                    flowOf<RawEventsData>(RawEventsData(hasPanel = false, viewMode = key.viewMode, contentMode = key.contentMode))
                } else {
                    val focusId = key.focusTemplateId
                    val eventsFlow = if (focusId == null) {
                        observeEventsUseCase.observeWithValues(EventQueryScope.All)
                    } else {
                        observeEventsUseCase.observeWithValues(EventQueryScope.ByTemplate(focusId))
                    }
                    val templatesFlow = eventTemplateRepository.observeAll()
                    val fieldsFlow = flow {
                        emit(
                            focusId?.let { eventTemplateRepository.getFieldsByTemplateSync(it) }
                                ?: emptyList(),
                        )
                    }
                    combine(eventsFlow, templatesFlow, fieldsFlow) { events, templates, fields ->
                        RawEventsData(
                            hasPanel = true,
                            viewMode = key.viewMode,
                            contentMode = key.contentMode,
                            focusTemplateId = focusId,
                            templates = templates,
                            focusedFields = fields,
                            allEvents = events,
                        )
                    }
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                // 静默降级为空面板（保留视图偏好，丢失数据订阅），记录日志
                emit(RawEventsData(hasPanel = false, viewMode = EventsViewMode.CARD, contentMode = EventsContentMode.MIXED))
                Log.e(TAG, "events data stream failed", e)
            }
            .combine(_eventFilters) { raw, filters ->
                val domains = computeNumberDomains(raw.allEvents, raw.focusedFields)
                EventsPanelUiState(
                    hasPanel = raw.hasPanel,
                    viewMode = raw.viewMode,
                    contentMode = raw.contentMode,
                    focusTemplateId = raw.focusTemplateId,
                    templates = raw.templates,
                    focusedFields = raw.focusedFields,
                    numberDomains = domains,
                    allEvents = raw.allEvents,
                    events = applyEventFilters(raw.allEvents, filters),
                    filters = filters,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventsPanelUiState())

    /** 切换卡片 / 表格视图（持久化到 stats_dashboard_config，向后兼容新增字段） */
    fun setEventsViewMode(mode: EventsViewMode) {
        updateDashboardConfig { it.copy(eventsViewMode = mode) }
    }

    /** 切换混排 / 单模板聚焦（null = 混排；切走聚焦时同步清空字段筛选防残留） */
    fun setEventsFocusTemplate(templateId: Long?) {
        if (templateId == null) {
            updateDashboardConfig {
                it.copy(
                    eventsContentMode = EventsContentMode.MIXED,
                    eventsFocusTemplateId = null,
                )
            }
            _eventFilters.value = EventPanelFilters()
        } else {
            updateDashboardConfig {
                it.copy(
                    eventsContentMode = EventsContentMode.FOCUS,
                    eventsFocusTemplateId = templateId,
                )
            }
        }
    }

    /** 应用事件面板筛选状态（UI 组件以纯函数 helper 计算新状态后整体写入） */
    fun applyEventFiltersChange(filters: EventPanelFilters) {
        _eventFilters.value = filters
    }

    private fun updateDashboardConfig(transform: (StatsDashboardConfig) -> StatsDashboardConfig) {
        applyDashboard(transform(_uiState.value.dashboardConfig), persist = true)
    }

    /** 面板存在性 / 聚焦偏好 / 视图偏好的去重键（驱动 eventsPanel 数据链重启） */
    private data class RawEventsKey(
        val hasPanel: Boolean,
        val focusTemplateId: Long?,
        val viewMode: EventsViewMode,
        val contentMode: EventsContentMode,
    )

    /** 原始数据载体（未过滤事件 + 模板/字段结构 + 视图偏好） */
    private data class RawEventsData(
        val hasPanel: Boolean,
        val viewMode: EventsViewMode,
        val contentMode: EventsContentMode,
        val focusTemplateId: Long? = null,
        val templates: List<EventTemplate> = emptyList(),
        val focusedFields: List<EventTemplateField> = emptyList(),
        val allEvents: List<BehaviorEventWithValues> = emptyList(),
    )

    private fun applyDashboard(config: StatsDashboardConfig, persist: Boolean) {
        val generation = ++persistGeneration
        dashboardPersistInFlight = true
        _uiState.update { it.copy(dashboardConfig = config) }
        if (persist) {
            viewModelScope.launch {
                try {
                    settingsPrefs.updateStatsDashboardConfig(_uiState.value.dashboardConfig)
                } finally {
                    if (generation == persistGeneration) {
                        dashboardPersistInFlight = false
                    }
                }
            }
        }
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
    StatsPanelType.EVENTS -> "复盘事件"
}
