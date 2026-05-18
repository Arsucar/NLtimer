package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import com.nltimer.app.experimental.ai_inter.chat.markdown.MarkdownBlock
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ChatMessage(
    msg: ConversationMessageEntity,
    showActions: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = msg.role == "user"
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isUser) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 340.dp),
            ) {
                SelectionContainer {
                    Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        MarkdownBlock(content = msg.content)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (msg.reasoning.isNotBlank()) {
                    ReasoningBlock(reasoning = msg.reasoning, defaultExpanded = false)
                }
                val tools = parseToolCalls(msg.toolCallsJson)
                if (tools.isNotEmpty()) {
                    ToolCallsBlock(toolCalls = tools, defaultExpanded = false)
                }
                SelectionContainer {
                    MarkdownBlock(content = msg.content, modifier = Modifier.fillMaxWidth())
                }
                MessageMetadata(msg = msg)
            }
        }

        AnimatedVisibility(visible = showActions, enter = fadeIn(), exit = fadeOut()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "复制", Modifier.size(18.dp)) }
                if (!isUser) {
                    IconButton(onClick = onRegenerate) { Icon(Icons.Default.Refresh, "重生成", Modifier.size(18.dp)) }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun MessageMetadata(msg: ConversationMessageEntity) {
    val hasToolCalls = msg.toolCallsJson.isNotBlank() && msg.toolCallsJson != "[]"
    val hasReasoning = msg.reasoning.isNotBlank()

    if (hasToolCalls || hasReasoning) {
        Row(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasReasoning) {
                Text(
                    text = "思考完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (hasToolCalls) {
                val toolCalls = parseToolCalls(msg.toolCallsJson)
                Text(
                    text = "${toolCalls.size} 个工具调用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private val toolCallsJson = Json { ignoreUnknownKeys = true }

private fun parseToolCalls(json: String): List<ToolCallRecord> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val arr: JsonArray = toolCallsJson.parseToJsonElement(json).jsonArray
        arr.map { el ->
            val o = el.jsonObject
            ToolCallRecord(
                id = o["id"]?.jsonPrimitive?.content.orEmpty(),
                name = o["name"]?.jsonPrimitive?.content.orEmpty(),
                arguments = o["arguments"]?.jsonPrimitive?.content.orEmpty(),
                result = o["result"]?.jsonPrimitive?.content.orEmpty(),
                success = o["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                durationMs = o["durationMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            )
        }
    }.getOrDefault(emptyList())
}
