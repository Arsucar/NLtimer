package com.nltimer.feature.ai.chat.export

enum class ExportFormat {
    MARKDOWN,
    JSON,
}

data class ExportOptions(
    val includeTools: Boolean = true,
    val includeReasoning: Boolean = true,
)
