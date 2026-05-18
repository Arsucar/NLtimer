package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    conversations: List<ConversationEntity>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var actionTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("对话", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCreate) { Icon(Icons.Default.Add, "新建对话") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                items(conversations, key = { it.id }) { conv ->
                    val selected = conv.id == currentId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSelect(conv.id) },
                                onLongClick = { actionTarget = conv },
                            ),
                    ) {
                        Text(
                            text = conv.title.ifBlank { "新对话" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    actionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(target.title.ifBlank { "新对话" }) },
            text = { Text("选择操作") },
            confirmButton = {
                TextButton(onClick = { renameTarget = target; actionTarget = null }) { Text("重命名") }
            },
            dismissButton = {
                TextButton(onClick = { onDelete(target.id); actionTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    renameTarget?.let { target ->
        var input by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名对话") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target.id, input)
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
        )
    }
}
