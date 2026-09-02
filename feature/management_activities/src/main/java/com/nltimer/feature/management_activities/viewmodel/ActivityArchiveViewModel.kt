package com.nltimer.feature.management_activities.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.groupArchivedItems
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.feature.management_activities.model.ActivityArchiveUiState
import com.nltimer.feature.management_activities.model.ArchivedActivityItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class ActivityArchiveViewModel @Inject constructor(
    private val repository: ActivityManagementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityArchiveUiState())
    val uiState: StateFlow<ActivityArchiveUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        combine(
            repository.getArchived(),
            repository.getAllGroups(),
        ) { activities, groups ->
            val groupNames = groups.associate { it.id to it.name }
            val items = activities.map { activity ->
                ArchivedActivityItem(
                    activity = activity,
                    groupName = activity.groupId?.let { groupNames[it] } ?: UNCATEGORIZED,
                )
            }
            groupArchivedItems(
                items = items,
                groupName = { it.groupName },
                archivedAt = { it.activity.archivedAt },
                itemName = { it.activity.name },
                uncategorizedName = UNCATEGORIZED,
            )
        }
            .catch { e ->
                if (e is CancellationException) throw e
                runCatching { Log.e(TAG, "load archived activities failed", e) }
                _uiState.update { it.copy(isLoading = false, errorMessage = "加载失败") }
            }
            .onEach { groups ->
                _uiState.update {
                    it.copy(isLoading = false, groups = groups, errorMessage = null)
                }
            }
            .launchIn(viewModelScope)
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            try {
                val item = _uiState.value.groups
                    .asSequence()
                    .flatMap { it.items.asSequence() }
                    .find { it.activity.id == id }
                if (item == null) {
                    _snackbarMessage.value = "恢复失败"
                    return@launch
                }
                repository.setArchived(id, false)
                _snackbarMessage.value = "已恢复「${item.activity.name}」"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                runCatching { Log.e(TAG, "restore activity failed", e) }
                _snackbarMessage.value = "恢复失败"
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    companion object {
        private const val TAG = "ActivityArchiveVM"
        const val UNCATEGORIZED = "未分类"
    }
}
