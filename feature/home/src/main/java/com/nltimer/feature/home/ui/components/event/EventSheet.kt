package com.nltimer.feature.home.ui.components.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.util.formatTimestamp
import com.nltimer.core.data.util.summarizeEventValues
import com.nltimer.core.designsystem.component.ConfirmDialog
import com.nltimer.feature.home.model.EventSheetPage
import com.nltimer.feature.home.model.EventSheetTarget

private const val ScrimAlpha = 0.32f

internal fun summarizeValues(values: List<BehaviorEventValue>): String =
    values.summarizeEventValues()

/**
 * 事件 BottomSheet（「+ 记一笔」/ 摘要行列表）
 * 默认停在 PartiallyExpanded（约半屏），上滑看更多内容才进入 Expanded。
 * dragHandle 保留以便手势提示；内部双页态（列表 ↔ 表单）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventSheet(
    target: EventSheetTarget,
    templates: List<EventTemplate>,
    events: List<BehaviorEventWithValues>,
    onDismiss: () -> Unit,
    onSave: (EventSheetTarget, Long, Boolean, List<BehaviorEventValue>) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenEdit: (Long) -> Unit,
    onOpenNew: () -> Unit,
    onQueryFields: suspend (Long) -> List<EventTemplateField>,
    onQueryEvent: suspend (Long) -> BehaviorEventWithValues?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var page by remember(target) { mutableStateOf(target.initialPage) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            when (page) {
                EventSheetPage.LIST -> EventListPage(
                    events = events,
                    templates = templates,
                    onOpenNew = onOpenNew,
                    onOpenEdit = onOpenEdit,
                    onDeleteEvent = onDelete,
                )
                EventSheetPage.FORM -> EventFormPage(
                    target = target,
                    templates = templates,
                    onQueryFields = onQueryFields,
                    onQueryEvent = onQueryEvent,
                    onSubmit = { templateId, attachToBehavior, values ->
                        onSave(target, templateId, attachToBehavior, values)
                        onDismiss()
                    },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun EventListPage(
    events: List<BehaviorEventWithValues>,
    templates: List<EventTemplate>,
    onOpenNew: () -> Unit,
    onOpenEdit: (Long) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }

    deleteConfirmId?.let { eventId ->
        ConfirmDialog(
            title = "删除事件",
            message = "确定要删除这条事件及其字段值吗？此操作不可撤销。",
            confirmText = "删除",
            confirmTextColor = MaterialTheme.colorScheme.error,
            onDismiss = { deleteConfirmId = null },
            onConfirm = {
                onDeleteEvent(eventId)
                deleteConfirmId = null
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "事件记录（${events.size}）",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenNew) { Text("记一笔") }
        }

        if (events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "该行为还没有事件记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenNew) { Text("记一笔") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 8.dp, top = 4.dp, bottom = 16.dp,
                ),
            ) {
                items(events, key = { it.event.id }) { item ->
                    EventListItem(
                        item = item,
                        templateName = templates
                            .firstOrNull { it.id == item.event.templateId }?.name,
                        onClick = { onOpenEdit(item.event.id) },
                        onDelete = { deleteConfirmId = item.event.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventListItem(
    item: BehaviorEventWithValues,
    templateName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(item.event.timestamp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(2.dp))
                val summary = buildList {
                    templateName?.let { add(it) }
                    summarizeValues(item.values).takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" · ")
                Text(
                    text = summary.ifEmpty { "无字段值" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "删除事件",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑事件",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
