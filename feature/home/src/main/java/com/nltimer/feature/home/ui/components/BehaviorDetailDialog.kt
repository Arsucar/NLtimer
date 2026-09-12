package com.nltimer.feature.home.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.data.util.hhmmFormatter
import com.nltimer.core.data.util.hhmmssFormatter
import com.nltimer.core.data.util.summarizeEventValues
import com.nltimer.feature.home.model.GridCellUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val yyyyMMddHHmmssSSSFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

private fun epochToMsString(epochMs: Long?): String {
    if (epochMs == null) return "(空)"
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(yyyyMMddHHmmssSSSFormatter)
}

private fun buildExportText(cell: GridCellUiState): String {
    val stringBuilder = StringBuilder()
    stringBuilder.appendLine("=== 行为详情 ===")
    stringBuilder.appendLine("behaviorId: ${cell.behaviorId ?: "null"}")
    stringBuilder.appendLine("activityIconKey: ${cell.activityIconKey ?: "(空)"}")
    stringBuilder.appendLine("activityName: ${cell.activityName ?: "(空)"}")
    stringBuilder.appendLine("status: ${cell.status?.name ?: "null"}")
    stringBuilder.appendLine("isCurrent: ${cell.isCurrent}")
    stringBuilder.appendLine("wasPlanned: ${cell.wasPlanned}")
    stringBuilder.appendLine("isAddPlaceholder: ${cell.isAddPlaceholder}")
    stringBuilder.appendLine("tags: ${cell.tags.joinToString("、") { "id=${it.id}, name=${it.name}, color=${it.color}, isActive=${it.isActive}" }.ifEmpty { "(空)" }}")
    stringBuilder.appendLine("startTime: ${cell.startTime?.format(hhmmssFormatter) ?: "(空)"}")
    stringBuilder.appendLine("startEpochMs: ${epochToMsString(cell.startEpochMs)}")
    stringBuilder.appendLine("endTime: ${cell.endTime?.format(hhmmssFormatter) ?: "(空)"}")
    stringBuilder.appendLine("endEpochMs: ${epochToMsString(cell.endEpochMs)}")
    stringBuilder.appendLine("estimatedDuration: ${cell.estimatedDuration?.let { "${formatDuration(it)} (${it}ms)" } ?: "(空)"}")
    stringBuilder.appendLine("actualDuration: ${cell.actualDuration?.let { "${formatDuration(it)} (${it}ms)" } ?: "(空)"}")
    stringBuilder.appendLine("durationMs: ${cell.durationMs?.let { "${formatDuration(it)} (${it}ms)" } ?: "(空)"}")
    stringBuilder.appendLine("achievementLevel: ${cell.achievementLevel?.toString() ?: "(空)"}")
    stringBuilder.appendLine("pomodoroCount: ${cell.pomodoroCount}")
    stringBuilder.appendLine("note: ${cell.note ?: "(空)"}")
    return stringBuilder.toString()
}

@Composable
fun BehaviorDetailDialog(
    cell: GridCellUiState,
    events: List<BehaviorEventWithValues> = emptyList(),
    templates: List<EventTemplate> = emptyList(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "行为详情 #${cell.behaviorId ?: "N/A"}",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DetailRow("behaviorId", "${cell.behaviorId ?: "null"}")
                DetailRow("activityIconKey", cell.activityIconKey ?: "(空)")
                DetailRow("activityName", cell.activityName ?: "(空)")
                DetailRow("status", cell.status?.name ?: "null")
                DetailRow("isCurrent", "${cell.isCurrent}")
                DetailRow("wasPlanned", "${cell.wasPlanned}")
                DetailRow("isAddPlaceholder", "${cell.isAddPlaceholder}")
                DetailRow("tags", cell.tags.joinToString("、") {
                    "id=${it.id}, name=${it.name}, color=${it.color}, isActive=${it.isActive}"
                }.ifEmpty { "(空)" })
                DetailRow("startTime", cell.startTime?.format(hhmmssFormatter) ?: "(空)")
                DetailRow("startEpochMs", epochToMsString(cell.startEpochMs))
                DetailRow("endTime", cell.endTime?.format(hhmmssFormatter) ?: "(空)")
                DetailRow("endEpochMs", epochToMsString(cell.endEpochMs))
                DetailRow("estimatedDuration", cell.estimatedDuration?.let {
                    "${formatDuration(it)} (${it}ms)"
                } ?: "(空)")
                DetailRow("actualDuration", cell.actualDuration?.let {
                    "${formatDuration(it)} (${it}ms)"
                } ?: "(空)")
                DetailRow("durationMs", cell.durationMs?.let {
                    "${formatDuration(it)} (${it}ms)"
                } ?: "(空)")
                DetailRow("achievementLevel", cell.achievementLevel?.toString() ?: "(空)")
                DetailRow("pomodoroCount", "${cell.pomodoroCount}")
                DetailRow("note", cell.note.let { it ?: "(空)" })
                // 复盘事件分区（R5）：note 在上、事件时间轴在下；无事件整分区不渲染
                if (events.isNotEmpty()) {
                    DetailRow("复盘事件", "${events.size} 条")
                    events.sortedByDescending { it.event.timestamp }.forEach { item ->
                        val timeText = Instant.ofEpochMilli(item.event.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .format(hhmmFormatter)
                        val summary = listOf(
                            templates.firstOrNull { it.id == item.event.templateId }?.name,
                            item.values.summarizeEventValues().ifBlank { null },
                        ).filterNotNull().joinToString(" · ")
                        DetailRow(
                            label = timeText,
                            value = summary.ifEmpty { "无字段值" },
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(buildExportText(cell)))
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }) {
                Text("导出到剪贴板")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.43f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.57f),
        )
    }
}
