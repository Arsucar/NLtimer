package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.GridRowUiState
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridRow(
    row: GridRowUiState,
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellClick: (GridCellUiState) -> Unit = {},
    onCellLongClick: (GridCellUiState) -> Unit = {},
    timeLabelConfig: TimeLabelConfig = TimeLabelConfig(),
    modifier: Modifier = Modifier,
    gridStyle: GridLayoutStyle = GridLayoutStyle(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
) {
    val gridMinHeight = gridStyle.minRowHeight.dp
    val columnSpacing = gridStyle.columnSpacing.dp

    Column(modifier = modifier.fillMaxWidth()) {
        if (row.cells.isNotEmpty() && timeLabelConfig.visible) {
            TimeFloatingLabel(
                time = row.startTime,
                isCurrentRow = row.isCurrentRow,
                config = timeLabelConfig,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        ) {
                val addPlaceholderIndex = row.cells.indexOfFirst { it.isAddPlaceholder }
                val firstEmptyIndex = row.cells.indexOfFirst { it.behaviorId == null }
                val targetEmptyIndex = if (addPlaceholderIndex != -1) addPlaceholderIndex else firstEmptyIndex

                repeat(gridStyle.columns) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val cell = row.cells.getOrNull(index)
                        if (cell != null) {
                            when {
                                row.isLocked -> GridCellLocked(modifier = Modifier.heightIn(min = gridMinHeight))
                                cell.behaviorId != null -> GridCell(
                                    cell = cell,
                                    modifier = Modifier
                                        .heightIn(min = gridMinHeight)
                                        .combinedClickable(
                                            onClick = { onCellClick(cell) },
                                            onLongClick = { onCellLongClick(cell) },
                                        ),
                                    gridStyle = gridStyle,
                                    tagDisplayConfig = tagDisplayConfig,
                                )
                                index == targetEmptyIndex -> GridCellEmpty(
                                    onClick = { onEmptyCellClick(cell.startTime, cell.endTime) },
                                    modifier = Modifier.heightIn(min = gridMinHeight)
                                )
                                else -> {}
                            }
                        }
                    }
            }
        }
    }
}

