package com.nltimer.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// region 公共选择组件

/**
 * 选择对话框项数据类
 *
 * @param label 显示文本
 * @param value 选项值
 */
data class SelectionItem<T>(
    val label: String,
    val value: T,
)

/**
 * 分组列表项位置枚举
 */
enum class ItemPosition {
    /** 独立项（不与任何项相连，四角全圆角） */
    NONE,

    /** 分组首项（顶部大圆角，底部小圆角） */
    TOP,

    /** 分组中间项（四角小圆角） */
    MIDDLE,

    /** 分组末项（顶部小圆角，底部大圆角） */
    BOTTOM,
}

/** 首末项圆角半径 */
private const val END_RADIUS = 24

/** 连接处圆角半径 */
private const val CONNECTED_RADIUS = 6

// endregion

// region 选择瓷片组件

/**
 * 选择瓷片 — 点击后弹出选择对话框的列表项
 *
 * 参考 Mindful-strong 的 DefaultDropdownTile 设计，
 * 使用圆角容器包裹，显示当前选中值，末尾带下拉箭头图标。
 * 点击后以弹出动画展示 [SelectionDialog]。
 *
 * @param T 选项值的类型
 * @param title 标题文本
 * @param selected 当前选中的选项（可为null表示未选择）
 * @param items 可选项列表
 * @param onSelected 选中后的回调
 * @param modifier 修饰符
 * @param position 在分组列表中的位置（控制圆角）
 * @param leadingIcon 前置图标
 * @param dialogIcon 弹窗中的图标
 * @param enabled 是否启用
 */
@Composable
fun <T> SelectionTile(
    title: String,
    selected: SelectionItem<T>?,
    items: List<SelectionItem<T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    position: ItemPosition = ItemPosition.NONE,
    leadingIcon: ImageVector? = null,
    dialogIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    SelectionTileContent(
        title = title,
        subtitle = selected?.label,
        leadingIcon = leadingIcon,
        position = position,
        enabled = enabled,
        modifier = modifier,
        onClick = { if (enabled) showDialog = true },
    )

    if (showDialog) {
        SelectionDialog(
            title = title,
            icon = dialogIcon,
            items = items,
            selected = selected,
            onSelected = {
                onSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

/**
 * 选择瓷片的内容视图
 */
@Composable
private fun SelectionTileContent(
    title: String,
    subtitle: String?,
    leadingIcon: ImageVector?,
    position: ItemPosition,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = itemPositionShape(position)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}

// endregion

// region 选择弹窗组件

/**
 * 选择弹窗 — 参考 Mindful-strong 的 _DropdownMenuDialog
 *
 * 半透明遮罩之上的居中弹窗，展示选项列表。
 * 每个选项为一条圆角容器，选中项带 Check 图标标识。
 * 弹窗弹出时有缩放+位移动画。
 *
 * @param T 选项值的类型
 * @param title 弹窗标题
 * @param icon 弹窗图标
 * @param items 可选项列表
 * @param selected 当前选中的选项
 * @param onSelected 选中后的回调
 * @param onDismiss 关闭弹窗回调
 * @param infoText 可选的说明文本
 */
@Composable
fun <T> SelectionDialog(
    title: String,
    items: List<SelectionItem<T>>,
    selected: SelectionItem<T>?,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    infoText: String? = null,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // 弹窗内容 — 带弹出动画
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300))
                + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(300),
                )
                + slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = tween(300),
                ),
            exit = slideOutVertically(
                    targetOffsetY = { it / 8 },
                    animationSpec = tween(200),
                )
                + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(200),
                )
                + fadeOut(animationSpec = tween(200)),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(horizontal = 48.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 20.dp,
                        bottom = 4.dp,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 图标
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 标题
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // 说明文字
                    if (infoText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = infoText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 选项列表容器
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        LazyColumn {
                            itemsIndexed(items = items, key = { index, _ -> index }) { index, item ->
                                val isSelected = item.value == selected?.value
                                val itemPosition = getItemPosition(index, items.size)

                                SelectionDialogItem(
                                    label = item.label,
                                    position = itemPosition,
                                    isSelected = isSelected,
                                    onClick = { onSelected(item.value) },
                                )

                                if (index < items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                        }
                    }

                    // 取消按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 选择弹窗中的单个选项
 */
@Composable
private fun SelectionDialogItem(
    label: String,
    position: ItemPosition,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = itemPositionShape(position)
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(200),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .background(backgroundColor, shape = shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = tween(150))
                + scaleIn(initialScale = 0.5f, animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
                + scaleOut(targetScale = 0.5f, animationSpec = tween(150)),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// endregion

// region 工具函数

/**
 * 根据 [ItemPosition] 返回对应的 Compose Shape
 */
private fun itemPositionShape(position: ItemPosition): Shape = when (position) {
    ItemPosition.NONE -> RoundedCornerShape(END_RADIUS.dp)
    ItemPosition.TOP -> RoundedCornerShape(
        topStart = END_RADIUS.dp,
        topEnd = END_RADIUS.dp,
        bottomEnd = CONNECTED_RADIUS.dp,
        bottomStart = CONNECTED_RADIUS.dp,
    )
    ItemPosition.MIDDLE -> RoundedCornerShape(CONNECTED_RADIUS.dp)
    ItemPosition.BOTTOM -> RoundedCornerShape(
        topStart = CONNECTED_RADIUS.dp,
        topEnd = CONNECTED_RADIUS.dp,
        bottomEnd = END_RADIUS.dp,
        bottomStart = END_RADIUS.dp,
    )
}

/**
 * 根据索引和总数计算在列表中的位置（类似参考项目的 getItemPositionInList）
 */
private fun getItemPosition(index: Int, total: Int): ItemPosition = when {
    total == 1 -> ItemPosition.NONE
    index == 0 -> ItemPosition.TOP
    index == total - 1 -> ItemPosition.BOTTOM
    else -> ItemPosition.MIDDLE
}

// endregion
