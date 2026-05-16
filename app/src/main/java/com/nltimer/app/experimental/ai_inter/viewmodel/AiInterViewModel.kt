package com.nltimer.app.experimental.ai_inter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val apiClient: AiInterApiClient
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

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _modelsError = MutableStateFlow<String?>(null)
    val modelsError: StateFlow<String?> = _modelsError.asStateFlow()

    private val _selectedLog = MutableStateFlow<AiCallLogEntity?>(null)
    val selectedLog: StateFlow<AiCallLogEntity?> = _selectedLog.asStateFlow()

    private var currentStreamJob: Job? = null

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
        _isSending.value = false
    }

    fun selectLog(log: AiCallLogEntity) {
        _selectedLog.value = log
    }

    fun clearSelectedLog() {
        _selectedLog.value = null
    }

    fun fetchModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _modelsError.value = null
            val cfg = config.value
            apiClient.fetchModels(cfg.apiAddress, cfg.apiKey).fold(
                onSuccess = { models ->
                    _availableModels.value = models
                    _modelsError.value = null
                },
                onFailure = { e ->
                    _modelsError.value = e.message
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

        val userMessage = ChatMessage(role = "user", content = text)
        _chatMessages.value = _chatMessages.value + userMessage

        val cfg = config.value
        val allMessages = _chatMessages.value.map {
            AiInterApiClient.ChatMessage(role = it.role, content = it.content)
        }
        val startTime = System.currentTimeMillis()
        val fullUrl = cfg.apiAddress.trimEnd('/') + cfg.apiPath

        currentStreamJob = viewModelScope.launch {
            val assistantMessage = ChatMessage(role = "assistant", content = "", timestamp = startTime)
            _chatMessages.value = _chatMessages.value + assistantMessage

            val contentBuilder = StringBuilder()
            var errorMsg: String? = null

            try {
                apiClient.streamChat(
                    baseUrl = cfg.apiAddress,
                    path = cfg.apiPath,
                    apiKey = cfg.apiKey,
                    model = cfg.modelName,
                    messages = allMessages
                ).collect { token ->
                    contentBuilder.append(token)
                    val msgs = _chatMessages.value.toMutableList()
                    if (msgs.isNotEmpty()) {
                        msgs[msgs.lastIndex] = msgs.last().copy(content = contentBuilder.toString())
                        _chatMessages.value = msgs
                    }
                }
            } catch (e: Exception) {
                if (contentBuilder.isEmpty()) {
                    errorMsg = e.message ?: "Unknown error"
                }
            } finally {
                val duration = System.currentTimeMillis() - startTime
                repository.addLog(
                    AiCallLogEntity(
                        timestamp = System.currentTimeMillis(),
                        type = "Test Chat",
                        status = if (errorMsg == null && contentBuilder.isNotEmpty()) "Success" else "Failed",
                        durationMs = duration,
                        model = cfg.modelName,
                        tools = "",
                        prompt = text,
                        response = contentBuilder.toString(),
                        errorMessage = errorMsg,
                        requestUrl = fullUrl
                    )
                )
                _isSending.value = false
            }
        }
    }

    fun stopStreaming() {
        currentStreamJob?.cancel()
        _isSending.value = false
    }

    override fun onCleared() {
        super.onCleared()
        currentStreamJob?.cancel()
    }
}
