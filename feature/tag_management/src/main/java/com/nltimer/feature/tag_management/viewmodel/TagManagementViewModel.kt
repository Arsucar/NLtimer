package com.nltimer.feature.tag_management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.usecase.AddTagUseCase
import com.nltimer.feature.tag_management.model.CategoryWithTags
import com.nltimer.feature.tag_management.model.DialogState
import com.nltimer.feature.tag_management.model.TagManagementUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManagementViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val addTagUseCase: AddTagUseCase,
    private val activityRepository: ActivityManagementRepository,
    private val settingsPrefs: SettingsPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState: StateFlow<TagManagementUiState> = _uiState.asStateFlow()

    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            _addedCategories.value = settingsPrefs.getSavedTagCategoriesOrder().first()
        }
        loadData()
        loadActivities()
        loadGroups()
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
            _uiState.update { it.copy(dialogState = DialogState.EditTag(tag, activityId)) }
        }
    }

    fun showDeleteTagDialog(tag: Tag) {
        _uiState.update { it.copy(dialogState = DialogState.DeleteTag(tag)) }
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

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.setArchived(tag.id, true)
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
}
