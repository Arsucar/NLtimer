package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.designsystem.theme.DisplayColorMode

@Immutable
data class DisplayColorConfig(
    val activityIconColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
    val tagDisplayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
    val showTagIcon: Boolean = true,
)
