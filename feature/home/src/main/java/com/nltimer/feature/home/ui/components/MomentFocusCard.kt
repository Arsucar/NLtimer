package com.nltimer.feature.home.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.ui.components.moment.ActiveCard
import com.nltimer.feature.home.ui.components.moment.EmptyCard
import com.nltimer.feature.home.ui.components.moment.PendingCard

@Composable
fun MomentFocusCard(
    activeCell: GridCellUiState?,
    nextPendingCell: GridCellUiState?,
    onCompleteBehavior: (Long) -> Unit,
    _onStartNextPending: () -> Unit,
    onStartBehavior: (Long) -> Unit,
    onEmptyCellClick: () -> Unit,
    momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    focusCardConfig: FocusCardConfig = FocusCardConfig(),
    modifier: Modifier = Modifier,
) {
    when {
        activeCell != null -> ActiveCard(
            cell = activeCell,
            onComplete = { activeCell.behaviorId?.let(onCompleteBehavior) },
            _momentStyle = momentStyle,
            focusCardConfig = focusCardConfig,
            modifier = modifier,
        )
        nextPendingCell != null -> PendingCard(
            cell = nextPendingCell,
            onStart = { nextPendingCell.behaviorId?.let(onStartBehavior) },
            _momentStyle = momentStyle,
            focusCardConfig = focusCardConfig,
            modifier = modifier,
        )
        else -> EmptyCard(
            onClick = onEmptyCellClick,
            _momentStyle = momentStyle,
            focusCardConfig = focusCardConfig,
            modifier = modifier,
        )
    }
}
