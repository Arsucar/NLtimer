# AI Inter 网络通信与调试完善 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 AI Inter 模块添加真实网络通信（OkHttp + SSE 流式对话）、获取模型列表、完善调用日志详情页，并声明网络权限。

**架构：** 新增 `AiInterApiClient` 作为独立网络层，ViewModel 协调 Client（网络）与 Repository（持久化），UI 层支持流式逐字展示和模型列表选择。

**技术栈：** OkHttp 4.12.0, okhttp-sse, kotlinx-serialization-json, Room, DataStore, Hilt

**前置条件：** 已在 worktree `.worktrees/ai-inter` 的 `feature/ai-inter` 分支上

---

### 任务 1：依赖与权限

**文件：**
- 修改：`gradle/libs.versions.toml`
- 修改：`app/build.gradle.kts`
- 修改：`app/src/main/AndroidManifest.xml`

- [ ] **步骤 1：libs.versions.toml 添加 OkHttp 和 kotlinx-serialization**

在 `[libraries]` 区块追加：
```toml
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version = "4.12.0" }
okhttp-sse = { group = "com.squareup.okhttp3", name = "okhttp-sse", version = "4.12.0" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.8.1" }
```

- [ ] **步骤 2：app/build.gradle.kts 添加依赖**

```kotlin
implementation(libs.okhttp)
implementation(libs.okhttp.sse)
implementation(libs.kotlinx.serialization.json)
```

- [ ] **步骤 3：AndroidManifest.xml 添加 INTERNET 权限**

```xml
<uses-permission android:name="android.permission.INTERNET" />
```
放在 `<application>` 标签之前。

- [ ] **步骤 4：验证编译**

```bash
./gradlew app:compileDebugKotlin
```

- [ ] **步骤 5：Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat(ai-inter): add OkHttp, kotlinx-serialization dependencies and INTERNET permission"
```

---

### 任务 2：网络数据模型

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/network/AiNetModels.kt`

- [ ] **步骤 1：创建 AiNetModels.kt**

```kotlin
package com.nltimer.app.experimental.ai_inter.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiModelsResponse(
    val data: List<OpenAiModel>
)

@Serializable
data class OpenAiModel(
    val id: String
)

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiChatMessage? = null,
    val delta: OpenAiChatMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiStreamChunk(
    val choices: List<OpenAiChoice>
)

@Serializable
data class OpenAiUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0
)

@Serializable
data class OpenAiErrorResponse(
    val error: OpenAiErrorDetail? = null
)

@Serializable
data class OpenAiErrorDetail(
    val message: String? = null,
    val type: String? = null
)
```

- [ ] **步骤 2：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/network/AiNetModels.kt
git commit -m "feat(ai-inter): add OpenAI API response data models"
```

---

### 任务 3：扩展 Room Entity + 数据库迁移

**文件：**
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/data/Models.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/data/AiInterDatabase.kt`

- [ ] **步骤 1：扩展 AiCallLogEntity 添加新字段**

```kotlin
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
    val responseTokens: Int = 0
)
```

- [ ] **步骤 2：更新 AiInterDatabase 版本号**

```kotlin
@Database(entities = [AiCallLogEntity::class], version = 2, exportSchema = false)
abstract class AiInterDatabase : RoomDatabase() {
    abstract fun aiCallLogDao(): AiCallLogDao
}
```

添加 `fallbackToDestructiveMigration()` 保证旧数据可清除（实验性功能，数据不重要）。

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AiInterModule {
    @Provides
    @Singleton
    fun provideAiInterDatabase(@ApplicationContext context: Context): AiInterDatabase {
        return Room.databaseBuilder(
            context,
            AiInterDatabase::class.java,
            "ai_inter_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAiCallLogDao(database: AiInterDatabase): AiCallLogDao {
        return database.aiCallLogDao()
    }
}
```

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/data/Models.kt
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/data/AiInterDatabase.kt
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/di/AiInterModule.kt
git commit -m "feat(ai-inter): extend AiCallLogEntity with token fields and DB migration"
```

---

### 任务 4：AiInterApiClient 网络层

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/network/AiInterApiClient.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/di/AiInterModule.kt`

- [ ] **步骤 1：创建 AiInterApiClient**

```kotlin
package com.nltimer.app.experimental.ai_inter.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiInterApiClient @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // GET ${baseUrl}/models
    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> {
        return try {
            val url = "${baseUrl.trimEnd('/')}/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val modelsResponse = json.decodeFromString<OpenAiModelsResponse>(body)
                val modelNames = modelsResponse.data.map { it.id }.sorted()
                Result.success(modelNames)
            } else {
                val errorMsg = try {
                    json.decodeFromString<OpenAiErrorResponse>(body).error?.message ?: response.message
                } catch (_: Exception) { response.message }
                Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 非流式 POST（备用）
    suspend fun chat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Result<ChatResult> {
        return try {
            val url = baseUrl.trimEnd('/') + path
            val requestBody = OpenAiChatRequest(
                model = model,
                messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
                stream = false
            )
            val jsonBody = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val chatResponse = json.decodeFromString<OpenAiChatResponse>(body)
                val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
                Result.success(ChatResult(
                    content = content,
                    promptTokens = chatResponse.usage?.promptTokens ?: 0,
                    responseTokens = chatResponse.usage?.completionTokens ?: 0
                ))
            } else {
                val errorMsg = try {
                    json.decodeFromString<OpenAiErrorResponse>(body).error?.message ?: response.message
                } catch (_: Exception) { response.message }
                Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 流式 SSE POST，返回 Flow<String> 逐个 token
    fun streamChat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Flow<String> = callbackFlow {
        val url = baseUrl.trimEnd('/') + path
        val requestBody = OpenAiChatRequest(
            model = model,
            messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
            stream = true
        )
        val jsonBody = json.encodeToString(requestBody)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val factory = EventSources.createFactory(client)
        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val chunk = json.decodeFromString<OpenAiStreamChunk>(data)
                    val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                    if (content.isNotEmpty()) {
                        trySend(content)
                    }
                } catch (_: Exception) {
                    // skip malformed chunks
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE connection failed"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }

    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ChatResult(
        val content: String,
        val promptTokens: Int,
        val responseTokens: Int
    )
}
```

- [ ] **步骤 2：更新 AiInterModule 提供 AiInterApiClient**

在 `AiInterModule` 中没有变化 —— `@Singleton @Inject constructor()` 即可被 Hilt 自动构造。

- [ ] **步骤 3：验证编译**

```bash
./gradlew app:compileDebugKotlin
```

- [ ] **步骤 4：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/network/AiInterApiClient.kt
git commit -m "feat(ai-inter): add AiInterApiClient with SSE streaming and model fetching"
```

---

### 任务 5：改造 AiInterViewModel — 真实网络调用替换模拟

**文件：**
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/viewmodel/AiInterViewModel.kt`

- [ ] **步骤 1：替换 AiInterViewModel 完整实现**

```kotlin
package com.nltimer.app.experimental.ai_inter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

    val config: StateFlow<AiInterConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiInterConfig())

    val logs: StateFlow<List<AiCallLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    // Model list fetching
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _modelsError = MutableStateFlow<String?>(null)
    val modelsError: StateFlow<String?> = _modelsError.asStateFlow()

    // Selected log for detail view
    private val _selectedLog = MutableStateFlow<AiCallLogEntity?>(null)
    val selectedLog: StateFlow<AiCallLogEntity?> = _selectedLog.asStateFlow()

    private var currentStreamJob: Job? = null

    fun updateConfig(
        apiAddress: String = config.value.apiAddress,
        apiPath: String = config.value.apiPath,
        apiKey: String = config.value.apiKey,
        modelName: String = config.value.modelName
    ) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(
                apiAddress = apiAddress,
                apiPath = apiPath,
                apiKey = apiKey,
                modelName = modelName
            )}
        }
    }

    fun updatePrompts(
        promptNotes: String = config.value.promptNotes,
        promptTaskGen: String = config.value.promptTaskGen,
        promptChat: String = config.value.promptChat
    ) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(
                promptNotes = promptNotes,
                promptTaskGen = promptTaskGen,
                promptChat = promptChat
            )}
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
            val result = apiClient.fetchModels(cfg.apiAddress, cfg.apiKey)
            result.fold(
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
        if (_isSending.value) return
        _isSending.value = true
        _streamingContent.value = ""

        val userMessage = ChatMessage(role = "user", content = text)
        _chatMessages.value = _chatMessages.value + userMessage

        val cfg = config.value
        val allMessages = _chatMessages.value.map { AiInterApiClient.ChatMessage(role = it.role, content = it.content) }
        val startTime = System.currentTimeMillis()
        val fullUrl = cfg.apiAddress.trimEnd('/') + cfg.apiPath

        currentStreamJob = viewModelScope.launch {
            val assistantMessage = ChatMessage(role = "assistant", content = "", timestamp = startTime)
            _chatMessages.value = _chatMessages.value + assistantMessage

            val contentBuilder = StringBuilder()
            var errorMsg: String? = null
            var usedTokens = 0
            var generatedTokens = 0

            try {
                apiClient.streamChat(
                    baseUrl = cfg.apiAddress,
                    path = cfg.apiPath,
                    apiKey = cfg.apiKey,
                    model = cfg.modelName,
                    messages = allMessages
                ).collect { token ->
                    contentBuilder.append(token)
                    usedTokens++
                    val msgs = _chatMessages.value.toMutableList()
                    if (msgs.isNotEmpty()) {
                        msgs[msgs.lastIndex] = msgs.last().copy(content = contentBuilder.toString())
                        _chatMessages.value = msgs
                    }
                    _streamingContent.value = contentBuilder.toString()
                }
                generatedTokens = usedTokens
            } catch (e: Exception) {
                errorMsg = e.message ?: "Unknown error"
                // Still show what we got
                val msgs = _chatMessages.value.toMutableList()
                if (msgs.isNotEmpty()) {
                    msgs[msgs.lastIndex] = msgs.last().copy(content = contentBuilder.toString())
                    _chatMessages.value = msgs
                }
            } finally {
                val duration = System.currentTimeMillis() - startTime
                // Write log to Room
                val logEntry = AiCallLogEntity(
                    type = "Test Chat",
                    status = if (errorMsg == null) "Success" else "Failed",
                    durationMs = duration,
                    model = cfg.modelName,
                    tools = "",
                    prompt = text,
                    response = contentBuilder.toString(),
                    errorMessage = errorMsg,
                    requestUrl = fullUrl,
                    requestTokens = 0,
                    responseTokens = generatedTokens
                )
                try {
                    repository.addLog(logEntry)
                } catch (_: Exception) { }

                _isSending.value = false
                _streamingContent.value = ""
            }
        }
    }

    fun stopStreaming() {
        currentStreamJob?.cancel()
        if (_chatMessages.value.lastOrNull()?.content?.isEmpty() == true) {
            _chatMessages.value = _chatMessages.value.dropLast(1)
        }
        _isSending.value = false
        _streamingContent.value = ""
    }
}
```

- [ ] **步骤 2：验证编译**

```bash
./gradlew app:compileDebugKotlin
```

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/viewmodel/AiInterViewModel.kt
git commit -m "feat(ai-inter): replace mock sendMessage with real SSE streaming API calls"
```

---

### 任务 6：Provider Config — 获取模型列表 UI

**文件：**
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterSubScreens.kt`

- [ ] **步骤 1：改造 `AiProviderConfigRoute` 添加模型列表获取和选择**

在 `AiInterSubScreens.kt` 中找到 `AiProviderConfigRoute`，修改为：

```kotlin
@Composable
fun AiProviderConfigRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val modelsError by viewModel.modelsError.collectAsStateWithLifecycle()

    var showModelSheet by remember { mutableStateOf(false) }

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(modelsError) {
        modelsError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearModelsError()
        }
    }

    SettingsSubpageScaffold(
        title = "提供商配置",
        onBackClick = { LocalOnBackPressedDispatcher.current.onBackPressed() }
    ) { innerPadding ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = config.apiAddress,
                    onValueChange = { viewModel.updateConfig(apiAddress = it) },
                    label = { Text("API 地址") },
                    placeholder = { Text("https://integrate.api.nvidia.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.apiPath,
                    onValueChange = { viewModel.updateConfig(apiPath = it) },
                    label = { Text("API 路径") },
                    placeholder = { Text("/chat/completions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.apiKey,
                    onValueChange = { viewModel.updateConfig(apiKey = it) },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // Model row with fetch button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = config.modelName,
                        onValueChange = { viewModel.updateConfig(modelName = it) },
                        label = { Text("当前模型") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    FilledTonalButton(
                        onClick = {
                            viewModel.fetchModels()
                            showModelSheet = true
                        },
                        enabled = !isLoadingModels && config.apiKey.isNotBlank()
                    ) {
                        if (isLoadingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("获取模型列表")
                    }
                }

                // Show available models in a bottom sheet
                if (showModelSheet && availableModels.isNotEmpty()) {
                    ModalBottomSheet(
                        onDismissRequest = { showModelSheet = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "可用模型 (${availableModels.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                items(availableModels) { model ->
                                    ListItem(
                                        headlineContent = { Text(model) },
                                        modifier = Modifier.clickable {
                                            viewModel.updateConfig(modelName = model)
                                            showModelSheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

需要确保导入 `ModalBottomSheet` 和相关 Material 3 组件。

- [ ] **步骤 2：验证编译**

```bash
./gradlew app:compileDebugKotlin
```

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterSubScreens.kt
git commit -m "feat(ai-inter): add model list fetching and selection in Provider Config"
```

---

### 任务 7：测试对话 — 流式显示 + 停止按钮

**文件：**
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterSubScreens.kt`

- [ ] **步骤 1：改造 `AiTestChatRoute` 支持流式显示和停止按钮**

找到 `AiTestChatRoute`，将其替换为：

```kotlin
@Composable
fun AiTestChatRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    SettingsSubpageScaffold(
        title = "测试对话",
        onBackClick = { LocalOnBackPressedDispatcher.current.onBackPressed() },
        actions = {
            if (chatMessages.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.Delete, contentDescription = "清空对话")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(message = msg)
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    enabled = !isSending,
                    maxLines = 4
                )
                if (isSending) {
                    IconButton(onClick = { viewModel.stopStreaming() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "停止生成",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}
```

`LocalOnBackPressedDispatcher` 的导入需要确认。项目中可能使用 `OnBackPressedDispatcher` 或 Compose 内置的后退处理。需要检查项目中是否已定义 `LocalOnBackPressedDispatcher`。如果在实验模块中没有定义，使用：

```kotlin
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
// ...
val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
```

需要打开 `AiInterSubScreens.kt` 查看当前后退按钮的实现方式并保持一致。

- [ ] **步骤 2：验证编译**

```bash
./gradlew app:compileDebugKotlin
```

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterSubScreens.kt
git commit -m "feat(ai-inter): add streaming display and stop button in test chat"
```

---

### 任务 8：调用日志详情页

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterLogDetailRoute.kt`
- 修改：`app/src/main/java/com/nltimer/app/navigation/NLtimerRoutes.kt`
- 修改：`app/src/main/java/com/nltimer/app/navigation/NLtimerNavHost.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterSubScreens.kt`（日志条目点击事件）

- [ ] **步骤 1：创建 AiInterLogDetailRoute.kt**

```kotlin
package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.viewmodel.AiInterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLogDetailRoute(
    viewModel: AiInterViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()

    if (selectedLog == null) {
        onBackClick()
        return
    }

    val log = selectedLog!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调用日志详情") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedLog()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailField("时间戳", SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(Date(log.timestamp)))
            DetailField("请求 URL", log.requestUrl)
            DetailField("类型", log.type)
            StatusField("状态", log.status)
            DetailField("耗时", "${log.durationMs} ms")
            DetailField("模型", log.model)
            if (log.tools.isNotBlank()) {
                DetailField("工具", log.tools)
            }
            DetailField("请求 Token", if (log.requestTokens > 0) log.requestTokens.toString() else "-")
            DetailField("响应 Token", if (log.responseTokens > 0) log.responseTokens.toString() else "-")
            HorizontalDivider()
            SectionHeader("提示词")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = log.prompt.ifEmpty { "(空)" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SectionHeader("响应体")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = log.response.ifEmpty {
                        if (log.status == "Failed") "(请求失败)" else "(空)"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            log.errorMessage?.let { error ->
                SectionHeader("错误信息")
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusField(label: String, status: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val color = if (status == "Success") MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
```

- [ ] **步骤 2：NLtimerRoutes.kt 添加新路由**

```kotlin
const val AI_CALL_LOG_DETAIL = "ai_call_log_detail"
```

添加后确认 `SETTINGS_FULLSCREEN_ROUTES` 已包含它（即需要全屏顶部栏）。

- [ ] **步骤 3：NLtimerNavHost.kt 注册新路由**

```kotlin
composable(NLtimerRoutes.AI_CALL_LOG_DETAIL) {
    AiLogDetailRoute(
        onBackClick = { navController.popBackStack() }
    )
}
```

- [ ] **步骤 4：AiInterSubScreens.kt 日志条目添加点击事件**

在 `AiCallLogsRoute` 中，给每个日志条目的 `GroupCard` 添加 `Modifier.clickable`：

```kotlin
GroupCard(
    modifier = Modifier.clickable {
        viewModel.selectLog(log)
        onNavigateToLogDetail()
    },
    // ... existing content
)
```

修改 composable 签名，新增 `onNavigateToLogDetail` 回调。

- [ ] **步骤 5：验证编译**

```bash
./gradlew app:compileDebugKotlin
```

- [ ] **步骤 6：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterLogDetailRoute.kt
git add app/src/main/java/com/nltimer/app/navigation/NLtimerRoutes.kt
git add app/src/main/java/com/nltimer/app/navigation/NLtimerNavHost.kt
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterSubScreens.kt
git commit -m "feat(ai-inter): add call log detail page with full field display"
```

---

### 任务 9：端到端验证

- [ ] **步骤 1：编译 Release 构建**

```bash
./gradlew app:assembleDebug
```

- [ ] **步骤 2：验证 APK 包含 INTERNET 权限**

```bash
./gradlew app:lintDebug
```

- [ ] **步骤 3：安装并运行**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **步骤 4：最终 Commit（如有修复）**

```bash
git add -A && git commit -m "fix: end-to-end build fixes"
```
