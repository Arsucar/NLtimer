package com.nltimer.app.experimental.ai_inter.chat.export

enum class ExportFormat {
    MARKDOWN,
    JSON,
}

data class ExportOptions(
    val includeTools: Boolean = true,
    val includeReasoning: Boolean = true,
)
