package com.nltimer.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Long?.toComposeColor(default: Color = MaterialTheme.colorScheme.primary): Color {
    return this?.let { Color(it.toInt()) } ?: default
}
