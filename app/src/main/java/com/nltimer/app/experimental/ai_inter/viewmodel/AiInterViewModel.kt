package com.nltimer.app.experimental.ai_inter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import com.nltimer.core.tools.ToolDefinition
import com.nltimer.core.tools.ToolRegistry
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
import javax.inject.Inject

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AiInterViewModel @Inject constructor(
    private val repository: AiInterRepository,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry
) : ViewModel() {

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

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

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
        _streamingContent.value = ""
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
        _streamingContent.value = ""
        _chatError.value = null

        val userMessage = ChatMessage(role = "user", content = text)
        _chatMessages.value = _chatMessages.value + userMessage

        val cfg = config.value
        val systemPrompt = cfg.promptChat.takeIf { it.isNotBlank() }
        val historyMessages = _chatMessages.value.map {
            AiInterApiClient.ChatMessage(role = it.role, content = it.content)
        }
        val payload = if (systemPrompt != null) {
            listOf(AiInterApiClient.ChatMessage(role = "system", content = systemPrompt)) + historyMessages
        } else {
            historyMessages
        }

        val startTime = System.currentTimeMillis()
        val fullUrl = cfg.apiAddress.trimEnd('/') + cfg.apiPath

        currentStreamJob = viewModelScope.launch {
            val contentBuilder = StringBuilder()
            var errorMsg: String? = null
            var wasCancelled = false

            try {
                apiClient.streamChat(
                    baseUrl = cfg.apiAddress,
                    path = cfg.apiPath,
                    apiKey = cfg.apiKey,
                    model = cfg.modelName,
                    messages = payload
                ).collect { token ->
                    contentBuilder.append(token)
                    _streamingContent.value = contentBuilder.toString()
                }
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            } catch (e: Exception) {
                errorMsg = e.message ?: (e::class.simpleName ?: "Unknown error")
            } finally {
                val finalContent = contentBuilder.toString()
                val duration = System.currentTimeMillis() - startTime

                if (wasCancelled) {
                    if (finalContent.isNotEmpty()) {
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            role = "assistant",
                            content = "$finalContent\n\n(已中断)",
                            timestamp = System.currentTimeMillis()
                        )
                    }
                    _streamingContent.value = ""
                    _isSending.value = false
                } else {
                    val assistantContent = when {
                        finalContent.isNotEmpty() && errorMsg == null -> finalContent
                        finalContent.isNotEmpty() && errorMsg != null -> "$finalContent\n\n(中途出错：$errorMsg)"
                        errorMsg != null -> "(请求失败：$errorMsg)"
                        else -> "(空响应 — 服务端正常关闭流但未发送任何 token。请确认：模型名「${cfg.modelName}」是否存在、API 路径「${cfg.apiPath}」是否正确、API Key 是否有效)"
                    }
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        role = "assistant",
                        content = assistantContent,
                        timestamp = System.currentTimeMillis()
                    )
                    _streamingContent.value = ""

                    if (errorMsg != null) {
                        _chatError.value = errorMsg
                    }

                    repository.addLog(
                        AiCallLogEntity(
                            timestamp = System.currentTimeMillis(),
                            type = "Test Chat",
                            status = if (errorMsg == null && finalContent.isNotEmpty()) "Success" else "Failed",
                            durationMs = duration,
                            model = cfg.modelName,
                            tools = "",
                            prompt = text,
                            response = finalContent,
                            errorMessage = errorMsg,
                            requestUrl = fullUrl
                        )
                    )

                    _isSending.value = false
                }
            }
        }
    }

    fun stopStreaming() {
        currentStreamJob?.cancel()
        val partial = _streamingContent.value
        if (partial.isNotEmpty()) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                role = "assistant",
                content = "$partial\n\n(已中断)"
            )
        }
        _streamingContent.value = ""
        _isSending.value = false
    }

    override fun onCleared() {
        super.onCleared()
        currentStreamJob?.cancel()
    }
}
