package com.nltimer.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val OptionsRowPaddingHorizontal = 16.dp
private val OptionsGap = 8.dp
private val OptionsGapFromFab = 68.dp
private val OptionCornerRadius = 8.dp
private val OptionPaddingVertical = 12.dp
private val OptionTonalElevation = 4.dp
private val OptionShadowElevation = 4.dp
private const val MAX_OPTIONS_PER_ROW = 3

enum class DragMenuOptionsPlacement {
    AboveAnchorTop,
    AboveAnchorBottom,
}

@Composable
fun DragMenuOptions(
    state: DragMenuState,
    options: List<String>,
    modifier: Modifier = Modifier,
    placement: DragMenuOptionsPlacement = DragMenuOptionsPlacement.AboveAnchorBottom,
    gapFromAnchor: Dp = OptionsGapFromFab,
    horizontalPadding: Dp = OptionsRowPaddingHorizontal,
    rowGap: Dp = OptionsGap,
    maxOptionsPerRow: Int = MAX_OPTIONS_PER_ROW,
) {
    if (!state.isDragging) return

    val density = LocalDensity.current
    val gapPx = with(density) { gapFromAnchor.toPx() }
    val anchorY = when (placement) {
        DragMenuOptionsPlacement.AboveAnchorTop -> state.anchorLayoutPosition.y
        DragMenuOptionsPlacement.AboveAnchorBottom -> state.anchorLayoutPosition.y + state.anchorSize.height
    }
    val optionsY = anchorY - state.optionsRowHeight - gapPx - state.containerPositionInWindow.y

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .offset { IntOffset(0, optionsY.roundToInt()) }
            .onGloballyPositioned { coords ->
                state.optionsRowHeight = coords.size.height.toFloat()
            },
        verticalArrangement = Arrangement.spacedBy(rowGap)
    ) {
        options.chunked(maxOptionsPerRow).reversed().forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowGap)
            ) {
                rowOptions.forEach { option ->
                    OptionCell(state, option, Modifier.weight(1f))
                }
                repeat(maxOptionsPerRow - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FabDragOptions(
    state: DragFabState,
    options: List<String>,
    modifier: Modifier = Modifier,
) {
    DragMenuOptions(
        state = state,
        options = options,
        modifier = modifier,
        placement = DragMenuOptionsPlacement.AboveAnchorBottom,
        gapFromAnchor = OptionsGapFromFab,
        horizontalPadding = OptionsRowPaddingHorizontal,
    )
}

@Composable
private fun OptionCell(
    state: DragMenuState,
    option: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates ->
                val position = layoutCoordinates.positionInWindow()
                val size = layoutCoordinates.size
                state.optionsLayoutBounds[option] = Rect(
                    position.x,
                    position.y,
                    position.x + size.width,
                    position.y + size.height
                )
            },
        shape = RoundedCornerShape(OptionCornerRadius),
        color = if (state.hoveredOption == option)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = OptionTonalElevation,
        shadowElevation = OptionShadowElevation
    ) {
        Box(
            modifier = Modifier.padding(vertical = OptionPaddingVertical),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (state.hoveredOption == option)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
