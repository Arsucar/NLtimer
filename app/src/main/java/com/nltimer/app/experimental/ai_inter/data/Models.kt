package com.nltimer.app.experimental.ai_inter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_call_logs")
data class AiCallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val status: String,
    val durationMs: Long,
    val model: String,
    val tools: String,
    val prompt: String,
    val response: String,
    val errorMessage: String? = null,
    val requestUrl: String = "",
    val requestTokens: Int = 0,
    val responseTokens: Int = 0,
    val reasoning: String = "",
    val toolCallsJson: String = ""
)

typealias AiInterConfig = com.nltimer.core.ai.config.AiInterConfig
