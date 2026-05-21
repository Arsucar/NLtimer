package com.nltimer.core.ai.network

sealed class StreamEvent {
    data class Content(val text: String) : StreamEvent()

    data class Reasoning(val text: String) : StreamEvent()

    data class ToolCallDelta(
        val index: Int,
        val id: String?,
        val name: String?,
        val argumentsChunk: String,
    ) : StreamEvent()
}
