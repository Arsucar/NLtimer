package com.nltimer.core.designsystem.form.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.component.ColorPickerDialog
import com.nltimer.core.designsystem.form.FormRow

@Suppress("ComposableNaming")
@Composable
internal fun iconColorRenderer(
    _row: FormRow.IconColor,
    emoji: String,
    colorValue: String,
    onEmojiChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
) {
    val fallbackColor = MaterialTheme.colorScheme.primary
    val currentColor = remember(colorValue) {
        try {
            if (colorValue.isNotBlank()) Color(colorValue.toULong(16).toLong())
            else fallbackColor
        } catch (_: Exception) {
            fallbackColor
        }
    }

    var showEmojiEditor by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                "图标",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showEmojiEditor = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                "颜色",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .clickable { showColorPicker = true },
            )
        }
    }

    if (showEmojiEditor) {
        emojiEditDialog(
            current = emoji,
            onConfirm = { newEmoji ->
                onEmojiChange(newEmoji)
                showEmojiEditor = false
            },
            onDismiss = { showEmojiEditor = false },
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = currentColor,
            onSelect = { color ->
                val hex = (color.toArgb().toLong() and 0xFFFFFFFFL).toString(16)
                onColorChange(hex)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }
}

@Suppress("ComposableNaming")
@Composable
private fun emojiEditDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑图标") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 4) text = it },
                label = { Text("Emoji") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.ifBlank { current }) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
