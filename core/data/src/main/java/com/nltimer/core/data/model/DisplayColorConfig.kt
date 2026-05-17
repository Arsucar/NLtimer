package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

enum class DisplayColorMode {
    NORMAL,
    COLOR,
}

@Immutable
data class DisplayColorConfig(
    val activityIconColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
    val tagDisplayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
)
