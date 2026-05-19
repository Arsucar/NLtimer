package com.nltimer.feature.home.ui.components.moment

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.FocusCardCornerStyle
import com.nltimer.core.data.model.FocusCardShadowStyle
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledCorner
@Composable
internal fun FocusCardConfig.resolvedCornerDp(): Float = when (cornerStyle) {
    FocusCardCornerStyle.NONE -> 0f
    FocusCardCornerStyle.SMALL -> 8f
    FocusCardCornerStyle.LARGE -> styledCorner(ShapeTokens.CORNER_FULL).value
    FocusCardCornerStyle.CUSTOM -> customCornerSize.toFloat()
}

internal fun FocusCardConfig.resolvedElevation(): Float = when (shadowStyle) {
    FocusCardShadowStyle.NONE -> 0f
    FocusCardShadowStyle.LIGHT -> 2f
    FocusCardShadowStyle.STANDARD -> 6f
    FocusCardShadowStyle.HEAVY -> 16f
}

@Composable
internal fun FocusCardConfig.resolvedContainerColor(): Color {
    return themeColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primaryContainer
}

@Composable
internal fun FocusCardConfig.resolvedContentColor(): Color {
    return themeColor?.let { rawColor ->
        // 1. Wrap the primitive color in a Compose Color object first
        val composeColor = Color(rawColor) 
        
        // 2. Now you can safely call .luminance() on it
        if (composeColor.luminance() > 0.5f) {
            Color(0xFF1B1B1B) // 亮背景用深色字
        } else {
            Color(0xFFF5F5F5) // 暗背景用浅色字
        }
    } ?: MaterialTheme.colorScheme.onPrimaryContainer
}