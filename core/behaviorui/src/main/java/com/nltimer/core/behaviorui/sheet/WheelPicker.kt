package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

internal val WheelItemRotationDegrees = -35f
internal val WheelItemScaleFar = 0.45f
internal val WheelItemAlphaFar = 0.6f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    modifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemHeight: Dp = 40.dp,
    visibleItemsCount: Int = 3,
    initialScrollIndex: Int = 0,
    animate: Boolean = true,
    onCenterClick: () -> Unit = {},
    content: @Composable (item: T, isSelected: Boolean) -> Unit = { item, isSelected ->
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant
        val selectedColor = MaterialTheme.colorScheme.onSecondaryContainer
        Text(
            text = item.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (isSelected) 14.sp else 12.sp,
                color = if (isSelected) selectedColor else textColor,
            ),
        )
    },
) {
    val effectiveInitialIndex = remember {
        items.indexOf(selectedItem).takeIf { it >= 0 } ?: initialScrollIndex
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = effectiveInitialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val paddingCount = visibleItemsCount / 2

    val paddedItems = remember(items) {
        val list = mutableListOf<T?>()
        repeat(paddingCount) { list.add(null) }
        list.addAll(items)
        repeat(paddingCount) { list.add(null) }
        list
    }

    LaunchedEffect(selectedItem) {
        if (!listState.isScrollInProgress) {
            val index = items.indexOf(selectedItem)
            if (index != -1 && listState.firstVisibleItemIndex != index) {
                if (animate) {
                    listState.animateScrollToItem(index)
                } else {
                    listState.scrollToItem(index)
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val item = items.getOrNull(index)
                if (item != null && item != selectedItem) {
                    onItemSelected(item)
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .nestedScroll(remember { object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {} }),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            count = paddedItems.size,
            key = { index -> index },
        ) { index ->
            val item = paddedItems[index]
            val isSelected = item == selectedItem

            WheelPickerItem(
                itemHeight = itemHeight,
                listState = listState,
                index = index,
                visibleItemsCount = visibleItemsCount,
                isSelected = isSelected,
                onCenterClick = onCenterClick,
            ) {
                if (item != null) {
                    content(item, isSelected)
                }
            }
        }
    }
}

@Composable
private fun WheelPickerItem(
    itemHeight: Dp,
    listState: LazyListState,
    index: Int,
    visibleItemsCount: Int,
    isSelected: Boolean,
    onCenterClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(itemHeight)
            .graphicsLayer {
                val layoutInfo = listState.layoutInfo
                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                if (itemInfo != null) {
                    val viewportCenter =
                        (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                    val distanceFromCenter = itemCenter - viewportCenter

                    val fraction = (distanceFromCenter / (itemHeight.toPx() * (visibleItemsCount / 2f))).coerceIn(-1f, 1f)

                    rotationX = fraction * WheelItemRotationDegrees
                    scaleY = 1f - abs(fraction) * WheelItemScaleFar
                    scaleX = 1f + abs(fraction) * WheelItemScaleFar
                    alpha = 1f - abs(fraction) * WheelItemAlphaFar
                }
            }
            .then(if (isSelected) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCenterClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
