package com.nltimer.feature.settings.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.ExportData
import com.nltimer.core.data.repository.ImportMode
import com.nltimer.core.data.repository.ImportResult
import com.nltimer.core.data.usecase.ExportDataUseCase
import com.nltimer.core.data.usecase.ExportScope
import com.nltimer.core.data.usecase.ImportDataUseCase
import com.nltimer.core.data.usecase.ImportScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ExportEvent(val data: ExportData, val scope: ExportScope)

data class DataManagementUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val snackbarMessage: String? = null,
    val pendingImportData: ExportData? = null,
    val pendingImportScope: ImportScope? = null,
    val pendingExportData: ExportData? = null,
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    private val _exportEvents = MutableSharedFlow<ExportEvent>(extraBufferCapacity = 1)
    val exportEvents: SharedFlow<ExportEvent> = _exportEvents.asSharedFlow()

    fun exportData(scope: ExportScope) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val data = exportDataUseCase(scope)
                _uiState.update { it.copy(isExporting = false, pendingExportData = data) }
                _exportEvents.tryEmit(ExportEvent(data, scope))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, snackbarMessage = "导出失败")
                }
            }
        }
    }

    fun writeExportToFileForLauncher(context: Context, uri: Uri) {
        val data = _uiState.value.pendingExportData ?: return
        viewModelScope.launch {
            try {
                val content = json.encodeToString(ExportData.serializer(), data)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                _uiState.update { it.copy(pendingExportData = null, snackbarMessage = "数据已导出") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "导出失败") }
            }
        }
    }

    fun onFileSelectedForImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes().decodeToString()
                } ?: error("无法读取文件")
                val data = json.decodeFromString(ExportData.serializer(), content)
                _uiState.update { it.copy(pendingImportData = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "文件格式无效") }
            }
        }
    }

    fun triggerImport(scope: ImportScope) {
        _uiState.update { it.copy(pendingImportScope = scope) }
    }

    fun confirmImport(mode: ImportMode) {
        val data = _uiState.value.pendingImportData ?: return
        val scope = _uiState.value.pendingImportScope ?: return
        _uiState.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            val result = importDataUseCase(data, scope, mode)
            _uiState.update {
                when (result) {
                    is ImportResult.Success -> it.copy(
                        isImporting = false,
                        pendingImportData = null,
                        pendingImportScope = null,
                        snackbarMessage = buildImportMessage(result),
                    )
                    is ImportResult.Error -> it.copy(
                        isImporting = false,
                        snackbarMessage = "导入失败：${result.message}",
                    )
                }
            }
        }
    }

    fun dismissImportDialog() {
        _uiState.update {
            it.copy(pendingImportData = null, pendingImportScope = null)
        }
    }

    fun exportToClipboard(context: Context, scope: ExportScope) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val data = exportDataUseCase(scope)
                val content = json.encodeToString(ExportData.serializer(), data)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("NLtimer Export", content))
                _uiState.update {
                    it.copy(isExporting = false, snackbarMessage = "已复制到剪贴板")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, snackbarMessage = "导出到剪贴板失败")
                }
            }
        }
    }

    fun importFromClipboard(context: Context, scope: ImportScope) {
        _uiState.update { it.copy(pendingImportScope = scope) }
        viewModelScope.launch {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val content = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                    ?: error("剪贴板为空")
                val data = json.decodeFromString(ExportData.serializer(), content)
                _uiState.update { it.copy(pendingImportData = data) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "剪贴板数据格式无效", pendingImportScope = null)
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun buildImportMessage(result: ImportResult.Success): String {
        val parts = mutableListOf<String>()
        if (result.activitiesImported > 0) parts.add("${result.activitiesImported} 条活动")
        if (result.tagsImported > 0) parts.add("${result.tagsImported} 条标签")
        if (result.activityGroupsImported > 0) parts.add("${result.activityGroupsImported} 个分组")
        if (result.tagCategoriesImported > 0) parts.add("${result.tagCategoriesImported} 个标签分类")
        return if (parts.isEmpty()) "无新数据导入" else "已导入 ${parts.joinToString("、")}"
    }
}
