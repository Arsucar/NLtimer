package com.nltimer.feature.tag_management.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.groupArchivedItems
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.feature.tag_management.model.TagArchiveUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class TagArchiveViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagArchiveUiState())
    val uiState: StateFlow<TagArchiveUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        tagRepository.getArchived()
            .catch { e ->
                if (e is CancellationException) throw e
                runCatching { Log.e(TAG, "load archived tags failed", e) }
                _uiState.update { it.copy(isLoading = false, errorMessage = "加载失败") }
            }
            .onEach { items ->
                val groups = groupArchivedItems(
                    items = items,
                    groupName = { it.category?.takeIf { name -> name.isNotBlank() } ?: UNCATEGORIZED },
                    archivedAt = { it.archivedAt },
                    itemName = { it.name },
                    uncategorizedName = UNCATEGORIZED,
                )
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
                    .find { it.id == id }
                if (item == null) {
                    _snackbarMessage.value = "恢复失败"
                    return@launch
                }
                tagRepository.setArchived(id, false)
                _snackbarMessage.value = "已恢复「${item.name}」"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                runCatching { Log.e(TAG, "restore tag failed", e) }
                _snackbarMessage.value = "恢复失败"
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    companion object {
        private const val TAG = "TagArchiveVM"
        const val UNCATEGORIZED = "默认"
    }
}
