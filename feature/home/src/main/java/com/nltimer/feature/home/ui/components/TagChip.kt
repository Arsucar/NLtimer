package com.nltimer.feature.home.ui.components

import android.graphics.DiscretePathEffect as AndroidDiscretePathEffect
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.data.model.TagDisplayConfig
import com.nltimer.core.designsystem.theme.BorderTokens
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledBorder
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.TagUiState

@Composable
fun TagChip(
    tag: TagUiState,
    modifier: Modifier = Modifier,
    tagDisplayConfig: TagDisplayConfig = TagDisplayConfig(),
) {
    val baseColor = tag.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    val displayMode = tagDisplayConfig.displayMode
    val useColor = tagDisplayConfig.useColorForText

    val containerColor = baseColor.copy(alpha = styledAlpha(0.15f))
    val contentColor = if (useColor) {
        baseColor.copy(alpha = styledAlpha(0.9f))
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = baseColor.copy(alpha = styledAlpha(0.5f))

    val shape = when (displayMode) {
        ChipDisplayMode.Capsules -> RoundedCornerShape(50)
        ChipDisplayMode.Squares, ChipDisplayMode.SquareBorder, ChipDisplayMode.None -> RoundedCornerShape(0)
        else -> RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL))
    }

    val surfaceColor = when (displayMode) {
        ChipDisplayMode.Filled, ChipDisplayMode.Capsules,
        ChipDisplayMode.RoundedCorners, ChipDisplayMode.Squares -> containerColor
        else -> Color.Transparent
    }

    val border = when (displayMode) {
        ChipDisplayMode.Capsules -> BorderStroke(styledBorder(BorderTokens.THIN), borderColor)
        else -> null
    }

    val drawModifier = when (displayMode) {
        ChipDisplayMode.Underline -> Modifier.drawBehind {
            val strokeWidth = 2.dp.toPx()
            val y = size.height - strokeWidth / 2
            drawLine(color = containerColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = strokeWidth)
        }
        ChipDisplayMode.SquareBorder -> Modifier.drawBehind {
            val strokeWidth = 1.5.dp.toPx()
            drawRect(color = borderColor, style = Stroke(width = strokeWidth, pathEffect = PathEffect.cornerPathEffect(2.5f), join = StrokeJoin.Round, cap = StrokeCap.Round))
        }
        ChipDisplayMode.HandDrawn -> Modifier.drawBehind {
            val strokeWidth = 1.5.dp.toPx()
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = android.graphics.Color.argb(
                    (borderColor.alpha * 255).toInt(),
                    (borderColor.red * 255).toInt(),
                    (borderColor.green * 255).toInt(),
                    (borderColor.blue * 255).toInt(),
                )
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                pathEffect = AndroidDiscretePathEffect(10f, 5f)
            }
            drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
        ChipDisplayMode.DashedLines -> Modifier.drawBehind {
            val strokeWidth = 1.5.dp.toPx()
            val r = 6.dp.toPx()
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))),
            )
        }
        else -> Modifier
    }

    Surface(
        modifier = modifier.then(drawModifier),
        color = surfaceColor,
        contentColor = contentColor,
        shape = shape,
        border = border,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = if (displayMode == ChipDisplayMode.None) 0.dp else 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = tag.name,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = contentColor,
            )
        }
    }
}

@Composable
fun TagChipSmall(
    name: String,
    modifier: Modifier = Modifier,
    tagDisplayConfig: TagDisplayConfig = TagDisplayConfig(),
) {
    val useColor = tagDisplayConfig.useColorForText
    val baseColor = MaterialTheme.colorScheme.secondaryContainer

    if (useColor) {
        Surface(
            color = baseColor.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier,
        ) {
            Text(
                text = "#$name",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier,
        ) {
            Text(
                text = "#$name",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
