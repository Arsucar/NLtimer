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
    val eventsPanel by viewModel.eventsPanel.collectAsStateWithLifecycle()

    StatsScreen(
        uiState = uiState,
        onTimeRangeChange = viewModel::updateTimeRange,
        onToggleEditMode = viewModel::toggleEditMode,
        onMovePanel = viewModel::movePanel,
        onPersistPanels = viewModel::persistPanels,
        onRemovePanel = viewModel::removePanel,
        onAddPanel = viewModel::addPanel,
        onResetToDefault = viewModel::resetToDefault,
        eventsPanel = eventsPanel,
        onEventsViewModeChange = viewModel::setEventsViewMode,
        onEventsFocusTemplateChange = viewModel::setEventsFocusTemplate,
        onEventsFilterChange = viewModel::applyEventFiltersChange,
        onBarClick = viewModel::selectActivity,
        onDismissActivity = { viewModel.selectActivity(null) },
    )
}
