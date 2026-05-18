package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode

@Immutable
data class DialogGridConfig(
    val activityDisplayMode: ChipDisplayMode = ChipDisplayMode.Filled,
    val activityLayoutMode: GridLayoutMode = GridLayoutMode.Horizontal,
    val activityColumnLines: Int = 2,
    val activityHorizontalLines: Int = 2,
    val activityUseColorForText: Boolean = true,
    val tagDisplayMode: ChipDisplayMode = ChipDisplayMode.Filled,
    val tagLayoutMode: GridLayoutMode = GridLayoutMode.Horizontal,
    val tagColumnLines: Int = 2,
    val tagHorizontalLines: Int = 2,
    val tagUseColorForText: Boolean = true,
    val showBehaviorNature: Boolean = true,
    val pathDrawMode: PathDrawMode = PathDrawMode.StartToEnd,
    val secondsStrategy: SecondsStrategy = SecondsStrategy.OPEN_TIME,
    val autoMatchNote: Boolean = false,
)
