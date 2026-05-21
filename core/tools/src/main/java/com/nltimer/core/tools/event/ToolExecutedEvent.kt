package com.nltimer.core.tools.event

import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolResult

data class ToolExecutedEvent(
    val toolName: String,
    val category: ToolCategory,
    val result: ToolResult,
    val timestamp: Long = System.currentTimeMillis(),
)
