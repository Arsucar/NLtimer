package com.nltimer.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun ArchiveItemDetailDialog(
    title: String,
    archivedAtText: String,
    archiveNote: String?,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(archivedAtText)
                Text("感想：${archiveNote?.takeIf { it.isNotBlank() } ?: "无"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onRestore) {
                Text("恢复")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
