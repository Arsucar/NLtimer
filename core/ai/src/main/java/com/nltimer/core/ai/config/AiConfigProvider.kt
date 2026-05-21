package com.nltimer.core.ai.config

import kotlinx.coroutines.flow.Flow

interface AiConfigProvider {
    val config: Flow<AiInterConfig>
    suspend fun updateConfig(update: (AiInterConfig) -> AiInterConfig)
}
