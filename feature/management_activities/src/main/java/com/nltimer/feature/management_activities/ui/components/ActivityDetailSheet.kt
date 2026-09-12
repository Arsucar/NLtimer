package com.nltimer.feature.management_activities.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.ActivityStats
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.util.formatDurationMinutes
import com.nltimer.core.data.util.formatTimestamp
import com.nltimer.core.data.util.summarizeEventValues
import com.nltimer.core.designsystem.component.AppTagChip
import com.nltimer.core.designsystem.component.AppTagChipStyle
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.debugui.FieldDetailDialog
import com.nltimer.core.debugui.toFieldInfoList
import com.nltimer.core.debugui.toJsonString


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailSheet(
    activity: Activity,
    stats: ActivityStats,
    _allGroups: List<ActivityGroup>,
    onDismiss: () -> Unit,
    onEdit: (Activity) -> Unit,
    onDelete: () -> Unit,
    events: List<BehaviorEventWithValues> = emptyList(),
    eventTemplates: List<EventTemplate> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFieldDetail by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconRenderer(
                        iconKey = activity.iconKey,
                        defaultEmoji = "📌",
                        iconSize = 48.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = activity.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    val context = LocalContext.current
                    val isDebug = remember(context) {
                        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    }
                    if (isDebug) {
                        IconButton(onClick = { showFieldDetail = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "字段详细",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = { onEdit(activity) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "简单统计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRow("累计使用次数", "${stats.usageCount} 次")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    StatRow("累计总时长", formatDurationMinutes(stats.totalDurationMinutes))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    StatRow("最近一次使用", stats.lastUsedTimestamp?.let { if (it == 0L) "从未使用" else formatTimestamp(it) } ?: "从未使用")
                }
            }

            EventSection(events = events, templates = eventTemplates)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showFieldDetail) {
        val fields = remember(activity) { activity.toFieldInfoList() }
        val rawJson = remember(fields) { fields.toJsonString() }

        FieldDetailDialog(
            title = "活动字段详情",
            fields = fields,
            rawJson = rawJson,
            onDismiss = { showFieldDetail = false },
        )
    }
}

/**
 * 「复盘事件 (N)」区块：简单统计之后展示（PRD R4 活动详情入口）
 * 空事件整分区不渲染（TagNoteRow「空则隐藏」先例）；默认最近 [EVENT_PREVIEW_COUNT] 条 + 查看全部展开
 */
private const val EVENT_PREVIEW_COUNT = 3

@Composable
private fun EventSection(
    events: List<BehaviorEventWithValues>,
    templates: List<EventTemplate>,
) {
    if (events.isEmpty()) return
    var expanded by remember(events) { mutableStateOf(false) }
    val sorted = events.sortedByDescending { it.event.timestamp }
    val displayed = if (expanded) sorted else sorted.take(EVENT_PREVIEW_COUNT)

    Column {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "复盘事件（${events.size}）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (sorted.size > EVENT_PREVIEW_COUNT) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "收起" else "查看全部")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            displayed.forEach { item ->
                ActivityEventRow(
                    item = item,
                    templateName = templates.firstOrNull { it.id == item.event.templateId }?.name,
                )
            }
        }
    }
}

@Composable
private fun ActivityEventRow(
    item: BehaviorEventWithValues,
    templateName: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppTagChip(
                    label = templateName ?: "未知模板",
                    color = null,
                    style = AppTagChipStyle.Compact,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(item.event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val summary = item.values.summarizeEventValues()
            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

