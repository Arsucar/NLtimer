package com.nltimer.feature.behavior_management.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.util.startOfDayMillis
import com.nltimer.feature.behavior_management.export.BehaviorExportData
import com.nltimer.feature.behavior_management.export.JsonExporter
import com.nltimer.feature.behavior_management.export.JsonImporter
import com.nltimer.feature.behavior_management.model.BehaviorManagementUiState
import com.nltimer.feature.behavior_management.model.DuplicateHandling
import com.nltimer.feature.behavior_management.model.TimeRangePreset
import com.nltimer.feature.behavior_management.model.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BehaviorManagementViewModel @Inject constructor(
    application: Application,
    private val behaviorRepository: BehaviorRepository,
    private val activityRepository: ActivityRepository,
    private val activityManagementRepository: ActivityManagementRepository,
    private val tagRepository: TagRepository,
    private val settingsPrefs: SettingsPrefs,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BehaviorManagementUiState())
    val uiState: StateFlow<BehaviorManagementUiState> = _uiState.asStateFlow()

    val activityGroups = activityRepository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tagCategories = tagRepository.getDistinctCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allActivities = activityRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTags = tagRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activityLastUsedMap = behaviorRepository.getAllActivityLastUsed()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val tagLastUsedMap = behaviorRepository.getAllTagLastUsed()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val tagCategoryOrder = settingsPrefs.getSavedTagCategoriesOrder()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        observeBehaviors()
    }

    private fun observeBehaviors() {
        viewModelScope.launch {
            _uiState
                .map { Triple(it.rangeStartDate, it.rangeStartHour, it.timeRange) }
                .distinctUntilChanged()
                .flatMapLatest { (date, hour, preset) ->
                    val range = date.epochRangeFor(preset, hour)
                    behaviorRepository.getBehaviorsWithDetailsOverlappingTimeRange(range.startEpoch, range.endEpoch)
                        .combine(activityGroups) { behaviors, groups ->
                            Pair(behaviors, groups)
                        }
                }
                .combine(_uiState) { (behaviors, groups), state ->
                    applyFilters(behaviors, state, groups)
                }
                .catch {
                    _uiState.update { s -> s.copy(behaviors = persistentListOf()) }
                }
                .collect { filtered ->
                    _uiState.update { it.copy(behaviors = filtered.toImmutableList()) }
                }
        }
    }

    private fun applyFilters(
        behaviors: List<BehaviorWithDetails>,
        state: BehaviorManagementUiState,
        groups: List<ActivityGroup>,
    ): List<BehaviorWithDetails> {
        var result = behaviors

        state.selectedActivityGroup?.let { groupName ->
            val groupIds = groups
                .filter { it.name == groupName }
                .map { it.id }
                .toSet()
            result = result.filter { it.activity.groupId in groupIds }
        }

        state.selectedTagCategory?.let { category ->
            result = result.filter { bwd ->
                bwd.tags.any { it.category == category }
            }
        }

        state.selectedStatus?.let { nature ->
            result = result.filter { it.behavior.status == nature }
        }

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            result = result.filter { bwd ->
                bwd.activity.name.lowercase().contains(query) ||
                    bwd.tags.any { it.name.lowercase().contains(query) } ||
                    bwd.behavior.note?.lowercase()?.contains(query) == true
            }
        }

        return result
    }

    fun setTimeRange(preset: TimeRangePreset) {
        _uiState.update {
            it.copy(
                timeRange = preset,
                rangeStartDate = it.rangeStartDate.normalizedFor(preset),
            )
        }
    }

    fun setRangeStartDate(date: LocalDate) {
        _uiState.update { it.copy(rangeStartDate = date.normalizedFor(it.timeRange)) }
    }

    fun setRangeStartHour(hour: Int) {
        _uiState.update { it.copy(rangeStartHour = hour.coerceIn(0, 23)) }
    }

    fun navigateRange(direction: Int) {
        _uiState.update { state ->
            if (state.timeRange == TimeRangePreset.FOUR_HOURS || state.timeRange == TimeRangePreset.EIGHT_HOURS) {
                val newStart = state.rangeStartDate
                    .atTime(state.rangeStartHour, 0)
                    .plusHours(state.timeRange.hours * direction)
                return@update state.copy(
                    rangeStartDate = newStart.toLocalDate(),
                    rangeStartHour = newStart.hour,
                )
            }

            val newDate = when (state.timeRange) {
                TimeRangePreset.FOUR_HOURS, TimeRangePreset.EIGHT_HOURS, TimeRangePreset.ONE_DAY ->
                    state.rangeStartDate.plusDays(direction.toLong())
                TimeRangePreset.THREE_DAYS ->
                    state.rangeStartDate.plusDays(3L * direction)
                TimeRangePreset.SEVEN_DAYS ->
                    state.rangeStartDate.plusWeeks(direction.toLong())
                TimeRangePreset.ONE_MONTH ->
                    state.rangeStartDate.plusMonths(direction.toLong())
                TimeRangePreset.ONE_YEAR ->
                    state.rangeStartDate.plusYears(direction.toLong())
            }
            state.copy(rangeStartDate = newDate.normalizedFor(state.timeRange))
        }
    }

    fun setActivityGroupFilter(groupName: String?) {
        _uiState.update { it.copy(selectedActivityGroup = groupName) }
    }

    fun setTagCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedTagCategory = category) }
    }

    fun setStatusFilter(nature: BehaviorNature?) {
        _uiState.update { it.copy(selectedStatus = nature) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun startEditBehavior(behaviorId: Long) {
        _uiState.update { it.copy(editBehaviorId = behaviorId) }
    }

    fun finishEditBehavior() {
        _uiState.update { it.copy(editBehaviorId = null) }
    }

    fun toggleMultiSelect(behaviorId: Long) {
        _uiState.update { state ->
            val newIds = if (behaviorId in state.selectedBehaviorIds) {
                state.selectedBehaviorIds - behaviorId
            } else {
                state.selectedBehaviorIds + behaviorId
            }
            state.copy(
                selectedBehaviorIds = newIds,
                isMultiSelectMode = newIds.isNotEmpty(),
            )
        }
    }

    fun exitMultiSelect() {
        _uiState.update { it.copy(selectedBehaviorIds = emptySet(), isMultiSelectMode = false) }
    }

    fun deleteSelectedBehaviors() {
        viewModelScope.launch {
            _uiState.value.selectedBehaviorIds.forEach { id ->
                behaviorRepository.delete(id)
            }
            exitMultiSelect()
        }
    }

    fun exportToJson(uri: Uri, json: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray())
                    os.flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@launch
                val data = JsonImporter.parse(json)
                val localActivities = allActivities.value
                val localTags = allTags.value
                val existingBehaviors = _uiState.value.behaviors.map { it.behavior }
                val preview = JsonImporter.analyzeDuplicates(data, localActivities, localTags, existingBehaviors)
                _uiState.update { it.copy(importPreview = preview, importData = data) }
            } catch (_: Exception) {
            }
        }
    }

    fun updateBehavior(
        id: Long,
        activityId: Long,
        tagIds: List<Long>,
        startTime: LocalDateTime,
        endTime: LocalDateTime?,
        nature: BehaviorNature,
        note: String?,
    ) {
        viewModelScope.launch {
            val startEpoch = if (nature == BehaviorNature.PENDING) {
                0L
            } else {
                startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val endEpoch = if (nature == BehaviorNature.COMPLETED) {
                endTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: startEpoch
            } else {
                null
            }
            behaviorRepository.updateBehavior(id, activityId, startEpoch, endEpoch, nature.key, note)
            behaviorRepository.updateTagsForBehavior(id, tagIds)
            finishEditBehavior()
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

    fun executeImport(data: BehaviorExportData, handling: DuplicateHandling) {
        viewModelScope.launch {
            val localActivities = allActivities.value
            val localTags = allTags.value
            val activityMap = localActivities.associateBy { it.name }
            val tagMap = localTags.associateBy { it.name }
            val existingBehaviors = _uiState.value.behaviors.map { it.behavior }

            for (item in data.behaviors) {
                val localActivity = activityMap[item.activity.name] ?: continue
                val isDuplicate = existingBehaviors.any { existing ->
                    existing.activityId == localActivity.id && timeOverlaps(
                        existing.startTime, existing.endTime,
                        item.startTime, item.endTime,
                    )
                }

                when {
                    isDuplicate && handling == DuplicateHandling.SKIP -> continue
                    isDuplicate && handling == DuplicateHandling.OVERWRITE -> {
                        val existing = existingBehaviors.first { existing ->
                            existing.activityId == localActivity.id && timeOverlaps(
                                existing.startTime, existing.endTime,
                                item.startTime, item.endTime,
                            )
                        }
                        behaviorRepository.updateBehavior(
                            existing.id, localActivity.id,
                            item.startTime, item.endTime, item.status, item.note,
                        )
                        val tagIds = item.tags.mapNotNull { tagMap[it.name]?.id }
                        behaviorRepository.updateTagsForBehavior(existing.id, tagIds)
                    }
                    else -> {
                        val tagIds = item.tags.mapNotNull { tagMap[it.name]?.id }
                        behaviorRepository.insert(
                            Behavior(
                                id = 0,
                                activityId = localActivity.id,
                                startTime = item.startTime,
                                endTime = item.endTime,
                                status = BehaviorNature.entries.first { it.key == item.status },
                                note = item.note,
                                pomodoroCount = item.pomodoroCount,
                                sequence = item.sequence,
                                estimatedDuration = item.estimatedDuration,
                                actualDuration = item.actualDuration,
                                achievementLevel = item.achievementLevel,
                                wasPlanned = item.wasPlanned,
                            ),
                            tagIds = tagIds,
                        )
                    }
                }
            }
            _uiState.update { it.copy(importPreview = null, importData = null) }
        }
    }

    fun dismissImportPreview() {
        _uiState.update { it.copy(importPreview = null, importData = null) }
    }

    private fun timeOverlaps(
        existingStart: Long, existingEnd: Long?,
        newStart: Long, newEnd: Long?,
    ): Boolean {
        val eEnd = existingEnd ?: Long.MAX_VALUE
        val nEnd = newEnd ?: Long.MAX_VALUE
        return newStart < eEnd && existingStart < nEnd
    }

    private data class EpochRange(val startEpoch: Long, val endEpoch: Long)

    private fun LocalDate.normalizedFor(preset: TimeRangePreset): LocalDate =
        when (preset) {
            TimeRangePreset.ONE_MONTH -> withDayOfMonth(1)
            TimeRangePreset.ONE_YEAR -> withDayOfYear(1)
            else -> this
        }

    private fun LocalDate.epochRangeFor(preset: TimeRangePreset, startHour: Int): EpochRange {
        val startDate = normalizedFor(preset)
        val startDateTime = when (preset) {
            TimeRangePreset.FOUR_HOURS, TimeRangePreset.EIGHT_HOURS ->
                startDate.atTime(startHour.coerceIn(0, 23), 0)
            else -> startDate.atStartOfDay()
        }
        val endEpoch = when (preset) {
            TimeRangePreset.FOUR_HOURS, TimeRangePreset.EIGHT_HOURS ->
                startDateTime.plusHours(preset.hours).toEpochMillis()
            TimeRangePreset.ONE_DAY -> startDate.plusDays(1).startOfDayMillis()
            TimeRangePreset.THREE_DAYS -> startDate.plusDays(3).startOfDayMillis()
            TimeRangePreset.SEVEN_DAYS -> startDate.plusWeeks(1).startOfDayMillis()
            TimeRangePreset.ONE_MONTH -> startDate.plusMonths(1).startOfDayMillis()
            TimeRangePreset.ONE_YEAR -> startDate.plusYears(1).startOfDayMillis()
        }
        return EpochRange(
            startEpoch = startDateTime.toEpochMillis(),
            endEpoch = endEpoch,
        )
    }

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
