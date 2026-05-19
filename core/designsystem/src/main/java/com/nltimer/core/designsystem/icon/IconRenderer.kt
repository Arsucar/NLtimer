package com.nltimer.core.designsystem.icon

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IconRenderer(
    iconKey: String?,
    modifier: Modifier = Modifier,
    defaultEmoji: String = "📌",
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconSize: Dp = 24.dp,
    emojiFontSize: TextUnit = TextUnit.Unspecified,
) {
    val resolvedFontSize = if (emojiFontSize == TextUnit.Unspecified) {
        iconSize.value.sp
    } else {
        emojiFontSize
    }

    when {
        iconKey == null -> {
            Text(
                text = defaultEmoji,
                fontSize = resolvedFontSize,
                modifier = modifier,
            )
        }
        IconKeyResolver.isHugeIcon(iconKey) -> {
            val imageVector = IconKeyResolver.resolveImageVector(iconKey)
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = IconKeyResolver.iconKeyToDisplayText(iconKey),
                    tint = tint,
                    modifier = modifier,
                )
            } else {
                android.util.Log.w("IconRenderer", "Failed to resolve: $iconKey")
                Text(
                    text = defaultEmoji,
                    fontSize = resolvedFontSize,
                    modifier = modifier,
                )
            }
        }
        IconKeyResolver.isMaterialIcon(iconKey) -> {
            val imageVector = IconKeyResolver.resolveImageVector(iconKey)
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = IconKeyResolver.iconKeyToDisplayText(iconKey),
                    tint = tint,
                    modifier = modifier,
                )
            } else {
                android.util.Log.w("IconRenderer", "Failed to resolve: $iconKey")
                Text(
                    text = defaultEmoji,
                    fontSize = resolvedFontSize,
                    modifier = modifier,
                )
            }
        }
        else -> {
            Text(
                text = iconKey,
                fontSize = resolvedFontSize,
                modifier = modifier,
            )
        }
    }
}
