package com.nltimer.feature.behavior_management.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.feature.behavior_management.export.JsonExporter
import com.nltimer.feature.behavior_management.viewmodel.BehaviorManagementViewModel

@Composable
fun BehaviorManagementRoute(
    _onNavigateBack: () -> Unit,
    viewModel: BehaviorManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val data = JsonExporter.fromBehaviors(
                behaviors = uiState.behaviors,
                timeRangeLabel = uiState.timeRange.label,
                startTime = null,
                endTime = null,
                activityGroup = uiState.selectedActivityGroup,
                tagCategory = uiState.selectedTagCategory,
                status = uiState.selectedStatus?.key,
            )
            val json = JsonExporter.export(data)
            viewModel.exportToJson(it, json)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFromJson(it)
        }
    }

    BehaviorManagementScreen(
        viewModel = viewModel,
        onExport = {
            exportLauncher.launch("nltimer_behaviors_${System.currentTimeMillis()}.json")
        },
        onImport = {
            importLauncher.launch(arrayOf("application/json"))
        },
    )
}
