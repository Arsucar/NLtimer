package com.nltimer.app.experimental.ai_inter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_call_logs")
data class AiCallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String, // Chat, Task, Plan, etc.
    val status: String, // Success, Failed
    val durationMs: Long,
    val model: String,
    val tools: String, // Comma separated tool names
    val prompt: String,
    val response: String,
    val errorMessage: String? = null
)

data class AiInterConfig(
    val apiAddress: String = "https://integrate.api.nvidia.com/v1",
    val apiPath: String = "/chat/completions",
    val apiKey: String = "",
    val modelName: String = "openai/gpt-oss-120b"
)
