package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode

@Immutable
data class TagDisplayConfig(
    val displayMode: ChipDisplayMode = ChipDisplayMode.Filled,
    val layoutMode: GridLayoutMode = GridLayoutMode.Horizontal,
    val columnLines: Int = 2,
    val horizontalLines: Int = 2,
    val useColorForText: Boolean = true,
)
