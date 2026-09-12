package com.nltimer.feature.home.ui.components.moment

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 活跃卡片底部事件摘要行（R3）
 * - eventCount > 0 才渲染（未打点整行隐藏，TagNoteRow「空即隐藏」先例）
 * - 标准态：📝 N 笔 · 最新事件摘要 ⌄（约 22-24dp 单行）
 * - 紧凑态（compact，cardHeight 较小）：仅 📝N 徽标，避免挤压滑条区
 */
@Composable
internal fun EventSummaryRow(
    eventCount: Int,
    latestSummary: String?,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    if (eventCount <= 0) return
    if (compact) {
        EventCountBadge(
            eventCount = eventCount,
            modifier = modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        )
    } else {
        Box(
            modifier = modifier
                .heightIn(min = 24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .animateContentSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "📝 $eventCount 笔",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                latestSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                    Box(
                        modifier = Modifier.height(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "查看事件",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 紧凑降级徽标：只显示 📝N（cardHeight < 阈值时） */
@Composable
private fun EventCountBadge(
    eventCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "📝$eventCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
