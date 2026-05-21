package com.nltimer.core.ai.toolcall

data class ToolCallRecord(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String,
    val success: Boolean,
    val durationMs: Long,
)
