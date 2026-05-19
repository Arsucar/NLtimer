package com.nltimer.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.startOfDayMillis
import com.nltimer.core.data.util.endOfDayMillis
import com.nltimer.core.data.usecase.AddActivityUseCase
import com.nltimer.core.data.usecase.AddBehaviorUseCase
import com.nltimer.core.data.usecase.AddTagUseCase
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.tools.match.ApplyNoteDirectivesUseCase
import com.nltimer.core.tools.match.NoteDirectiveParser
import com.nltimer.core.tools.match.NoteMatcher
import com.nltimer.core.tools.match.NoteProcessOutcome
import com.nltimer.core.tools.match.NoteScanResult
import com.nltimer.feature.home.match.MatchStrategy
import com.nltimer.feature.home.model.AddSheetMode
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    private val settingsPrefs: SettingsPrefs,
    private val matchStrategy: MatchStrategy,
    private val noteMatcher: NoteMatcher,
    private val addBehaviorUseCase: AddBehaviorUseCase,
    private val addTagUseCase: AddTagUseCase,
    private val addActivityUseCase: AddActivityUseCase,
    private val applyNoteDirectivesUseCase: ApplyNoteDirectivesUseCase,
    private val clockService: ClockService,
) : ViewModel() {

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

    private val today = LocalDate.now()

    private val _loadedEarliest = MutableStateFlow(today)
    private val _earliestRecord = MutableStateFlow<LocalDate?>(null)
    private val _isLoadingMore = MutableStateFlow(false)

    init {
        loadHomeBehaviors()
        loadActivitiesAndGroups()
        loadAllTags()
        loadLastUsedMaps()
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
                _loadedEarliest.flatMapLatest { earliest ->
                    behaviorRepository.getHomeBehaviors(
                        earliest.startOfDayMillis(),
                        today.endOfDayMillis()
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
                        errorMessage = current.errorMessage,
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
            today = today,
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
    suspend fun processNote(note: String): NoteProcessOutcome {
        val parsed = NoteDirectiveParser.parse(note)
        val directive = applyNoteDirectivesUseCase(
            parsed.directives,
            _activities.value,
            _allTags.value,
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

    fun deleteBehavior(id: Long) {
        viewModelScope.launch {
            behaviorRepository.delete(id)
        }
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
}
