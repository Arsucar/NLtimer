package com.nltimer.feature.ai.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nltimer.feature.ai.chat.export.ExportFormat
import com.nltimer.feature.ai.chat.export.ExportOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    onDismiss: () -> Unit,
    onExport: (ExportFormat, ExportOptions) -> String,
) {
    var format by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    var includeTools by remember { mutableStateOf(true) }
    var includeReasoning by remember { mutableStateOf(true) }
    val preview = remember(format, includeTools, includeReasoning) {
        onExport(format, ExportOptions(includeTools, includeReasoning))
    }
    val context: Context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("导出当前对话", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("格式：")
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = format == ExportFormat.MARKDOWN,
                    onClick = { format = ExportFormat.MARKDOWN },
                    label = { Text("Markdown") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = format == ExportFormat.JSON,
                    onClick = { format = ExportFormat.JSON },
                    label = { Text("JSON") },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeTools, onCheckedChange = { includeTools = it })
                Text("包含工具调用详细信息")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeReasoning, onCheckedChange = { includeReasoning = it })
                Text("包含 reasoning 思考过程")
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
            ) {
                Text(
                    text = preview.take(2000) + if (preview.length > 2000) "\n…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("AI 对话", preview))
                    onDismiss()
                }) { Text("复制") }
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (format == ExportFormat.JSON) "application/json" else "text/plain"
                        putExtra(Intent.EXTRA_TEXT, preview)
                    }
                    context.startActivity(Intent.createChooser(intent, "分享对话"))
                    onDismiss()
                }) { Text("分享") }
            }
        }
    }
}
