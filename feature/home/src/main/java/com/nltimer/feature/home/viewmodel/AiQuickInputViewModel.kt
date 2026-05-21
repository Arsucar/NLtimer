package com.nltimer.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.ai.config.AiConfigProvider
import com.nltimer.core.ai.config.AiInterConfig
import com.nltimer.core.ai.network.AiInterApiClient
import com.nltimer.core.ai.network.StreamEvent
import com.nltimer.core.ai.network.toOpenAiFunctionJson
import com.nltimer.core.ai.toolcall.AiChatToolHelper
import com.nltimer.core.ai.toolcall.ToolCallBuffer
import com.nltimer.core.tools.ToolRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class AiQuickInputState {
    object Idle : AiQuickInputState()
    data class Streaming(val text: String, val toolCalls: List<ToolCallCard>) : AiQuickInputState()
    data class Done(val text: String, val toolCalls: List<ToolCallCard>) : AiQuickInputState()
    data class Error(val message: String) : AiQuickInputState()
}

data class ToolCallCard(
    val name: String,
    val success: Boolean,
    val summary: String,
)

@HiltViewModel
class AiQuickInputViewModel @Inject constructor(
    private val configProvider: AiConfigProvider,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry,
) : ViewModel() {

    val config: StateFlow<AiInterConfig> = configProvider.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiInterConfig(),
    )

    private val _state = MutableStateFlow<AiQuickInputState>(AiQuickInputState.Idle)
    val state: StateFlow<AiQuickInputState> = _state.asStateFlow()

    private var streamJob: Job? = null

    fun send(text: String) {
        if (text.isBlank() || _state.value is AiQuickInputState.Streaming) return
        streamJob = viewModelScope.launch {
            _state.value = AiQuickInputState.Streaming("", emptyList())
            try {
                val cfg = configProvider.config.first()
                val systemPrompt = buildSystemPrompt(cfg)
                val wireHistory = mutableListOf(
                    buildJsonObject { put("role", "system"); put("content", systemPrompt) },
                    buildJsonObject { put("role", "user"); put("content", text) },
                )
                val toolsJson = toolRegistry.getAllTools()
                    .map { it.toOpenAiFunctionJson() }
                    .let { JsonArray(it) }

                var round = 0
                val allToolCalls = mutableListOf<ToolCallCard>()
                val fullContent = StringBuilder()

                while (round < cfg.maxToolRounds) {
                    val roundContent = StringBuilder()
                    val toolBuffers = mutableMapOf<Int, ToolCallBuffer>()

                    apiClient.streamChat(
                        cfg.apiAddress, cfg.apiPath, cfg.apiKey,
                        cfg.modelName, JsonArray(wireHistory), toolsJson,
                    ).collect { event ->
                        when (event) {
                            is StreamEvent.Content -> {
                                roundContent.append(event.text)
                                fullContent.append(event.text)
                                _state.value = AiQuickInputState.Streaming(
                                    fullContent.toString(), allToolCalls.toList()
                                )
                            }
                            is StreamEvent.ToolCallDelta -> {
                                val buf = toolBuffers.getOrPut(event.index) { ToolCallBuffer() }
                                event.id?.let { buf.id = it }
                                event.name?.let { buf.name = it }
                                buf.arguments.append(event.argumentsChunk)
                            }
                            is StreamEvent.Reasoning -> { }
                        }
                    }

                    if (toolBuffers.isEmpty()) break

                    wireHistory += AiChatToolHelper.buildAssistantToolMessage(
                        roundContent.toString(), toolBuffers
                    )

                    toolBuffers.forEach { (_, buf) ->
                        val record = AiChatToolHelper.executeToolCall(buf, toolRegistry)
                        allToolCalls += ToolCallCard(
                            name = record.name,
                            success = record.success,
                            summary = if (record.success) "成功" else record.result.take(100),
                        )
                        _state.value = AiQuickInputState.Streaming(
                            fullContent.toString(), allToolCalls.toList()
                        )
                        wireHistory += AiChatToolHelper.buildToolResultMessage(record)
                    }
                    round++
                }

                _state.value = AiQuickInputState.Done(fullContent.toString(), allToolCalls.toList())
            } catch (e: CancellationException) {
                _state.value = AiQuickInputState.Idle
                throw e
            } catch (e: Exception) {
                _state.value = AiQuickInputState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun cancelStream() {
        streamJob?.cancel()
    }

    fun reset() {
        streamJob?.cancel()
        _state.value = AiQuickInputState.Idle
    }

    private fun buildSystemPrompt(cfg: AiInterConfig): String {
        val now = java.time.OffsetDateTime.now()
            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val base = AiChatToolHelper.TOOLS_SYSTEM_PROMPT
        val custom = cfg.promptChat
        return buildString {
            append("当前时间：$now\n\n")
            append(base)
            if (custom.isNotBlank()) {
                append("\n\n## 用户自定义提示词\n$custom")
            }
        }
    }
}
