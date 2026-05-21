package com.nltimer.core.ai.config

data class AiInterConfig(
    val apiAddress: String = "https://integrate.api.nvidia.com/v1",
    val apiPath: String = "/chat/completions",
    val apiKey: String = "",
    val modelName: String = "openai/gpt-oss-120b",
    val promptNotes: String = "",
    val promptTaskGen: String = "",
    val promptChat: String = "",
    val promptSystem: String = "",
    val maxToolRounds: Int = 5,
    val maxBatchSize: Int = 20,
)
