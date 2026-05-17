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
    /** 模型推理过程（reasoning_content），可能为空 */
    val reasoning: String = "",
    /**
     * 工具调用明细 JSON 数组字符串；空字符串表示当轮未触发工具调用。
     * 每个元素：{"name":..., "arguments":..., "result":..., "success":..., "durationMs":...}
     */
    val toolCallsJson: String = ""
)

data class AiInterConfig(
    val apiAddress: String = "https://integrate.api.nvidia.com/v1",
    val apiPath: String = "/chat/completions",
    val apiKey: String = "",
    val modelName: String = "openai/gpt-oss-120b",
    val promptNotes: String = "",
    val promptTaskGen: String = "",
    val promptChat: String = ""
)
