package com.nltimer.core.designsystem.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val FabShadowElevation = 4.dp
private val FabContentPaddingHorizontal = 16.dp
private val FabContentPaddingVertical = 16.dp
private val FabIconSpacerWidth = 8.dp

class DragMenuState {
    var isDragging by mutableStateOf(false)
    var dragOffset by mutableStateOf(Offset.Zero)
    var dragStartOffset by mutableStateOf(Offset.Zero)
    var hoveredOption by mutableStateOf<String?>(null)
    val optionsLayoutBounds = mutableStateMapOf<String, Rect>()
    var anchorLayoutPosition by mutableStateOf(Offset.Zero)
    var anchorSize by mutableStateOf(IntSize.Zero)
    var optionsRowHeight by mutableFloatStateOf(0f)
    var containerPositionInWindow by mutableStateOf(Offset.Zero)

    var fabLayoutPosition: Offset
        get() = anchorLayoutPosition
        set(value) {
            anchorLayoutPosition = value
        }

    var fabSize: IntSize
        get() = anchorSize
        set(value) {
            anchorSize = value
        }

    var boxPositionInWindow: Offset
        get() = containerPositionInWindow
        set(value) {
            containerPositionInWindow = value
        }
}

@Composable
fun rememberDragMenuState(): DragMenuState = remember { DragMenuState() }

typealias DragFabState = DragMenuState

@Composable
fun rememberDragFabState(): DragFabState = rememberDragMenuState()

@Composable
fun DraggableMenuAnchor(
    state: DragMenuState,
    modifier: Modifier = Modifier,
    constrainToScreen: Boolean = true,
    onOptionSelected: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val currentOnOptionSelected by rememberUpdatedState(onOptionSelected)

    Box(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates ->
                state.anchorLayoutPosition = layoutCoordinates.positionInWindow()
                state.anchorSize = layoutCoordinates.size
            }
            .offset {
                IntOffset(
                    state.dragOffset.x.roundToInt(),
                    state.dragOffset.y.roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        state.isDragging = true
                        state.dragStartOffset = startOffset
                        state.optionsLayoutBounds.clear()
                    },
                    onDragEnd = {
                        state.hoveredOption?.let { option ->
                            currentOnOptionSelected(option)
                        }
                        resetDragState(state)
                    },
                    onDragCancel = { resetDragState(state) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.dragOffset += dragAmount
                        if (constrainToScreen) {
                            val anchorW = state.anchorSize.width.toFloat()
                            val anchorH = state.anchorSize.height.toFloat()
                            state.dragOffset = Offset(
                                x = state.dragOffset.x.coerceIn(
                                    -state.anchorLayoutPosition.x,
                                    screenWidthPx - state.anchorLayoutPosition.x - anchorW
                                ),
                                y = state.dragOffset.y.coerceIn(
                                    -state.anchorLayoutPosition.y,
                                    screenHeightPx - state.anchorLayoutPosition.y - anchorH
                                )
                            )
                        }
                        val currentPointerPosition =
                            state.anchorLayoutPosition + state.dragOffset + state.dragStartOffset
                        val hit = state.optionsLayoutBounds.entries
                            .firstOrNull { it.value.contains(currentPointerPosition) }
                            ?.key
                        if (hit != state.hoveredOption) {
                            state.hoveredOption = hit
                        }
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun DragActionFab(
    state: DragFabState,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    label: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    cornerRadius: Dp = 28.dp,
    onClick: () -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    DraggableMenuAnchor(
        state = state,
        modifier = modifier,
        onOptionSelected = onOptionSelected,
    ) {
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = FabShadowElevation,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = FabContentPaddingHorizontal,
                    vertical = FabContentPaddingVertical
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
                if (label != null) {
                    Spacer(modifier = Modifier.width(FabIconSpacerWidth))
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

private fun resetDragState(state: DragMenuState) {
    state.isDragging = false
    state.dragOffset = Offset.Zero
    state.dragStartOffset = Offset.Zero
    state.hoveredOption = null
}
