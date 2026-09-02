package com.nltimer.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nltimer.core.designsystem.theme.appOutlinedTextFieldColors

@Composable
fun ArchiveConfirmDialog(
    title: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String?) -> Unit,
) {
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("可留空，例如看完后的想法") },
                minLines = 6,
                maxLines = 12,
                colors = appOutlinedTextFieldColors(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note.trim().ifBlank { null }) },
            ) {
                Text("确认归档")
            }
        },
    )
}
