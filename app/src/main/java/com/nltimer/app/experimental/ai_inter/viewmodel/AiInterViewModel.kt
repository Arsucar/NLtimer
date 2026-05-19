package com.nltimer.app.experimental.ai_inter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import com.nltimer.app.experimental.ai_inter.network.StreamEvent
import com.nltimer.app.experimental.ai_inter.network.toOpenAiFunctionJson
import com.nltimer.core.tools.ToolConfig
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolRegistry
import com.nltimer.core.tools.ToolResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** 工具一次调用的记录，写库与展示共用 */
data class ToolCallRecord(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String,
    val success: Boolean,
    val durationMs: Long,
)

data class ChatMessage(
    val role: String,
    val content: String,
    /** 模型推理过程，可空；UI 折叠展示 */
    val reasoning: String = "",
    /** 本轮（含子轮循环）工具调用，按发生顺序排列 */
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)

/** 流式中累计的临时状态，发送给 UI 让其分块渲染 */
data class StreamingState(
    val reasoning: String = "",
    val content: String = "",
    val toolCalls: List<ToolCallRecord> = emptyList(),
) {
    val isEmpty: Boolean get() = reasoning.isEmpty() && content.isEmpty() && toolCalls.isEmpty()
}

@HiltViewModel
class AiInterViewModel @Inject constructor(
    private val repository: AiInterRepository,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry,
    private val toolConfig: ToolConfig,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val config: StateFlow<AiInterConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiInterConfig()
    ).also { flow ->
        viewModelScope.launch {
            flow.collect { cfg -> toolConfig.maxBatchSize = cfg.maxBatchSize }
        }
    }

    val logs: StateFlow<List<AiCallLogEntity>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _streamingState = MutableStateFlow(StreamingState())
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _modelsError = MutableStateFlow<String?>(null)
    val modelsError: StateFlow<String?> = _modelsError.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private var currentStreamJob: Job? = null

    fun getLogById(id: Long): Flow<AiCallLogEntity?> = repository.getLogById(id)

    fun getAllTools(): List<ToolDefinition> = toolRegistry.getAllTools()

    fun updateConfig(apiAddress: String, apiPath: String, apiKey: String, modelName: String) {
        viewModelScope.launch {
            repository.updateConfig {
                it.copy(
                    apiAddress = apiAddress,
                    apiPath = apiPath,
                    apiKey = apiKey,
                    modelName = modelName
                )
            }
        }
    }

    fun updatePrompts(notes: String, taskGen: String, chat: String, system: String) {
        viewModelScope.launch {
            repository.updateConfig {
                it.copy(
                    promptNotes = notes,
                    promptTaskGen = taskGen,
                    promptChat = chat,
                    promptSystem = system
                )
            }
        }
    }

    fun updateMaxToolRounds(rounds: Int) {
        val clamped = rounds.coerceIn(1, 15)
        viewModelScope.launch {
            repository.updateConfig { it.copy(maxToolRounds = clamped) }
        }
    }

    fun updateMaxBatchSize(size: Int) {
        val clamped = size.coerceIn(1, 200)
        viewModelScope.launch {
            repository.updateConfig { it.copy(maxBatchSize = clamped) }
            toolConfig.maxBatchSize = clamped
        }
    }

    fun clearLogs() {
        viewModelScope.launch { repository.clearLogs() }
    }

    fun clearChat() {
        currentStreamJob?.cancel()
        _chatMessages.value = emptyList()
        _streamingState.value = StreamingState()
        _isSending.value = false
        _chatError.value = null
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun fetchModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _modelsError.value = null
            val cfg = config.value
            apiClient.fetchModels(cfg.apiAddress, cfg.apiKey).fold(
                onSuccess = { models ->
                    _availableModels.value = models
                    if (models.isEmpty()) {
                        _modelsError.value = "服务端返回的模型列表为空（响应已成功，但 data 数组为空）"
                    }
                },
                onFailure = { e ->
                    _modelsError.value = e.message ?: (e::class.simpleName ?: "未知错误") + "（无错误描述）"
                    _availableModels.value = emptyList()
                }
            )
            _isLoadingModels.value = false
        }
    }

    fun clearModelsError() {
        _modelsError.value = null
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return
        _isSending.value = true
        _streamingState.value = StreamingState()
        _chatError.value = null

        val userMessage = ChatMessage(role = "user", content = text)
        _chatMessages.value = _chatMessages.value + userMessage

        val cfg = config.value
        val toolDefs = toolRegistry.getAllTools()
        val toolsJson: JsonArray? = if (toolDefs.isNotEmpty()) {
            buildJsonArray { toolDefs.forEach { add(it.toOpenAiFunctionJson()) } }
        } else null

        // wire-format 历史：role/content（含 system）+ 历史 assistant tool_calls + tool 回执
        val wireHistory: MutableList<JsonObject> = mutableListOf()
        // 强制系统消息：当前时间锚点 + 工具规则 + 用户自定义追加
        val nowIso = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val systemContent = buildString {
            append("当前时间: ")
            append(nowIso)
            append("\n\n")
            if (cfg.promptSystem.isNotBlank()) {
                append(cfg.promptSystem)
            } else {
                append(AiChatToolHelper.TOOLS_SYSTEM_PROMPT)
            }
            if (cfg.promptChat.isNotBlank()) {
                append("\n\n# 附加指引\n")
                append(cfg.promptChat)
            }
        }
        wireHistory += buildJsonObject {
            put("role", "system")
            put("content", systemContent)
        }
        _chatMessages.value.forEach { msg ->
            wireHistory += buildJsonObject {
                put("role", msg.role)
                put("content", msg.content)
            }
        }

        val startTime = System.currentTimeMillis()
        val fullUrl = cfg.apiAddress.trimEnd('/') + cfg.apiPath

        currentStreamJob = viewModelScope.launch {
            val finalReasoning = StringBuilder()
            val finalContent = StringBuilder()
            val allToolCalls: MutableList<ToolCallRecord> = mutableListOf()
            var errorMsg: String? = null
            var wasCancelled = false

            try {
                var round = 0
                while (round < cfg.maxToolRounds) {
                    val roundReasoning = StringBuilder()
                    val roundContent = StringBuilder()
                    val toolBuffers: MutableMap<Int, ToolCallBuffer> = mutableMapOf()

                    apiClient.streamChat(
                        baseUrl = cfg.apiAddress,
                        path = cfg.apiPath,
                        apiKey = cfg.apiKey,
                        model = cfg.modelName,
                        messagesJson = JsonArray(wireHistory),
                        toolsJson = toolsJson,
                    ).collect { event ->
                        when (event) {
                            is StreamEvent.Content -> {
                                roundContent.append(event.text)
                                finalContent.append(event.text)
                                emitStreamingState(
                                    reasoning = finalReasoning.toString() + roundReasoning.toString(),
                                    content = finalContent.toString(),
                                    toolCalls = allToolCalls,
                                )
                            }
                            is StreamEvent.Reasoning -> {
                                roundReasoning.append(event.text)
                                emitStreamingState(
                                    reasoning = finalReasoning.toString() + roundReasoning.toString(),
                                    content = finalContent.toString(),
                                    toolCalls = allToolCalls,
                                )
                            }
                            is StreamEvent.ToolCallDelta -> {
                                val buf = toolBuffers.getOrPut(event.index) { ToolCallBuffer() }
                                event.id?.let { buf.id = it }
                                event.name?.let { buf.name = it }
                                buf.arguments.append(event.argumentsChunk)
                            }
                        }
                    }

                    // 本轮 reasoning 累积到最终
                    if (roundReasoning.isNotEmpty()) {
                        if (finalReasoning.isNotEmpty()) finalReasoning.append("\n")
                        finalReasoning.append(roundReasoning)
                    }

                    if (toolBuffers.isEmpty()) break

                    // 把 assistant 的 tool_calls 消息写入 wire 历史
                    val assistantToolMsg = AiChatToolHelper.buildAssistantToolMessage(
                        content = roundContent.toString(),
                        toolBuffers = toolBuffers,
                    )
                    wireHistory += assistantToolMsg

                    // 执行每个工具调用，把结果作为 role=tool 消息塞回历史
                    toolBuffers.toSortedMap().forEach { (_, buf) ->
                        val record = AiChatToolHelper.executeToolCall(buf, toolRegistry)
                        allToolCalls += record
                        emitStreamingState(
                            reasoning = finalReasoning.toString(),
                            content = finalContent.toString(),
                            toolCalls = allToolCalls,
                        )
                        wireHistory += AiChatToolHelper.buildToolResultMessage(record)
                    }

                    round++
                }
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            } catch (e: Exception) {
                errorMsg = e.message ?: (e::class.simpleName ?: "Unknown error")
            } finally {
                val duration = System.currentTimeMillis() - startTime
                val finalContentStr = finalContent.toString()
                val finalReasoningStr = finalReasoning.toString()
                val finalToolCalls = allToolCalls.toList()

                if (wasCancelled) {
                    if (finalContentStr.isNotEmpty() || finalReasoningStr.isNotEmpty() || finalToolCalls.isNotEmpty()) {
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            role = "assistant",
                            content = if (finalContentStr.isNotEmpty()) "$finalContentStr\n\n(已中断)" else "(已中断)",
                            reasoning = finalReasoningStr,
                            toolCalls = finalToolCalls,
                        )
                    }
                    _streamingState.value = StreamingState()
                    _isSending.value = false
                } else {
                    val assistantContent = when {
                        finalContentStr.isNotEmpty() && errorMsg == null -> finalContentStr
                        finalContentStr.isNotEmpty() && errorMsg != null -> "$finalContentStr\n\n(中途出错：$errorMsg)"
                        errorMsg != null -> "(请求失败：$errorMsg)"
                        finalToolCalls.isNotEmpty() -> "(已完成 ${finalToolCalls.size} 次工具调用，但模型未给出文本回复)"
                        else -> "(空响应 — 服务端正常关闭流但未发送任何 token。请确认：模型名「${cfg.modelName}」是否存在、API 路径「${cfg.apiPath}」是否正确、API Key 是否有效)"
                    }
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        role = "assistant",
                        content = assistantContent,
                        reasoning = finalReasoningStr,
                        toolCalls = finalToolCalls,
                    )
                    _streamingState.value = StreamingState()

                    if (errorMsg != null) {
                        _chatError.value = errorMsg
                    }

                    repository.addLog(
                        AiCallLogEntity(
                            timestamp = System.currentTimeMillis(),
                            type = "Test Chat",
                            status = if (errorMsg == null && (finalContentStr.isNotEmpty() || finalToolCalls.isNotEmpty())) "Success" else "Failed",
                            durationMs = duration,
                            model = cfg.modelName,
                            tools = finalToolCalls.joinToString(",") { it.name },
                            prompt = text,
                            response = finalContentStr,
                            errorMessage = errorMsg,
                            requestUrl = fullUrl,
                            reasoning = finalReasoningStr,
                            toolCallsJson = AiChatToolHelper.serializeToolCalls(finalToolCalls),
                        )
                    )

                    _isSending.value = false
                }
            }
        }
    }

    fun stopStreaming() {
        currentStreamJob?.cancel()
        val partial = _streamingState.value
        if (!partial.isEmpty) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                role = "assistant",
                content = if (partial.content.isNotEmpty()) "${partial.content}\n\n(已中断)" else "(已中断)",
                reasoning = partial.reasoning,
                toolCalls = partial.toolCalls,
            )
        }
        _streamingState.value = StreamingState()
        _isSending.value = false
    }

    override fun onCleared() {
        super.onCleared()
        currentStreamJob?.cancel()
    }

    private fun emitStreamingState(
        reasoning: String,
        content: String,
        toolCalls: List<ToolCallRecord>,
    ) {
        _streamingState.value = StreamingState(
            reasoning = reasoning,
            content = content,
            toolCalls = toolCalls.toList(),
        )
    }

}
