package com.nltimer.feature.stats.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.model.StatsPanelType
import com.nltimer.feature.stats.viewmodel.StatsViewModel

@Composable
fun StatsRoute(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StatsScreen(
        uiState = uiState,
        onTimeRangeChange = viewModel::updateTimeRange,
        onToggleEditMode = viewModel::toggleEditMode,
        onMovePanel = viewModel::movePanel,
        onRemovePanel = viewModel::removePanel,
        onAddPanel = viewModel::addPanel,
        onResetToDefault = viewModel::resetToDefault,
        onBarClick = viewModel::selectActivity,
        onDismissActivity = { viewModel.selectActivity(null) },
    )
}
