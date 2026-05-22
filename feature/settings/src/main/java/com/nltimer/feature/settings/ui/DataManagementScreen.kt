package com.nltimer.feature.settings.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.repository.ImportMode
import com.nltimer.core.data.usecase.ExportScope
import com.nltimer.core.data.usecase.ImportScope
import com.nltimer.core.designsystem.component.ExpandableCard
import com.nltimer.core.designsystem.component.SettingsEntryCard

@Composable
fun DataManagementRoute(
    _onNavigateBack: () -> Unit,
    onNavigateToBehaviorManagement: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.writeExportToFileForLauncher(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onFileSelectedForImport(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.exportEvents.collect { event ->
            val scopeName = when (event.scope) {
                ExportScope.ALL -> "all"
                ExportScope.ACTIVITIES -> "activities"
                ExportScope.TAGS -> "tags"
                ExportScope.CATEGORIES -> "categories"
            }
            val timestamp = com.nltimer.core.data.util.formatExportTimestamp()
            exportLauncher.launch("nltimer_${scopeName}_$timestamp.json")
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.pendingImportData != null) {
        ImportModeDialog(
            onConfirm = { mode -> viewModel.confirmImport(mode) },
            onDismiss = { viewModel.dismissImportDialog() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        DataManagementScreen(
            _isExporting = uiState.isExporting,
            _isImporting = uiState.isImporting,
            onExport = { scope -> viewModel.exportData(scope) },
            onImport = { scope ->
                viewModel.triggerImport(scope)
                importLauncher.launch(arrayOf("application/json"))
            },
            onExportToClipboard = { scope -> viewModel.exportToClipboard(context, scope) },
            onImportFromClipboard = { scope -> viewModel.importFromClipboard(context, scope) },
            onNavigateToBehaviorManagement = onNavigateToBehaviorManagement,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun DataManagementScreen(
    _isExporting: Boolean,
    _isImporting: Boolean,
    onExport: (ExportScope) -> Unit,
    onImport: (ImportScope) -> Unit,
    onExportToClipboard: (ExportScope) -> Unit,
    onImportFromClipboard: (ImportScope) -> Unit,
    onNavigateToBehaviorManagement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedAll by remember { mutableStateOf(false) }
    var expandedActivities by remember { mutableStateOf(false) }
    var expandedTags by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExpandableCard(
                title = "总数据",
                expanded = expandedAll,
                onToggle = { expandedAll = !expandedAll },
            ) {
                DataActionButtons(
                    onExport = { onExport(ExportScope.ALL) },
                    onImport = { onImport(ImportScope.ALL) },
                    onExportToClipboard = { onExportToClipboard(ExportScope.ALL) },
                    onImportFromClipboard = { onImportFromClipboard(ImportScope.ALL) },
                )
            }
        }

        item {
            ExpandableCard(
                title = "活动数据",
                expanded = expandedActivities,
                onToggle = { expandedActivities = !expandedActivities },
            ) {
                DataActionButtons(
                    onExport = { onExport(ExportScope.ACTIVITIES) },
                    onImport = { onImport(ImportScope.ACTIVITIES) },
                    onExportToClipboard = { onExportToClipboard(ExportScope.ACTIVITIES) },
                    onImportFromClipboard = { onImportFromClipboard(ImportScope.ACTIVITIES) },
                )
            }
        }

        item {
            ExpandableCard(
                title = "标签数据",
                expanded = expandedTags,
                onToggle = { expandedTags = !expandedTags },
            ) {
                DataActionButtons(
                    onExport = { onExport(ExportScope.TAGS) },
                    onImport = { onImport(ImportScope.TAGS) },
                    onExportToClipboard = { onExportToClipboard(ExportScope.TAGS) },
                    onImportFromClipboard = { onImportFromClipboard(ImportScope.TAGS) },
                )
            }
        }

        item {
            ExpandableCard(
                title = "分类数据",
                subtitle = "活动分组 + 标签分类",
                expanded = expandedCategories,
                onToggle = { expandedCategories = !expandedCategories },
            ) {
                DataActionButtons(
                    onExport = { onExport(ExportScope.CATEGORIES) },
                    onImport = { onImport(ImportScope.CATEGORIES) },
                    onExportToClipboard = { onExportToClipboard(ExportScope.CATEGORIES) },
                    onImportFromClipboard = { onImportFromClipboard(ImportScope.CATEGORIES) },
                )
            }
        }

        item {
            SettingsEntryCard(
                icon = Icons.Default.Storage,
                title = "行为记录管理",
                subtitle = "管理计时记录",
                onClick = onNavigateToBehaviorManagement,
            )
        }
    }
}

@Composable
private fun DataActionButtons(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportToClipboard: () -> Unit,
    onImportFromClipboard: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(onClick = onExport, modifier = Modifier.weight(1f)) {
            Text(text = "导出", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onImport, modifier = Modifier.weight(1f)) {
            Text(text = "导入", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onExportToClipboard, modifier = Modifier.weight(1f)) {
            Text(text = "复制", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onImportFromClipboard, modifier = Modifier.weight(1f)) {
            Text(text = "粘贴", style = MaterialTheme.typography.labelSmall)
        }
    }
}
