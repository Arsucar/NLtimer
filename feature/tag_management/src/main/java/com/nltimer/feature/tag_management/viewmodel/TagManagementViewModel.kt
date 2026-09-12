package com.nltimer.feature.tag_management.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventQueryScope
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.usecase.AddTagUseCase
import com.nltimer.core.designsystem.theme.DisplayColorMode
import com.nltimer.feature.tag_management.model.CategoryWithTags
import com.nltimer.feature.tag_management.model.DialogState
import com.nltimer.feature.tag_management.model.TagManagementUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManagementViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val addTagUseCase: AddTagUseCase,
    private val activityRepository: ActivityManagementRepository,
    private val eventTemplateRepository: EventTemplateRepository,
    private val settingsPrefs: SettingsPrefs,
    private val behaviorEventRepository: BehaviorEventRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "TagManagementViewModel"
    }

    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState: StateFlow<TagManagementUiState> = _uiState.asStateFlow()

    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            _addedCategories.value = settingsPrefs.getSavedTagCategoriesOrder().first()
            loadData()
        }
        loadActivities()
        loadGroups()
        loadTemplates()
        settingsPrefs.getDisplayColorConfigFlow()
            .onEach { config ->
                _uiState.update { it.copy(displayColorConfig = config) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        combine(
            tagRepository.getAllActive(),
            tagRepository.getDistinctCategories(),
            _addedCategories,
        ) { allTags, dbCategories, addedCategories ->
            val uncategorizedTags = allTags.filter { it.category.isNullOrBlank() }
            val categorizedTags = allTags.filter { !it.category.isNullOrBlank() }
            val addedSet = addedCategories.toSet()
            val dbCategorySet = dbCategories.toSet()
            val orderedKnownCategories = addedCategories.filter { it in dbCategorySet || it in addedSet }
            val missingCategories = (dbCategorySet - orderedKnownCategories.toSet()).sorted()
            val allCategories = orderedKnownCategories + missingCategories
            val categoriesWithTags = allCategories.map { categoryName ->
                CategoryWithTags(
                    categoryName = categoryName,
                    tags = categorizedTags.filter { it.category == categoryName },
                )
            }
            if (_expandedCategories.value.isEmpty() && _uiState.value.categories.isEmpty()) {
                _expandedCategories.value = allCategories.toSet()
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    uncategorizedTags = uncategorizedTags,
                    categories = categoriesWithTags,
                    categoryNames = allCategories,
                    expandedCategoryNames = _expandedCategories.value,
                )
            }
        }
            .catch { _uiState.update { it.copy(isLoading = false) } }
            .launchIn(viewModelScope)
    }

    private fun loadActivities() {
        activityRepository.getAllActivities()
            .onEach { activities ->
                _uiState.update { it.copy(allActivities = activities) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadGroups() {
        activityRepository.getAllGroups()
            .onEach { groups ->
                _uiState.update { it.copy(activityGroups = groups) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTemplates() {
        eventTemplateRepository.observeAll()
            .onEach { templates ->
                _uiState.update { it.copy(allTemplates = templates) }
            }
            .launchIn(viewModelScope)
    }

    /** 标签关联事件弹层数据源（DialogState.TagEvents 驱动；关闭弹层自动清空） */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTagEvents: StateFlow<List<BehaviorEventWithValues>> = uiState
        .map { (it.dialogState as? DialogState.TagEvents)?.tag?.id }
        .distinctUntilChanged()
        .flatMapLatest { tagId ->
            if (tagId == null) {
                flowOf(emptyList())
            } else {
                behaviorEventRepository.observeEventsWithValues(EventQueryScope.ByTag(tagId))
            }
        }
        .catch { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "observe tag events failed", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showAddTagDialog(category: String? = null) {
        _uiState.update { it.copy(dialogState = DialogState.AddTag(category)) }
    }

    fun toggleCategoryExpand(categoryName: String) {
        val current = _expandedCategories.value
        _expandedCategories.value = if (categoryName in current) current - categoryName else current + categoryName
        _uiState.update { it.copy(expandedCategoryNames = _expandedCategories.value) }
    }

    fun setAllCategoriesExpanded(expanded: Boolean) {
        _expandedCategories.value = if (expanded) {
            _uiState.value.categories.map { it.categoryName }.toSet()
        } else {
            emptySet()
        }
        _uiState.update { it.copy(expandedCategoryNames = _expandedCategories.value) }
    }

    fun reorderCategories(orderedNames: List<String>) {
        viewModelScope.launch {
            val allCurrent = _uiState.value.categoryNames
            val ordered = orderedNames + allCurrent.filterNot { it in orderedNames }
            _addedCategories.value = ordered
            settingsPrefs.saveTagCategoriesOrder(ordered)
        }
    }

    fun showEditTagDialog(tag: Tag) {
        viewModelScope.launch {
            val activityId = tagRepository.getActivityIdsForTag(tag.id).firstOrNull()
            val boundTemplateId = eventTemplateRepository
                .observeTemplatesByTag(tag.id)
                .first()
                .firstOrNull()
                ?.id
            _uiState.update { it.copy(dialogState = DialogState.EditTag(tag, activityId, boundTemplateId)) }
        }
    }

    fun showDeleteTagDialog(tag: Tag) {
        _uiState.update { it.copy(dialogState = DialogState.DeleteTag(tag)) }
    }

    /** 标签「查看关联事件」入口（EditTagFormSheet 行动作 → 独立 DialogState 弹层） */
    fun showTagEvents(tag: Tag) {
        _uiState.update { it.copy(dialogState = DialogState.TagEvents(tag)) }
    }

    /** 关联事件弹层内删除单条事件（事件本体删除，字段值随外键级联消失） */
    fun deleteTagEvent(eventId: Long) {
        viewModelScope.launch {
            behaviorEventRepository.deleteEvent(eventId)
        }
    }

    fun showMoveTagDialog(tag: Tag, currentCategory: String?) {
        _uiState.update { it.copy(dialogState = DialogState.MoveTag(tag, currentCategory)) }
    }

    fun showAddCategoryDialog() {
        _uiState.update { it.copy(dialogState = DialogState.AddCategory) }
    }

    fun showRenameCategoryDialog(name: String) {
        _uiState.update { it.copy(dialogState = DialogState.RenameCategory(name)) }
    }

    fun showDeleteCategoryDialog(name: String, tagCount: Int) {
        _uiState.update { it.copy(dialogState = DialogState.DeleteCategory(name, tagCount)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    fun addTag(name: String, color: Long?, iconKey: String?, priority: Int, category: String?, keywords: String?, activityId: Long?) {
        viewModelScope.launch {
            addTagUseCase(name, color, iconKey, priority, category, keywords, activityId)
            dismissDialog()
        }
    }

    fun updateTag(tag: Tag, activityId: Long?) {
        viewModelScope.launch {
            tagRepository.update(tag)
            tagRepository.setActivityTagBindings(tag.id, listOfNotNull(activityId))
            dismissDialog()
        }
    }

    /**
     * 标签 ↔ 打点模板绑定（编辑标签表单选择后即时持久化）
     * previousTemplateId 为该标签当前绑定的模板（=新的为切换，移除旧绑定 + 增加新绑定；
     * 模板删除或标签删除时绑定随外键级联消失，事件本体不受影响）
     */
    fun bindTagTemplate(tagId: Long, newTemplateId: Long?, previousTemplateId: Long?) {
        viewModelScope.launch {
            if (previousTemplateId != null && previousTemplateId != newTemplateId) {
                eventTemplateRepository.removeTagBinding(previousTemplateId, tagId)
            }
            if (newTemplateId != null) {
                eventTemplateRepository.addTagBinding(newTemplateId, tagId)
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.setArchived(tag.id, true)
            dismissDialog()
        }
    }

    fun archiveTag(tag: Tag, note: String?) {
        viewModelScope.launch {
            tagRepository.update(
                tag.copy(
                    isArchived = true,
                    archivedAt = System.currentTimeMillis(),
                    archiveNote = note,
                ),
            )
            dismissDialog()
        }
    }

    fun moveTagToCategory(tagId: Long, newCategory: String?) {
        viewModelScope.launch {
            val updatedTag = tagRepository.getById(tagId)?.copy(category = newCategory)
            if (updatedTag != null) {
                tagRepository.update(updatedTag)
            }
            dismissDialog()
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            val updated = (_addedCategories.value + trimmed).distinct()
            _addedCategories.value = updated
            settingsPrefs.saveTagCategoriesOrder(updated)
            dismissDialog()
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            tagRepository.renameCategory(oldName, newName)
            if (oldName in _addedCategories.value) {
                val updated = _addedCategories.value.map { if (it == oldName) newName else it }.distinct()
                _addedCategories.value = updated
                settingsPrefs.saveTagCategoriesOrder(updated)
            }
            if (oldName in _expandedCategories.value) {
                _expandedCategories.value = _expandedCategories.value - oldName + newName
            }
            dismissDialog()
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            tagRepository.resetCategory(name)
            val updated = _addedCategories.value - name
            _addedCategories.value = updated
            settingsPrefs.saveTagCategoriesOrder(updated)
            _expandedCategories.value = _expandedCategories.value - name
            dismissDialog()
        }
    }

    fun updateTagDisplayColorMode(mode: DisplayColorMode) {
        viewModelScope.launch {
            val currentConfig = _uiState.value.displayColorConfig
            settingsPrefs.updateDisplayColorConfig(
                currentConfig.copy(tagDisplayColorMode = mode)
            )
        }
    }
}
