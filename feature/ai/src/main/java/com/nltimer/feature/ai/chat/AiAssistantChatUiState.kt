package com.nltimer.feature.ai.chat

import com.nltimer.core.ai.toolcall.ToolCallRecord

data class StreamingState(
    val reasoning: String = "",
    val content: String = "",
    val toolCalls: List<ToolCallRecord> = emptyList(),
) {
    val isEmpty: Boolean get() = reasoning.isEmpty() && content.isEmpty() && toolCalls.isEmpty()
}
