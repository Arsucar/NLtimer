# AI Inter 网络通信与调试完善设计

## 概述

为 AI Inter 模块添加真实网络通信能力，替换现有的模拟实现。包括：获取模型列表、流式 SSE 对话、调用日志详情页、网络权限声明。

## 架构：分层 API Client

```
UI (Compose) → ViewModel → AiInterApiClient → OkHttp → Remote API
                              ↓
                    AiInterRepository (Room + DataStore)
```

新增 `AiInterApiClient` 类作为独立网络层，不与 Repository 混合。ViewModel 协调 Client 和 Repository。

## 1. 依赖与权限

### libs.versions.toml 新增
```toml
[libraries]
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version = "4.12.0" }
okhttp-sse = { group = "com.squareup.okhttp3", name = "okhttp-sse", version = "4.12.0" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.8.1" }
```

### app/build.gradle.kts 新增
```kotlin
implementation(libs.okhttp)
implementation(libs.okhttp.sse)
implementation(libs.kotlinx.serialization.json)
```

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 2. 网络层 — AiInterApiClient

新建文件：`experimental/ai_inter/network/AiInterApiClient.kt`

```kotlin
@Singleton
class AiInterApiClient @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // 获取模型列表：GET ${baseUrl}/models
    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>>

    // 流式对话：POST ${baseUrl}${path}，SSE 逐 token
    fun streamChat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Flow<String>

    // 非流式对话（备用）
    suspend fun chat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Result<String>
}
```

### fetchModels 实现
- `GET ${baseUrl}/models`，Header `Authorization: Bearer ${apiKey}`
- 解析 OpenAI 格式响应：`{ "data": [{ "id": "model-name" }, ...] }`
- 返回 model ID 列表，按字母排序

### streamChat 实现
- `POST ${baseUrl}${path}`，Header `Authorization: Bearer ${apiKey}`
- 请求体：
  ```json
  {
    "model": "...",
    "messages": [{"role": "user", "content": "..."}],
    "stream": true
  }
  ```
- 使用 OkHttp `EventSource` 解析 SSE
- 每个 `data: {...}` 行提取 `choices[0].delta.content`
- 遇到 `data: [DONE]` 结束
- 返回 `Flow<String>` 逐个 token 发射

### chat 实现
- 同 streamChat 但 `"stream": false`
- 直接返回 `choices[0].message.content`

## 3. 数据模型扩展

### AiCallLogEntity 扩展字段
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
    // 新增字段
    val requestUrl: String = "",
    val requestTokens: Int = 0,
    val responseTokens: Int = 0
)
```

数据库版本从 1 升级到 2，使用 `fallbackToDestructiveMigration()`。

### 网络响应数据类
```kotlin
@Serializable
data class OpenAiModelsResponse(val data: List<OpenAiModel>)

@Serializable
data class OpenAiModel(val id: String)

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean = false
)

@Serializable
data class OpenAiMessage(val role: String, val content: String)

@Serializable
data class OpenAiChatResponse(val choices: List<OpenAiChoice>)

@Serializable
data class OpenAiChoice(val message: OpenAiMessage? = null, val delta: OpenAiMessage? = null)

@Serializable
data class OpenAiStreamChunk(val choices: List<OpenAiChoice>)
```

## 4. ViewModel 改造

### AiInterViewModel 新增状态
```kotlin
val availableModels: StateFlow<List<String>>  // 获取到的模型列表
val isLoadingModels: StateFlow<Boolean>       // 正在获取模型
val modelsError: StateFlow<String?>           // 获取模型错误信息
```

### sendMessage 改造（禁用模拟）
```kotlin
fun sendMessage(text: String) {
    // 1. 追加用户消息
    // 2. 调用 apiClient.streamChat() 获取 Flow<String>
    // 3. 收集 flow，实时更新 assistant bubble（逐字追加）
    // 4. 完成后计算耗时，写入 Room 日志（包含真实 token 数、完整响应、状态）
    // 5. 错误时写入 Failed 状态 + errorMessage
}
```

### fetchModels 新增
```kotlin
fun fetchModels() {
    viewModelScope.launch {
        _isLoadingModels.value = true
        val result = apiClient.fetchModels(config.value.apiAddress, config.value.apiKey)
        // 更新 availableModels 或 modelsError
        _isLoadingModels.value = false
    }
}
```

## 5. UI 改造

### 5.1 Provider Config — 获取模型列表
- "获取模型列表" 按钮触发 `viewModel.fetchModels()`
- 加载中显示 CircularProgressIndicator
- 成功后弹出 BottomSheet 显示模型列表
- 点击模型自动填入 modelName 字段并保存
- 失败显示 Snackbar 错误信息

### 5.2 测试对话 — 流式响应
- `sendMessage` 返回的 Flow 实时更新 assistant bubble
- 用户可看到逐字生成效果
- 发送期间输入框禁用
- 支持停止生成按钮（取消 coroutine）

### 5.3 调用日志详情页
- 点击日志条目导航到新路由 `AI_CALL_LOG_DETAIL`
- 新增路由：`NLtimerRoutes.AI_CALL_LOG_DETAIL = "ai_call_log_detail"`
- 详情页展示全部字段：
  - 时间戳（格式化显示）
  - 请求 URL
  - 类型、状态（色标）
  - 耗时（ms）
  - 模型名称
  - 工具列表（Chip 标签）
  - 请求 token 数 / 响应 token 数
  - 提示词全文（可复制）
  - 完整响应体（可复制）
  - 错误信息（如有）
- 使用 ScrollView + 分组卡片布局

## 6. 文件变更清单

| 操作 | 文件 |
|------|------|
| 新建 | `experimental/ai_inter/network/AiInterApiClient.kt` |
| 新建 | `experimental/ai_inter/network/Models.kt`（网络数据类） |
| 新建 | `experimental/ai_inter/AiInterLogDetailRoute.kt`（日志详情页） |
| 修改 | `libs.versions.toml`（添加 okhttp 依赖） |
| 修改 | `app/build.gradle.kts`（添加 okhttp、serialization 依赖） |
| 修改 | `AndroidManifest.xml`（添加 INTERNET 权限） |
| 修改 | `data/Models.kt`（扩展 AiCallLogEntity 字段） |
| 修改 | `data/AiInterDatabase.kt`（版本升级） |
| 修改 | `data/AiInterRepository.kt`（无变化，确认兼容） |
| 修改 | `di/AiInterModule.kt`（提供 AiInterApiClient） |
| 修改 | `viewmodel/AiInterViewModel.kt`（真实网络调用替换模拟） |
| 修改 | `AiInterSubScreens.kt`（模型选择 UI、流式对话 UI） |
| 修改 | `NLtimerRoutes.kt`（新增 AI_CALL_LOG_DETAIL 路由） |
| 修改 | `NLtimerNavHost.kt`（注册新路由） |
