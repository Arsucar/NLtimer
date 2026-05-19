package com.nltimer.app.experimental.ai_inter.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import com.nltimer.app.experimental.ai_inter.chat.export.ConversationExporter
import com.nltimer.app.experimental.ai_inter.chat.export.ExportFormat
import com.nltimer.app.experimental.ai_inter.chat.export.ExportOptions
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import com.nltimer.app.experimental.ai_inter.network.StreamEvent
import com.nltimer.app.experimental.ai_inter.network.toOpenAiFunctionJson
import com.nltimer.app.experimental.ai_inter.viewmodel.AiChatToolHelper
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallBuffer
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord
import com.nltimer.core.tools.ToolRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AiAssistantChatViewModel @Inject constructor(
    private val repository: AiInterRepository,
    private val conversationDao: ConversationDao,
    private val messageDao: ConversationMessageDao,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry,
    private val exporter: ConversationExporter,
) : ViewModel() {

    val config: StateFlow<AiInterConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiInterConfig(),
    )

    val conversations: StateFlow<List<ConversationEntity>> = conversationDao.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    val currentMessages: StateFlow<List<ConversationMessageEntity>> = _currentConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else messageDao.observeByConversation(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _streamingState = MutableStateFlow(StreamingState())
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private var streamJob: Job? = null

    init {
        newConversation()
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
    }

    fun newConversation() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            conversationDao.upsert(ConversationEntity(id, "新对话", now, now))
            _currentConversationId.value = id
        }
    }

    fun renameConversation(id: String, title: String) {
        viewModelScope.launch {
            val trimmed = title.trim().ifBlank { "新对话" }
            conversationDao.rename(id, trimmed, System.currentTimeMillis())
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.delete(id)
            if (_currentConversationId.value == id) {
                val remaining = conversationDao.observeAll().first()
                _currentConversationId.value = remaining.firstOrNull()?.id
                if (_currentConversationId.value == null) {
                    newConversation()
                }
            }
        }
    }

    fun clearCurrent() {
        viewModelScope.launch {
            val id = _currentConversationId.value ?: return@launch
            messageDao.deleteAllInConversation(id)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch { messageDao.delete(id) }
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun stopStreaming() {
        streamJob?.cancel()
    }

    fun copyMessage(id: String): String =
        currentMessages.value.firstOrNull { it.id == id }?.content.orEmpty()

    fun exportConversation(
        id: String,
        format: ExportFormat,
        options: ExportOptions,
    ): String {
        val conv = conversations.value.firstOrNull { it.id == id } ?: return ""
        val msgs = currentMessages.value
        return when (format) {
            ExportFormat.MARKDOWN -> exporter.exportMarkdown(conv, msgs, options)
            ExportFormat.JSON -> exporter.exportJson(conv, msgs, options)
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            val cfg = config.value
            apiClient.fetchModels(cfg.apiAddress, cfg.apiKey).fold(
                onSuccess = { _availableModels.value = it },
                onFailure = { _availableModels.value = emptyList() },
            )
        }
    }

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(modelName = modelName) }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return
        val conversationId = _currentConversationId.value ?: return
        _isSending.value = true
        _streamingState.value = StreamingState()
        _chatError.value = null

        streamJob = viewModelScope.launch {
            val cfg = config.value
            val now = System.currentTimeMillis()

            val userOrder = messageDao.nextOrder(conversationId)
            messageDao.insert(
                ConversationMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    order = userOrder,
                    role = "user",
                    content = text,
                    createdAt = now,
                )
            )
            conversationDao.touch(conversationId, now)

            val conv = conversationDao.get(conversationId)
            if (conv != null && conv.title == DEFAULT_TITLE) {
                val title = text.lineSequence().firstOrNull()?.take(TITLE_MAX_LEN)?.trim().orEmpty()
                if (title.isNotBlank()) conversationDao.rename(conversationId, title, now)
            }

            val history = messageDao.observeByConversation(conversationId).first()
            val workingHistory = buildWireHistory(cfg, history).toMutableList()

            val toolDefs = toolRegistry.getAllTools()
            val toolsJson: JsonArray? = if (toolDefs.isNotEmpty()) {
                buildJsonArray { toolDefs.forEach { add(it.toOpenAiFunctionJson()) } }
            } else null

            val finalReasoning = StringBuilder()
            val finalContent = StringBuilder()
            val allToolCalls = mutableListOf<ToolCallRecord>()
            var errorMsg: String? = null
            var wasCancelled = false
            val startTime = System.currentTimeMillis()
            val fullUrl = cfg.apiAddress.trimEnd('/') + cfg.apiPath

            try {
                var round = 0
                while (round < AiChatToolHelper.MAX_TOOL_ROUNDS) {
                    val roundReasoning = StringBuilder()
                    val roundContent = StringBuilder()
                    val toolBuffers = mutableMapOf<Int, ToolCallBuffer>()

                    apiClient.streamChat(
                        baseUrl = cfg.apiAddress,
                        path = cfg.apiPath,
                        apiKey = cfg.apiKey,
                        model = cfg.modelName,
                        messagesJson = JsonArray(workingHistory),
                        toolsJson = toolsJson,
                    ).collect { event ->
                        when (event) {
                            is StreamEvent.Content -> {
                                roundContent.append(event.text)
                                finalContent.append(event.text)
                                emitStreaming(finalReasoning, finalContent, allToolCalls)
                            }
                            is StreamEvent.Reasoning -> {
                                roundReasoning.append(event.text)
                                emitStreaming(StringBuilder(finalReasoning).append(roundReasoning), finalContent, allToolCalls)
                            }
                            is StreamEvent.ToolCallDelta -> {
                                val buf = toolBuffers.getOrPut(event.index) { ToolCallBuffer() }
                                event.id?.let { buf.id = it }
                                event.name?.let { buf.name = it }
                                buf.arguments.append(event.argumentsChunk)
                            }
                        }
                    }

                    if (roundReasoning.isNotEmpty()) {
                        if (finalReasoning.isNotEmpty()) finalReasoning.append("\n")
                        finalReasoning.append(roundReasoning)
                    }

                    if (toolBuffers.isEmpty()) break

                    workingHistory += AiChatToolHelper.buildAssistantToolMessage(
                        content = roundContent.toString(),
                        toolBuffers = toolBuffers,
                    )

                    toolBuffers.toSortedMap().forEach { (_, buf) ->
                        val record = AiChatToolHelper.executeToolCall(buf, toolRegistry)
                        allToolCalls += record
                        emitStreaming(finalReasoning, finalContent, allToolCalls)
                        workingHistory += AiChatToolHelper.buildToolResultMessage(record)
                    }

                    round++
                }
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            } catch (e: Exception) {
                errorMsg = e.message ?: e::class.simpleName ?: "Unknown error"
            } finally {
                val duration = System.currentTimeMillis() - startTime
                val finalContentStr = finalContent.toString()
                val finalReasoningStr = finalReasoning.toString()
                val finalToolCalls = allToolCalls.toList()

                val assistantContent = when {
                    wasCancelled && finalContentStr.isNotEmpty() -> "$finalContentStr\n\n(已中断)"
                    wasCancelled -> ""
                    errorMsg != null && finalContentStr.isNotEmpty() -> "$finalContentStr\n\n(中途出错：$errorMsg)"
                    errorMsg != null -> "(请求失败：$errorMsg)"
                    finalContentStr.isNotEmpty() -> finalContentStr
                    finalToolCalls.isNotEmpty() -> "(已完成 ${finalToolCalls.size} 次工具调用，但模型未给出文本回复)"
                    else -> "(空响应)"
                }

                if (assistantContent.isNotEmpty()) {
                    val assistantOrder = messageDao.nextOrder(conversationId)
                    messageDao.insert(
                        ConversationMessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            order = assistantOrder,
                            role = "assistant",
                            content = assistantContent,
                            reasoning = finalReasoningStr,
                            toolCallsJson = AiChatToolHelper.serializeToolCalls(finalToolCalls),
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                    conversationDao.touch(conversationId, System.currentTimeMillis())
                }

                _streamingState.value = StreamingState()
                _isSending.value = false
                if (errorMsg != null) _chatError.value = errorMsg

                repository.addLog(
                    AiCallLogEntity(
                        timestamp = startTime,
                        type = "Assistant Chat",
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
            }
        }
    }

    fun regenerateLastAssistant() {
        if (_isSending.value) return
        viewModelScope.launch {
            val id = _currentConversationId.value ?: return@launch
            val msgs = messageDao.observeByConversation(id).first()
            val lastAssistant = msgs.lastOrNull { it.role == "assistant" } ?: return@launch
            val precedingUser = msgs.lastOrNull { it.role == "user" && it.order < lastAssistant.order } ?: return@launch

            messageDao.delete(lastAssistant.id)
            sendMessage(precedingUser.content)
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }

    companion object {
        private const val DEFAULT_TITLE = "新对话"
        private const val TITLE_MAX_LEN = 24
    }

    private fun emitStreaming(
        reasoning: CharSequence,
        content: CharSequence,
        toolCalls: List<ToolCallRecord>,
    ) {
        _streamingState.value = StreamingState(
            reasoning = reasoning.toString(),
            content = content.toString(),
            toolCalls = toolCalls.toList(),
        )
    }

    private fun buildWireHistory(
        cfg: AiInterConfig,
        messages: List<ConversationMessageEntity>,
    ): List<JsonObject> {
        val nowIso = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val systemContent = buildString {
            append("当前时间: ")
            append(nowIso)
            append("\n\n")
            append(AiChatToolHelper.TOOLS_SYSTEM_PROMPT)
            if (cfg.promptChat.isNotBlank()) {
                append("\n\n# 附加指引\n")
                append(cfg.promptChat)
            }
        }
        val list = mutableListOf<JsonObject>()
        list += buildJsonObject {
            put("role", "system")
            put("content", systemContent)
        }
        messages.forEach { msg ->
            list += buildJsonObject {
                put("role", msg.role)
                put("content", msg.content)
            }
        }
        return list
    }

}
