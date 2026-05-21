package com.nltimer.feature.ai.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.feature.ai.chat.StreamingState
import com.nltimer.feature.ai.chat.data.ConversationMessageEntity
import com.nltimer.feature.ai.chat.markdown.MarkdownBlock
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun ChatList(
    innerPadding: PaddingValues,
    messages: List<ConversationMessageEntity>,
    streaming: StreamingState,
    isSending: Boolean,
    chatError: String?,
    listState: LazyListState,
    onCopy: (ConversationMessageEntity) -> Unit,
    onRegenerate: () -> Unit,
    onDelete: (ConversationMessageEntity) -> Unit,
    onDismissError: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(messages.size, isSending) {
        val targetIdx = if (isSending) messages.size else (messages.size - 1).coerceAtLeast(0)
        if (messages.isNotEmpty() || isSending) {
            listState.animateScrollToItem(targetIdx)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().hazeSource(state = hazeState),
        ) {
            itemsIndexed(messages, key = { _, m -> m.id }) { idx, msg ->
                ChatMessage(
                    msg = msg,
                    showActions = !(isSending && idx == messages.lastIndex),
                    onCopy = { onCopy(msg) },
                    onRegenerate = onRegenerate,
                    onDelete = { onDelete(msg) },
                )
            }
            if (isSending) {
                item("streaming") { StreamingBubble(streaming) }
            }
        }

        AnimatedVisibility(
            visible = chatError != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(innerPadding),
        ) {
            chatError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = err,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = onDismissError) { Text("关闭") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(streaming: StreamingState) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (streaming.reasoning.isNotBlank()) {
                ReasoningBlock(reasoning = streaming.reasoning, defaultExpanded = true)
            }
            if (streaming.toolCalls.isNotEmpty()) {
                ToolCallsBlock(toolCalls = streaming.toolCalls, defaultExpanded = false)
            }
            if (streaming.content.isNotBlank()) {
                MarkdownBlock(content = streaming.content, modifier = Modifier.fillMaxWidth())
            }
            if (streaming.isEmpty) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("正在生成…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            StreamingIndicator(streaming = streaming)
        }
    }
}

@Composable
private fun StreamingIndicator(streaming: StreamingState) {
    val statusText = when {
        streaming.reasoning.isNotBlank() && streaming.content.isBlank() -> "正在思考…"
        streaming.toolCalls.isNotEmpty() -> "正在调用工具…"
        streaming.content.isNotBlank() -> "正在生成回复…"
        else -> "正在连接…"
    }

    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
