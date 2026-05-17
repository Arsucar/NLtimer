package com.nltimer.core.behaviorui.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.styledAlpha
import kotlin.math.roundToInt

@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun <T : CategorizableItem> CategoryGroupCard(
    index: Int,
    groupName: String,
    items: List<T>,
    selectedId: Long? = null,
    selectedIds: Set<Long> = emptySet(),
    multiSelect: Boolean = false,
    onItemSelected: (Long) -> Unit = {},
    onItemsSelected: (Set<Long>) -> Unit = {},
    onItemLongClick: (Long) -> Unit = {},
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    shiftOffset: Float = 0f,
    collapsed: Boolean,
    onToggleCollapsed: (() -> Unit)? = null,
    showDragHandle: Boolean = true,
    emptyText: String = "暂无项目",
    showItemIcon: Boolean = true,
    showHeader: Boolean = true,
    headerActions: @Composable (() -> Unit)? = null,
    onAddItem: (() -> Unit)? = null,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onPositioned: (index: Int, y: Float, height: Float) -> Unit = { _, _, _ -> },
) {
    val shiftAnimatable = remember { Animatable(0f) }

    LaunchedEffect(shiftOffset) {
        if (!isDragging) {
            shiftAnimatable.animateTo(
                shiftOffset,
                animationSpec = tween(durationMillis = 200),
            )
        }
    }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            shiftAnimatable.snapTo(0f)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                onPositioned(index, pos.y, coordinates.size.height.toFloat())
            }
            .offset {
                val dy = if (isDragging) {
                    dragOffsetY.roundToInt()
                } else {
                    shiftAnimatable.value.roundToInt()
                }
                IntOffset(0, dy)
            }
            .shadow(
                elevation = if (isDragging) 8.dp else 0.dp,
                shape = RoundedCornerShape(8.dp),
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isDragging) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = styledAlpha(0.8f))
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (showHeader) {
                val headerModifier = if (onToggleCollapsed != null) {
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCollapsed() }
                } else {
                    Modifier.fillMaxWidth()
                }
                Row(
                    modifier = headerModifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showDragHandle) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "拖拽排序",
                            modifier = Modifier
                                .size(32.dp)
                                .alpha(0.5f)
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { onDragStart() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDrag(dragAmount.y)
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragCancel() },
                                    )
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${items.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = styledAlpha(0.6f)),
                    )
                    headerActions?.invoke()
                }
            }
            AnimatedVisibility(
                visible = !collapsed || !showHeader,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 32.dp)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = emptyText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items.forEach { item ->
                                val isSelected = if (multiSelect) {
                                    item.itemId in selectedIds
                                } else {
                                    item.itemId == selectedId
                                }
                                ItemChip(
                                    item = item,
                                    isSelected = isSelected,
                                    showIcon = showItemIcon && item is ActivityCategorizable,
                                    onClick = {
                                        if (multiSelect) {
                                            val newIds = if (isSelected) selectedIds - item.itemId else selectedIds + item.itemId
                                            onItemsSelected(newIds)
                                        } else {
                                            onItemSelected(item.itemId)
                                        }
                                    },
                                    onLongClick = { onItemLongClick(item.itemId) },
                                )
                            }
                        }
                        if (onAddItem != null) {
                            AddItemChip(onClick = onAddItem)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : CategorizableItem> ItemChip(
    item: T,
    isSelected: Boolean,
    showIcon: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
    ) {
        if (showIcon) {
            ChipContent(
                text = item.itemName,
                textColor = contentColor,
                iconSlot = {
                    IconRenderer(
                        iconKey = item.iconKey,
                        defaultEmoji = "❓",
                        iconSize = 20.dp,
                    )
                },
            )
        } else {
            ChipContent(
                text = item.itemName,
                textColor = contentColor,
                iconSlot = null,
            )
        }
    }
}

@Composable
private fun AddItemChip(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        ChipContent(
            text = "添加",
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            iconSlot = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
        )
    }
}

@Composable
private fun ChipContent(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    iconSlot: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconSlot != null) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                iconSlot()
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
