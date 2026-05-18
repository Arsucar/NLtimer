package com.nltimer.app.experimental.ai_inter.chat

import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord

data class StreamingState(
    val reasoning: String = "",
    val content: String = "",
    val toolCalls: List<ToolCallRecord> = emptyList(),
) {
    val isEmpty: Boolean get() = reasoning.isEmpty() && content.isEmpty() && toolCalls.isEmpty()
}
