package com.nltimer.feature.tag_management.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.util.formatTimestamp
import com.nltimer.core.data.util.summarizeEventValues
import com.nltimer.core.designsystem.component.AppTagChip
import com.nltimer.core.designsystem.component.AppTagChipStyle
import com.nltimer.core.designsystem.component.ConfirmDialog

/**
 * 标签关联事件弹层（R4 查看端，EditTagFormSheet「查看关联事件」行入口）
 * 实现形态：只读列表 AlertDialog（时间戳 + 模板徽标 + 字段值摘要），单条删除走 ConfirmDialog 二次确认；
 * 编辑动作省略——事件编辑入口在首页事件列表（报告决策）
 */
@Composable
fun TagEventsDialog(
    tag: Tag,
    events: List<BehaviorEventWithValues>,
    templates: List<EventTemplate>,
    onDismiss: () -> Unit,
    onDeleteEvent: (Long) -> Unit,
) {
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    pendingDeleteId?.let { eventId ->
        ConfirmDialog(
            title = "删除事件",
            message = "确定要删除这条事件及其字段值吗？此操作不可撤销。",
            confirmText = "删除",
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                onDeleteEvent(eventId)
                pendingDeleteId = null
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「${tag.name}」的关联事件（${events.size}）") },
        text = {
            if (events.isEmpty()) {
                Text(
                    text = "还没有与该标签相关的事件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(events, key = { it.event.id }) { item ->
                        TagEventRow(
                            item = item,
                            templateName = templates.firstOrNull { it.id == item.event.templateId }?.name,
                            onDelete = { pendingDeleteId = item.event.id },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun TagEventRow(
    item: BehaviorEventWithValues,
    templateName: String?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(item.event.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val summary = buildList {
                    templateName?.let { add(it) }
                    item.values.summarizeEventValues().takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" · ")
                Text(
                    text = summary.ifEmpty { "无字段值" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (templateName != null) {
                AppTagChip(
                    label = templateName,
                    color = null,
                    style = AppTagChipStyle.Compact,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "删除事件",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
