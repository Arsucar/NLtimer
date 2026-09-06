package com.nltimer.feature.stats.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nltimer.core.data.model.StatsPanelConfig
import kotlinx.coroutines.launch

internal const val GridColumns = 4

@Composable
internal fun StatsGridContainer(
    panels: List<StatsPanelConfig>,
    isEditMode: Boolean,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorderPersist: () -> Unit,
    onRemove: (panelId: String) -> Unit,
    panelContent: @Composable (StatsPanelConfig) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    topPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 100.dp,
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var dragPanelId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var pointerX by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GridColumns),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (headerContent != null) {
            item(span = { GridItemSpan(GridColumns) }) {
                headerContent()
            }
        }

        items(
            count = panels.size,
            key = { panels[it].id },
            span = { index ->
                val span = panels[index].colSpan.coerceIn(1, GridColumns)
                GridItemSpan(span)
            },
        ) { index ->
            val panel = panels[index]
            val isDragging = dragPanelId == panel.id

            if (isEditMode) {
                val editableModifier = Modifier
                    .pointerInput(panel.id) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val itemInfo = gridState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == panel.id }
                                pointerX = (itemInfo?.offset?.x ?: 0) + startOffset.x
                                pointerY = (itemInfo?.offset?.y ?: 0) + startOffset.y
                                dragPanelId = panel.id
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y
                                pointerX += dragAmount.x
                                pointerY += dragAmount.y
                                coroutineScope.launch {
                                    val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo
                                    val lastVisible = visibleItemsInfo.lastOrNull { it.key is String }
                                    if (lastVisible != null && lastVisible.key == panel.id) {
                                        gridState.scrollBy(dragAmount.y * 0.5f)
                                    }
                                    val firstVisible = visibleItemsInfo.firstOrNull { it.key is String }
                                    if (firstVisible != null && firstVisible.key == panel.id) {
                                        gridState.scrollBy(dragAmount.y * 0.5f)
                                    }
                                }
                            },
                            onDragEnd = {
                                val fromIndex = panels.indexOfFirst { it.id == panel.id }
                                val dropItems = gridState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                                    val panelIndex = panels.indexOfFirst { it.id == info.key }
                                    if (panelIndex < 0) return@mapNotNull null
                                    val target = panels[panelIndex]
                                    GridDropItem(
                                        index = panelIndex,
                                        offsetX = info.offset.x,
                                        offsetY = info.offset.y,
                                        width = info.size.width,
                                        height = info.size.height,
                                        colSpan = target.colSpan.coerceIn(1, GridColumns),
                                    )
                                }
                                if (fromIndex >= 0) {
                                    val insertBefore = computeGridDropIndex(
                                        pointerX = pointerX,
                                        pointerY = pointerY,
                                        items = dropItems,
                                        itemCount = panels.size,
                                    )
                                    if (fromIndex != insertBefore && fromIndex + 1 != insertBefore) {
                                        onReorder(fromIndex, insertBefore)
                                        onReorderPersist()
                                    }
                                }
                                dragPanelId = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                dragPanelId = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                        )
                    }
                    .then(
                        if (isDragging) {
                            Modifier
                                .zIndex(10f)
                                .graphicsLayer {
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                    shadowElevation = 16.dp.toPx()
                                    translationX = dragOffsetX
                                    translationY = dragOffsetY
                                }
                        } else {
                            Modifier.graphicsLayer {
                                shadowElevation = 0f
                            }
                        },
                    )

                EditModeGridPanelWrapper(
                    panelTitle = panel.title.ifBlank { panel.type.name },
                    onRemove = { onRemove(panel.id) },
                    modifier = editableModifier,
                ) {
                    panelContent(panel)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    panelContent(panel)
                }
            }
        }
    }
}

@Composable
private fun EditModeGridPanelWrapper(
    panelTitle: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = panelTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.padding(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            content()
        }
    }
}
