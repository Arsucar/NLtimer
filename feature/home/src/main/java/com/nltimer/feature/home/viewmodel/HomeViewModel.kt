package com.nltimer.feature.home.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.EventQueryScope
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.hhmmFormatter
import com.nltimer.core.data.util.startOfDayMillis
import com.nltimer.core.data.util.endOfDayMillis
import com.nltimer.core.data.usecase.AddActivityUseCase
import com.nltimer.core.data.usecase.AddBehaviorUseCase
import com.nltimer.core.data.usecase.AddEventUseCase
import com.nltimer.core.data.usecase.AddTagUseCase
import com.nltimer.core.data.usecase.DeleteEventUseCase
import com.nltimer.core.data.usecase.MatchTemplateByTagsUseCase
import com.nltimer.core.data.usecase.UpdateEventUseCase
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.tools.match.ApplyNoteDirectivesUseCase
import com.nltimer.core.tools.match.NoteDirectiveParser
import com.nltimer.core.tools.match.NoteMatcher
import com.nltimer.core.tools.match.NoteProcessOutcome
import com.nltimer.core.tools.match.NoteScanResult
import com.nltimer.core.tools.event.ToolEventBus
import com.nltimer.feature.home.model.AddSheetMode
import com.nltimer.feature.home.model.EventSheetPage
import com.nltimer.feature.home.model.EventSheetTarget
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 首页 ViewModel。
 * 负责加载当天行为、活动和标签数据，管理添加/完成行为、布局切换等交互逻辑。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val activityRepository: ActivityRepository,
    private val activityManagementRepository: ActivityManagementRepository,
    private val tagRepository: TagRepository,
    private val eventTemplateRepository: EventTemplateRepository,
    private val behaviorEventRepository: BehaviorEventRepository,
    private val settingsPrefs: SettingsPrefs,
    private val noteMatcher: NoteMatcher,
    private val addBehaviorUseCase: AddBehaviorUseCase,
    private val addTagUseCase: AddTagUseCase,
    private val addActivityUseCase: AddActivityUseCase,
    private val applyNoteDirectivesUseCase: ApplyNoteDirectivesUseCase,
    private val matchTemplateByTagsUseCase: MatchTemplateByTagsUseCase,
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val clockService: ClockService,
    private val toolEventBus: ToolEventBus,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val uiStateBuilder = HomeUiStateBuilder()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _activityGroups = MutableStateFlow<List<ActivityGroup>>(emptyList())
    val activityGroups: StateFlow<List<ActivityGroup>> = _activityGroups.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    private val _activityLastUsedMap = MutableStateFlow<Map<Long, Long?>>(emptyMap())
    val activityLastUsedMap: StateFlow<Map<Long, Long?>> = _activityLastUsedMap.asStateFlow()

    private val _tagLastUsedMap = MutableStateFlow<Map<Long, Long?>>(emptyMap())
    val tagLastUsedMap: StateFlow<Map<Long, Long?>> = _tagLastUsedMap.asStateFlow()

    val tagCategoryOrder: StateFlow<List<String>> = settingsPrefs.getSavedTagCategoriesOrder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), emptyList())

    private val _selectedActivityId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagsForSelectedActivity: StateFlow<List<Tag>> = _selectedActivityId
        .flatMapLatest { id ->
            if (id != null) tagRepository.getByActivityId(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), emptyList())

    val dialogConfig: StateFlow<DialogGridConfig> = settingsPrefs.getDialogConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), DialogGridConfig())

    val timeLabelConfig: StateFlow<TimeLabelConfig> = settingsPrefs.getTimeLabelConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), TimeLabelConfig())

    val homeLayoutConfig: StateFlow<HomeLayoutConfig> = settingsPrefs.getHomeLayoutConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), HomeLayoutConfig())

    val tagDisplayConfig: StateFlow<com.nltimer.core.data.model.TagDisplayConfig> = settingsPrefs.getTagDisplayConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), com.nltimer.core.data.model.TagDisplayConfig())

    val focusCardConfig: StateFlow<FocusCardConfig> = settingsPrefs.getFocusCardConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), FocusCardConfig())

    /** 全部打点模板（事件表单模板切换 / 列表页） */
    val eventTemplates: StateFlow<List<EventTemplate>> = eventTemplateRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), emptyList())

    /** 事件列表页数据（按当前 sheet 目标 behaviorId 动态观察） */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sheetEvents: StateFlow<List<BehaviorEventWithValues>> = _uiState
        .map { it.eventSheet?.behaviorId }
        .distinctUntilChanged()
        .flatMapLatest { behaviorId ->
            if (behaviorId == null) {
                flowOf(emptyList())
            } else {
                behaviorEventRepository.observeEventsWithValues(EventQueryScope.ByBehavior(behaviorId))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(HomeUiStateBuilder.STATE_TIMEOUT_MS), emptyList())

    private fun today(): LocalDate = LocalDate.now()

    private val _loadedEarliest = MutableStateFlow(LocalDate.now())
    private val _earliestRecord = MutableStateFlow<LocalDate?>(null)
    private val _isLoadingMore = MutableStateFlow(false)

    private val _todayRefreshTrigger = MutableStateFlow(0L)

    init {
        startMidnightTimer()
        loadHomeBehaviors()
        loadActivitiesAndGroups()
        loadAllTags()
        loadLastUsedMaps()
        observeActiveCellEventSummary()
        viewModelScope.launch {
            toolEventBus.events.collect {
                // Room Flow subscriptions in loadHomeBehaviors / loadActivitiesAndGroups /
                // loadAllTags already auto-refresh on DAO writes. This collector exists so
                // future category-specific side-effects (e.g. haptic, toast) can be added.
            }
        }
    }

    private fun startMidnightTimer() {
        viewModelScope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val delayMs = ChronoUnit.MILLIS.between(now, nextMidnight) + 1000L
                delay(delayMs)
                _todayRefreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

    private fun loadActivitiesAndGroups() {
        viewModelScope.launch {
            combine(
                activityRepository.getAllActive(),
                activityRepository.getAllGroups()
            ) { activities, groups ->
                activities to groups
            }.collect { (activities, groups) ->
                _activities.update { activities }
                _activityGroups.update { groups }
            }
        }
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            tagRepository.getAllActive().collect { list ->
                _allTags.update { list }
            }
        }
    }

    private fun loadLastUsedMaps() {
        viewModelScope.launch {
            behaviorRepository.getAllActivityLastUsed().collect { map ->
                _activityLastUsedMap.update { map }
            }
        }
        viewModelScope.launch {
            behaviorRepository.getAllTagLastUsed().collect { map ->
                _tagLastUsedMap.update { map }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadHomeBehaviors() {
        viewModelScope.launch {
            _earliestRecord.value = try {
                behaviorRepository.getEarliestBehaviorDate()
            } catch (_: Exception) {
                null
            }
        }
        viewModelScope.launch {
            combine(
                combine(_loadedEarliest, _todayRefreshTrigger) { earliest, _ -> earliest }
                    .flatMapLatest { earliest ->
                        behaviorRepository.getHomeBehaviors(
                            earliest.startOfDayMillis(),
                            today().endOfDayMillis()
                        )
                    },
                homeLayoutConfig,
                _isLoadingMore,
                _loadedEarliest,
                _earliestRecord,
            ) { behaviors, _, loadingMore, loadedEarliest, earliestRecord ->
                BehaviorsSnapshot(behaviors, loadingMore, loadedEarliest, earliestRecord)
            }.collect { snapshot ->
                val state = buildUiState(snapshot.behaviors)
                val reached = snapshot.earliestRecord?.let { !snapshot.loadedEarliest.isAfter(it) } ?: false
                _uiState.update { current ->
                    state.copy(
                        isLoadingMore = snapshot.isLoadingMore,
                        hasReachedEarliest = reached,
                        addSheetMode = current.addSheetMode,
                        idleStartTime = current.idleStartTime,
                        idleEndTime = current.idleEndTime,
                        editBehaviorId = current.editBehaviorId,
                        editInitialActivityId = current.editInitialActivityId,
                        editInitialTagIds = current.editInitialTagIds,
                        editInitialNote = current.editInitialNote,
                        editInitialEstimatedDurationMs = current.editInitialEstimatedDurationMs,
                        errorMessage = current.errorMessage,
                        eventSheet = current.eventSheet,
                        eventFeedback = current.eventFeedback,
                        // 摘要字段随后由 observeActiveCellEventSummary 流回填（见 restore）
                        momentCells = restoreEventSummary(state.momentCells, current.momentCells),
                    )
                }
                _isLoadingMore.value = false
            }
        }
    }

    private data class BehaviorsSnapshot(
        val behaviors: List<Behavior>,
        val isLoadingMore: Boolean,
        val loadedEarliest: LocalDate,
        val earliestRecord: LocalDate?,
    )

    private suspend fun buildUiState(behaviors: List<Behavior>): HomeUiState {
        val now = LocalTime.now()
        val behaviorIds = behaviors.map { it.id }
        val tagsByBehaviorId = try {
            behaviorRepository.getTagsForBehaviors(behaviorIds)
        } catch (_: Exception) {
            emptyMap()
        }

        return uiStateBuilder.buildUiState(
            behaviors = behaviors,
            activities = _activities.value,
            tagsByBehaviorId = tagsByBehaviorId,
            now = now,
            currentTimeMs = clockService.currentTimeMillis(),
            today = today(),
            gridColumns = homeLayoutConfig.value.grid.columns,
        )
    }

    fun addActivity(name: String, iconKey: String?, color: Long?, groupId: Long?, keywords: String?, tagIds: List<Long>) {
        viewModelScope.launch {
            addActivityUseCase(name, iconKey, color, groupId, keywords, tagIds)
        }
    }

    fun addTag(name: String, color: Long?, iconKey: String?, priority: Int, category: String?, keywords: String?, activityId: Long?) {
        viewModelScope.launch {
            addTagUseCase(name, color, iconKey, priority, category, keywords, activityId)
        }
    }

    fun showAddSheet(mode: AddSheetMode = AddSheetMode.COMPLETED, idleStart: LocalDateTime? = null, idleEnd: LocalDateTime? = null) {
        _uiState.update { it.copy(addSheetMode = mode, idleStartTime = idleStart, idleEndTime = idleEnd) }
    }

    fun showEditSheet(cell: GridCellUiState) {
        val mode = when (cell.status) {
            BehaviorNature.COMPLETED -> AddSheetMode.COMPLETED
            BehaviorNature.ACTIVE -> AddSheetMode.CURRENT
            BehaviorNature.PENDING -> AddSheetMode.TARGET
            null -> return
        }
        _uiState.update {
            it.copy(
                addSheetMode = mode,
                editBehaviorId = cell.behaviorId,
                editInitialActivityId = null,
                editInitialTagIds = cell.tags.map { tag -> tag.id }.toPersistentList(),
                editInitialNote = cell.note,
                editInitialEstimatedDurationMs = cell.estimatedDuration,
                idleStartTime = cell.startTime,
                idleEndTime = cell.endTime,
            )
        }
        cell.behaviorId?.let { behaviorId ->
            viewModelScope.launch {
                behaviorRepository.getBehaviorWithDetails(behaviorId)?.let { details ->
                    _uiState.update { it.copy(editInitialActivityId = details.activity.id) }
                    onActivitySelected(details.activity.id)
                }
            }
        }
    }

    fun hideAddSheet() {
        _uiState.update {
            it.copy(
                addSheetMode = null,
                idleStartTime = null,
                idleEndTime = null,
                editBehaviorId = null,
                editInitialActivityId = null,
                editInitialTagIds = persistentListOf(),
                editInitialNote = null,
                editInitialEstimatedDurationMs = null,
            )
        }
        _selectedActivityId.value = null
    }

    fun onActivitySelected(activityId: Long) {
        _selectedActivityId.value = activityId
    }

    fun addBehavior(
        activityId: Long,
        tagIds: List<Long>,
        startTime: Long,
        endTime: Long?,
        status: BehaviorNature,
        note: String?,
        estimatedDurationMs: Long? = null,
    ) {
        val editId = _uiState.value.editBehaviorId
        viewModelScope.launch {
            when (val result = addBehaviorUseCase(
                activityId = activityId,
                tagIds = tagIds,
                startTime = startTime,
                endTime = endTime,
                status = status,
                note = note,
                editBehaviorId = editId,
                estimatedDurationMs = estimatedDurationMs,
            )) {
                is AddBehaviorUseCase.Result.Success -> hideAddSheet()
                is AddBehaviorUseCase.Result.Conflict ->
                    _uiState.update { it.copy(errorMessage = result.message) }
                is AddBehaviorUseCase.Result.ValidationError ->
                    _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun completeBehavior(behaviorId: Long) {
        viewModelScope.launch {
            val isIdle = _uiState.value.isIdleMode
            behaviorRepository.completeCurrentAndStartNext(behaviorId, isIdle)
        }
    }

    /**
     * 在内存中扫描当前已加载的活动 / 标签列表，返回备注命中结果。
     * 纯内存 contains 计算，2000 条规模下 ~1-3ms，主线程直接调用即可。
     */
    fun matchNoteFromText(note: String): NoteScanResult =
        noteMatcher.scan(note, _activities.value, _allTags.value)

    /**
     * 智能识别按钮的入口：解析 @/# directive → 创建/复用 → 反向扫描备注，
     * 由 UI 层接管把结果合并到 sheet 状态。
     */
    suspend fun processNote(note: String, selectedActivityId: Long? = null): NoteProcessOutcome {
        val parsed = NoteDirectiveParser.parse(note)
        val directive = applyNoteDirectivesUseCase(
            parsed.directives,
            _activities.value,
            _allTags.value,
            selectedActivityId,
        )
        val scan = noteMatcher.scan(parsed.cleanedNote, _activities.value, _allTags.value)
        return NoteProcessOutcome(parsed.cleanedNote, directive, scan)
    }

    fun toggleIdleMode() {
        _uiState.update { it.copy(isIdleMode = !it.isIdleMode) }
    }

    fun startBehavior(behaviorId: Long) {
        viewModelScope.launch {
            behaviorRepository.setStatus(behaviorId, BehaviorNature.ACTIVE.key)
            behaviorRepository.setStartTime(behaviorId, clockService.currentTimeMillis())
        }
    }

    fun startNextPending() {
        viewModelScope.launch {
            val next = behaviorRepository.getNextPending() ?: return@launch
            behaviorRepository.setStatus(next.id, BehaviorNature.ACTIVE.key)
            behaviorRepository.setStartTime(next.id, clockService.currentTimeMillis())
        }
    }

    fun reorderGoals(orderedIds: List<Long>) {
        viewModelScope.launch {
            behaviorRepository.reorderGoals(orderedIds)
        }
    }

    fun reorderActivityGroups(orderedIds: List<Long>) {
        viewModelScope.launch {
            activityManagementRepository.reorderGroups(orderedIds)
        }
    }

    fun reorderTagCategories(orderedNames: List<String>) {
        viewModelScope.launch {
            settingsPrefs.saveTagCategoriesOrder(orderedNames)
        }
    }

    /**
     * 删除行为；keepEvents = true 时先把挂载事件全部转独立（behaviorId 置空，保留 activityId
     * 与字段值），再删行为本体——避免行为删除外键级联连事件一起抹掉（AC5 零数据丢失）。
     * 默认 false 保持旧语义（事件随级联删除）。
     */
    fun deleteBehavior(id: Long, keepEvents: Boolean = false) {
        viewModelScope.launch {
            try {
                behaviorRepository.delete(id, keepEvents)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deleteBehavior failed id=$id keepEvents=$keepEvents", e)
                _uiState.update { it.copy(errorMessage = "删除失败，请重试") }
            }
        }
    }

    /** 行为挂载的事件计数（删除弹窗三选判断：>0 展示「保留事件转独立」选项） */
    suspend fun queryEventCountForBehavior(behaviorId: Long): Int = try {
        behaviorEventRepository.observeEventCountByBehavior(behaviorId).first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "queryEventCountForBehavior failed id=$behaviorId", e)
        0
    }

    /** 行为详情弹窗「复盘事件」分区快照（调用方一次性查询，不订阅 Flow） */
    suspend fun queryBehaviorEvents(behaviorId: Long): List<BehaviorEventWithValues> = try {
        behaviorEventRepository
            .observeEventsWithValues(EventQueryScope.ByBehavior(behaviorId))
            .first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "queryBehaviorEvents failed id=$behaviorId", e)
        emptyList()
    }

    fun scrollToTime(hour: Int) {
        _uiState.update { it.copy(selectedTimeHour = hour) }
    }

    fun onHomeLayoutChange(layout: HomeLayout) {
        viewModelScope.launch {
            settingsPrefs.getThemeFlow().firstOrNull()?.let { theme ->
                settingsPrefs.updateTheme(theme.copy(homeLayout = layout))
            }
        }
    }

    fun onTimeLabelConfigChange(config: TimeLabelConfig) {
        viewModelScope.launch {
            settingsPrefs.updateTimeLabelConfig(config)
        }
    }

    fun onHomeLayoutConfigChange(config: HomeLayoutConfig) {
        viewModelScope.launch {
            settingsPrefs.updateHomeLayoutConfig(config)
        }
    }

    fun showAiQuickInput() {
        _uiState.update { it.copy(showAiQuickInput = true) }
    }

    fun hideAiQuickInput() {
        _uiState.update { it.copy(showAiQuickInput = false) }
    }

    // ---- 结构化事件（打点）----

    /**
     * 「+ 记一笔」入口：解析目标 cell → 模板回退链（标签匹配 → 上次使用 → 首个模板）。
     * 无任何模板时 initialTemplateId = null，表单呈现引导态（保存不可用）。
     */
    fun openEventAddSheet(cell: GridCellUiState?) {
        viewModelScope.launch {
            val tagIds = cell?.tags?.map { it.id } ?: emptyList()
            val templateId = resolveInitialTemplateId(tagIds)
            val activityId = cell?.behaviorId?.let { id ->
                behaviorRepository.getBehaviorWithDetails(id)?.activity?.id
            }
            _uiState.update {
                it.copy(
                    eventSheet = EventSheetTarget(
                        behaviorId = cell?.behaviorId,
                        activityId = activityId,
                        activityName = cell?.activityName,
                        startEpochMs = cell?.startEpochMs,
                        initialTemplateId = templateId,
                        editEventId = null,
                        initialPage = EventSheetPage.FORM,
                    ),
                )
            }
        }
    }

    /** 独立事件入口（无活跃计时亦可打点，AC6）：归属行显示「独立事件」 */
    fun openStandaloneEventSheet() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    eventSheet = EventSheetTarget(
                        behaviorId = null,
                        activityId = null,
                        activityName = null,
                        startEpochMs = null,
                        initialTemplateId = resolveInitialTemplateId(emptyList()),
                        initialPage = EventSheetPage.FORM,
                    ),
                )
            }
        }
    }

    /** 摘要行点击：打开该行为的事件列表（Sheet 内可点条目进编辑表单） */
    fun openEventListSheet(cell: GridCellUiState) {
        viewModelScope.launch {
            val behaviorId = cell.behaviorId ?: return@launch
            val activityId = behaviorRepository.getBehaviorWithDetails(behaviorId)?.activity?.id
            _uiState.update {
                it.copy(
                    eventSheet = EventSheetTarget(
                        behaviorId = behaviorId,
                        activityId = activityId,
                        activityName = cell.activityName,
                        startEpochMs = cell.startEpochMs,
                        initialPage = EventSheetPage.LIST,
                    ),
                )
            }
        }
    }

    /** 列表页 → 表单页编辑已有事件 */
    fun openEventEditSheet(eventId: Long) {
        _uiState.update { current ->
            current.copy(
                eventSheet = current.eventSheet?.copy(editEventId = eventId, initialPage = EventSheetPage.FORM),
            )
        }
    }

    /** 列表页 → 新增表单页 */
    fun openEventNewSheetFromList() {
        viewModelScope.launch {
            val templateId = eventTemplates.value.firstOrNull()?.id
            _uiState.update { current ->
                current.copy(
                    eventSheet = current.eventSheet?.copy(
                        editEventId = null,
                        initialTemplateId = current.eventSheet.initialTemplateId ?: templateId,
                        initialPage = EventSheetPage.FORM,
                    ),
                )
            }
        }
    }

    fun hideEventSheet() {
        _uiState.update { it.copy(eventSheet = null) }
    }

    /** 事件表单查询：编辑态载入事件详情（字段值） */
    suspend fun queryEventWithValues(eventId: Long): BehaviorEventWithValues? =
        try {
            behaviorEventRepository.getEventWithValues(eventId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "queryEventWithValues failed id=$eventId", e)
            null
        }

    fun saveEvent(
        target: EventSheetTarget,
        templateId: Long,
        attachToBehavior: Boolean,
        values: List<BehaviorEventValue>,
    ) {
        val editEventId = target.editEventId
        viewModelScope.launch {
            try {
                if (editEventId != null) {
                    when (val result = updateEventUseCase(
                        eventId = editEventId,
                        templateId = templateId,
                        values = values,
                    )) {
                        is UpdateEventUseCase.Result.Success ->
                            setEventFeedback(buildUpdateFeedback(result.droppedValueNames))
                        UpdateEventUseCase.Result.NotFound ->
                            setEventFeedback("事件不存在或已被删除")
                        is UpdateEventUseCase.Result.ValidationError ->
                            setEventFeedback(result.message)
                    }
                } else {
                    val behaviorId = if (attachToBehavior) target.behaviorId else null
                    when (val result = addEventUseCase(
                        templateId = templateId,
                        behaviorId = behaviorId,
                        activityId = target.activityId,
                        values = values,
                    )) {
                        is AddEventUseCase.Result.Success -> {
                            settingsPrefs.updateLastEventTemplateId(templateId)
                            setEventFeedback("已记录 ✅")
                        }
                        is AddEventUseCase.Result.ValidationError ->
                            setEventFeedback(result.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveEvent failed", e)
                setEventFeedback("保存失败，请重试")
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                deleteEventUseCase(eventId)
                setEventFeedback("已删除该事件")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deleteEvent failed id=$eventId", e)
                setEventFeedback("删除失败，请重试")
            }
        }
    }

    /** 表单页请求模板字段（切换模板时重载） */
    suspend fun queryTemplateFields(templateId: Long): List<EventTemplateField> =
        try {
            eventTemplateRepository.getFieldsByTemplateSync(templateId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "queryTemplateFields failed id=$templateId", e)
            emptyList()
        }

    private fun setEventFeedback(message: String) {
        _uiState.update { it.copy(eventFeedback = message) }
    }

    private fun buildUpdateFeedback(droppedValueNames: List<String>): String =
        if (droppedValueNames.isEmpty()) "已更新"
        else "已更新（同名同类型外的字段值已清空：${droppedValueNames.joinToString("、")}）"

    /** 模板回退链：标签匹配 → 上次使用（仍存在校验）→ 首个模板（无则 null = 引导态） */
    private suspend fun resolveInitialTemplateId(tagIds: List<Long>): Long? {
        matchTemplateByTagsUseCase(tagIds)?.let { return it.id }
        val lastId = settingsPrefs.getLastEventTemplateIdFlow().firstOrNull()
        if (lastId != null && eventTemplateRepository.getTemplateById(lastId) != null) return lastId
        return eventTemplateRepository.observeAll().firstOrNull()?.firstOrNull()?.id
    }

    fun clearEventFeedback() {
        _uiState.update { it.copy(eventFeedback = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun buildSummaryText(withValues: BehaviorEventWithValues, templateName: String?): String {
        val timeText = java.time.Instant.ofEpochMilli(withValues.event.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
            .format(hhmmFormatter)
        val parts = mutableListOf<String>()
        for (value in withValues.values) {
            parts +=
                when {
                    value.valueText != null -> value.valueText!!
                    value.valueNumber != null -> trimNumber(value.valueNumber!!)
                    else -> continue
                }
            if (parts.size >= 2) break
        }
        val valueText = parts.joinToString(" · ")
        return listOfNotNull(timeText, templateName, valueText.ifBlank { null }).joinToString(" ")
    }

    private fun trimNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    /**
     * 活跃 cell 的事件摘要流（R3 摘要行数据源）
     * 单一扁平化流（性能：只对当前活跃 behaviorId 查询，绝不逐 cell 建 query）：
     * combine(计数, 最新事件含字段值, 模板名) → 摘要文本 → 写回对应 cell
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeActiveCellEventSummary() {
        viewModelScope.launch {
            uiState
                .map { state ->
                    state.momentCells
                        .firstOrNull { it.isCurrent && it.status == BehaviorNature.ACTIVE }
                        ?.behaviorId
                }
                .distinctUntilChanged()
                .flatMapLatest { behaviorId ->
                    if (behaviorId == null) {
                        flowOf(null)
                    } else {
                        combine(
                            behaviorEventRepository.observeEventCountByBehavior(behaviorId),
                            behaviorEventRepository.observeLatestEventWithValuesByBehavior(behaviorId),
                            eventTemplateRepository.observeAll(),
                        ) { count, latest, templates ->
                            val templateName = latest?.let { e ->
                                templates.firstOrNull { it.id == e.event.templateId }?.name
                            }
                            val summary = latest?.let { buildSummaryText(it, templateName) }
                            behaviorId to (count to summary)
                        }
                    }
                }
                .map { pair ->
                    pair?.let { (behaviorId, countSummary) ->
                        EventCellSummary(behaviorId, countSummary.first, countSummary.second)
                    }
                }
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.e(TAG, "active cell event summary failed", e)
                }
                .collect { summary ->
                    applyActiveEventSummary(summary)
                }
        }
    }

    private suspend fun applyActiveEventSummary(summary: EventCellSummary?) {
        _uiState.update { state ->
            if (summary == null) {
                val cleared = state.momentCells.map { cell ->
                    if (cell.isCurrent && (cell.eventCount != 0 || cell.latestEventSummary != null)) {
                        cell.copy(eventCount = 0, latestEventSummary = null)
                    } else {
                        cell
                    }
                }.toPersistentList()
                if (cleared != state.momentCells) return@update state.copy(momentCells = cleared)
                return@update state
            }
            val (behaviorId, count, summaryText) = summary
            val updated = state.momentCells.map { cell ->
                if (cell.behaviorId == behaviorId && cell.isCurrent) {
                    cell.copy(eventCount = count, latestEventSummary = summaryText)
                } else {
                    cell
                }
            }.toPersistentList()
            if (updated != state.momentCells) {
                state.copy(momentCells = updated)
            } else {
                state
            }
        }
    }

    /** 摘要数据临时载体：behaviorId / 计数 / 最新事件摘要文本 */
    private data class EventCellSummary(
        val behaviorId: Long,
        val count: Int,
        val summaryText: String?,
    )

    /** 首页重建时把旧状态里活跃 cell 的事件摘要搬运到新 cells，避免下一帧闪烁 */
    private fun restoreEventSummary(
        newCells: List<GridCellUiState>,
        oldCells: List<GridCellUiState>,
    ): PersistentList<GridCellUiState> {
        val oldSummary = oldCells
            .firstOrNull { it.isCurrent && (it.eventCount != 0 || it.latestEventSummary != null) }
            ?: return newCells.toPersistentList()
        val newHasSummary = newCells.any {
            it.isCurrent && (it.eventCount != 0 || it.latestEventSummary != null)
        }
        if (newHasSummary) return newCells.toPersistentList()
        return newCells.map { cell ->
            if (cell.isCurrent && cell.behaviorId == oldSummary.behaviorId) {
                cell.copy(eventCount = oldSummary.eventCount, latestEventSummary = oldSummary.latestEventSummary)
            } else {
                cell
            }
        }.toPersistentList()
    }

    fun loadMore() {
        if (_isLoadingMore.value) return
        val current = _loadedEarliest.value
        val candidate = current.minusDays(7)
        val cap = _earliestRecord.value ?: return
        val target = if (candidate.isBefore(cap)) cap else candidate
        if (!target.isBefore(current)) return
        _isLoadingMore.value = true
        _loadedEarliest.value = target
    }

    suspend fun queryTagIdsForActivity(activityId: Long): List<Long> =
        activityManagementRepository.getTagIdsForActivity(activityId)

    suspend fun queryActivityIdsForTag(tagId: Long): List<Long> =
        tagRepository.getActivityIdsForTag(tagId)
}
