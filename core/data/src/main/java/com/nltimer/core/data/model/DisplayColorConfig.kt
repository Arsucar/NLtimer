package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class DisplayColorConfig(
    val activityUseColorForText: Boolean = true,
    val tagUseColorForText: Boolean = true,
)
