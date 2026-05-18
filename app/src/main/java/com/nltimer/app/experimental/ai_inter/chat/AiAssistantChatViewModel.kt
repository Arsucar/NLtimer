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
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolRegistry
import com.nltimer.core.tools.ToolResult
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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
        viewModelScope.launch {
            val list = conversationDao.observeAll().first()
            if (list.isNotEmpty()) {
                _currentConversationId.value = list.first().id
            } else {
                newConversation()
            }
        }
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
                while (round < MAX_TOOL_ROUNDS) {
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

                    workingHistory += buildJsonObject {
                        put("role", "assistant")
                        put("content", roundContent.toString())
                        putJsonArray("tool_calls") {
                            toolBuffers.toSortedMap().forEach { (_, buf) ->
                                add(buildJsonObject {
                                    put("id", buf.id ?: "")
                                    put("type", "function")
                                    putJsonObject("function") {
                                        put("name", buf.name ?: "")
                                        put("arguments", buf.arguments.toString())
                                    }
                                })
                            }
                        }
                    }

                    toolBuffers.toSortedMap().forEach { (_, buf) ->
                        val record = executeToolCall(buf)
                        allToolCalls += record
                        emitStreaming(finalReasoning, finalContent, allToolCalls)
                        workingHistory += buildJsonObject {
                            put("role", "tool")
                            put("tool_call_id", record.id)
                            put("name", record.name)
                            put("content", record.result)
                        }
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
                            toolCallsJson = serializeToolCalls(finalToolCalls),
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
                        toolCallsJson = serializeToolCalls(finalToolCalls),
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
        private const val MAX_TOOL_ROUNDS = 5
        private const val DEFAULT_TITLE = "新对话"
        private const val TITLE_MAX_LEN = 24

        private const val TOOLS_SYSTEM_PROMPT = """# 工具调用规则（强制）

## 1. 时间解析
- "10 点到 14 点" → 当天本地时区 10:00-14:00，ISO 8601 含时区偏移
- "15 点午休 30 分钟" → 起 15:00 止 15:30
- "现在结束计时" / "结束" → 直接调 endBehavior()，不要传时间参数

## 2. 默认值兜底（不要主动追问）
- 用户没指定活动分类时，createActivity 默认 groupName="预制菜"
- 用户没指定标签分类时，createTag 默认 category="预制菜"
- 颜色缺失时工具内自动生成莫奈中和色，无需问用户
- 标签图标缺失时默认 "#"

## 3. 多步序列分支
当用户说"先做 X，再做 Y"或一连串任务时：
1. 先 queryCurrentBehavior 看是否有 ACTIVE
2. 有 ACTIVE → 所有任务都走 createGoal（按顺序排队）
3. 无 ACTIVE → 第 1 个用 startBehavior 立即开始，剩下走 createGoal

## 4. 冲突处理
当 recordBehavior 返回 ValidationError 且 message 是 JSON 含 "code":"CONFLICT" 时：
1. 解析 message 里的 conflicts 数组（每项含 id / activityName / startTime / endTime）
2. 向用户复述冲突区间，给出三个选项：
   - 覆盖：对每个 conflict.id 调 deleteBehavior(id)，然后重发 recordBehavior
   - 取消：不做任何动作，告诉用户已取消
   - 调整时间：让用户给新时间，再发 recordBehavior

## 5. "查看所有标签"
- 默认 listTags() 返回未归档标签
- 用户明确说"包括归档"时才传 includeArchived=true
"""
    }

    private class ToolCallBuffer {
        var id: String? = null
        var name: String? = null
        val arguments: StringBuilder = StringBuilder()
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
            append(TOOLS_SYSTEM_PROMPT)
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

    private suspend fun executeToolCall(buf: ToolCallBuffer): ToolCallRecord {
        val name = buf.name.orEmpty()
        val argsStr = buf.arguments.toString()
        val id = buf.id ?: "call_${System.nanoTime()}"
        val started = System.currentTimeMillis()
        val argsMap = parseToolArguments(argsStr)
        val result = if (name.isBlank()) {
            ToolResult.Error(
                name = name,
                error = ToolError.ValidationError("模型未提供工具名"),
            )
        } else {
            toolRegistry.executeTool(name, argsMap)
        }
        val durationMs = System.currentTimeMillis() - started
        val (success, resultStr) = when (result) {
            is ToolResult.Success -> true to (result.data?.toString() ?: "null")
            is ToolResult.Error -> false to "[${result.error::class.simpleName}] ${result.error.message}"
        }
        return ToolCallRecord(
            id = id,
            name = name,
            arguments = argsStr,
            result = resultStr,
            success = success,
            durationMs = durationMs,
        )
    }

    private fun parseToolArguments(argsStr: String): Map<String, Any?> {
        if (argsStr.isBlank()) return emptyMap()
        return try {
            val parser = Json { ignoreUnknownKeys = true }
            val element = parser.parseToJsonElement(argsStr)
            if (element !is JsonObject) return emptyMap()
            element.mapValues { (_, v) ->
                when (v) {
                    is JsonPrimitive -> when {
                        v.isString -> v.content
                        v.content == "true" -> true
                        v.content == "false" -> false
                        else -> v.content.toLongOrNull() ?: v.content.toDoubleOrNull() ?: v.content
                    }
                    else -> v.toString()
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeToolCalls(records: List<ToolCallRecord>): String {
        if (records.isEmpty()) return ""
        val writer = Json { ignoreUnknownKeys = true }
        val array = buildJsonArray {
            records.forEach { rec ->
                add(buildJsonObject {
                    put("id", rec.id)
                    put("name", rec.name)
                    put("arguments", rec.arguments)
                    put("result", rec.result)
                    put("success", rec.success)
                    put("durationMs", rec.durationMs)
                })
            }
        }
        return writer.encodeToString(JsonArray.serializer(), array)
    }
}
