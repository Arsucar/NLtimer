package com.nltimer.app.experimental.ai_inter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import com.nltimer.app.experimental.ai_inter.network.StreamEvent
import com.nltimer.app.experimental.ai_inter.network.toOpenAiFunctionJson
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
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val config: StateFlow<AiInterConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiInterConfig()
    )

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

    fun updatePrompts(notes: String, taskGen: String, chat: String) {
        viewModelScope.launch {
            repository.updateConfig {
                it.copy(
                    promptNotes = notes,
                    promptTaskGen = taskGen,
                    promptChat = chat
                )
            }
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
            append(TOOLS_SYSTEM_PROMPT)
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
                while (round < MAX_TOOL_ROUNDS) {
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
                    val assistantToolMsg = buildJsonObject {
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
                    wireHistory += assistantToolMsg

                    // 执行每个工具调用，把结果作为 role=tool 消息塞回历史
                    toolBuffers.toSortedMap().forEach { (_, buf) ->
                        val record = executeToolCall(buf)
                        allToolCalls += record
                        emitStreamingState(
                            reasoning = finalReasoning.toString(),
                            content = finalContent.toString(),
                            toolCalls = allToolCalls,
                        )
                        wireHistory += buildJsonObject {
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
                            toolCallsJson = serializeToolCalls(finalToolCalls),
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

    private suspend fun executeToolCall(buf: ToolCallBuffer): ToolCallRecord {
        val name = buf.name ?: ""
        val argsStr = buf.arguments.toString()
        val id = buf.id ?: "call_${System.nanoTime()}"
        val started = System.currentTimeMillis()
        val argsMap: Map<String, Any?> = parseToolArguments(argsStr)
        val result = if (name.isBlank()) {
            ToolResult.Error(
                name = name,
                error = com.nltimer.core.tools.ToolError.ValidationError("模型未提供工具名"),
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
            val element = json.parseToJsonElement(argsStr)
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
        return json.encodeToString(JsonArray.serializer(), array)
    }

    private class ToolCallBuffer {
        var id: String? = null
        var name: String? = null
        val arguments: StringBuilder = StringBuilder()
    }

    companion object {
        /** 工具调用最大循环轮数，超出后不再回传给模型 */
        private const val MAX_TOOL_ROUNDS = 5

        /**
         * 工具调用强制规则 —— 与用户自定义 promptChat 并存，工具规则不可被覆盖。
         *
         * 设计意图：
         * - 时间解析（"10 点到 14 点"→ISO）放进 prompt，让 LLM 自己处理
         * - 默认分类"预制菜"、随机颜色、默认图标都由工具内部兜底，prompt 只是知会 LLM 不要主动追问
         * - 多步序列分支（startBehavior vs createGoal）与冲突回报分支由 prompt 显式教
         */
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
}
