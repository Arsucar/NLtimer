# AI 工具调用集成：ToolEventBus + ProcessNoteTool + FAB 快捷 AI 输入

## 背景

NLtimer 已有 39 个 ToolDefinition 和完整的 AI SSE 流式 Tool Calling 循环（AiInterViewModel + AiChatToolHelper + AiInterApiClient），但 AI 能力被隔离在 `experimental/ai_inter` 页面，与用户主流程完全脱节。核心问题：

1. **入口缺失** — 用户在首页记录行为时触不到 AI
2. **反馈断裂** — AI 调用工具后 UI 不实时更新
3. **匹配断层** — UI 层的 NoteMatcher 未暴露给 AI，两边各做各的
4. **设置不可达** — 个性化配置没有工具化

## 目标

打通 AI 能力与用户主流程的桥梁，让用户在首页通过 FAB 拖拽选 "AI" 选项，弹出一个快捷输入框，输入自然语言后 AI 自动调用工具完成操作，且 UI 实时刷新。

## 非目标

- 不做主动建议引擎（后台推送）
- 不做语音输入
- 不做设置/配置工具化（P2，后续任务）
- 不做 AI 初始化向导（P1，后续任务）
- 不做 AI 统计问答（P1，后续任务）

---

## 工作包 1：Tool 执行事件广播（ToolEventBus）

### 需求

ToolRegistry.executeTool() 执行后，所有监听的 ViewModel 实时感知变化，自动刷新 UI 状态。

### 交付物

#### 1.1 新增 `ToolExecutedEvent`

**文件**: `core/tools/src/main/java/com/nltimer/core/tools/event/ToolExecutedEvent.kt`

```kotlin
package com.nltimer.core.tools.event

import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.tools.ToolResult

data class ToolExecutedEvent(
    val toolName: String,
    val category: ToolCategory,
    val result: ToolResult,
    val timestamp: Long = System.currentTimeMillis(),
)
```

#### 1.2 新增 `ToolEventBus`

**文件**: `core/tools/src/main/java/com/nltimer/core/tools/event/ToolEventBus.kt`

```kotlin
package com.nltimer.core.tools.event

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class ToolEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ToolExecutedEvent>(
        extraBufferCapacity = 64,
    )
    val events: SharedFlow<ToolExecutedEvent> = _events.asSharedFlow()

    internal suspend fun emit(event: ToolExecutedEvent) {
        _events.emit(event)
    }
}
```

#### 1.3 修改 `ToolRegistry`

**文件**: `core/tools/src/main/java/com/nltimer/core/tools/ToolRegistry.kt`

变更点：
- 构造函数新增 `toolEventBus: ToolEventBus` 参数
- `executeTool()` 方法中，在 `withTimeoutOrNull` 返回结果后、return 之前，emit 事件
- import 新增 `ToolEventBus` 和 `ToolExecutedEvent`

关键修改逻辑（在 executeTool 方法内）：

```kotlin
// 原代码 return try { ... } 改为：
return try {
    validateParameters(tool, args)
    val result = withTimeoutOrNull(timeoutMillis) { tool.execute(args) }
        ?: ToolResult.Error(
            name = toolName,
            error = ToolError.TimeoutError("Tool execution timeout after ${timeoutMillis}ms"),
        )
    toolEventBus.emit(ToolExecutedEvent(toolName, tool.category, result))
    result
} catch (e: IllegalArgumentException) {
    val error = ToolResult.Error(name = toolName, error = ToolError.ValidationError(e.message ?: "Invalid arguments"))
    toolEventBus.emit(ToolExecutedEvent(toolName, tool.category, error))
    error
} catch (e: Exception) {
    Log.e(TAG, "Tool execution failed: $toolName", e)
    val error = ToolResult.Error(name = toolName, error = ToolError.InternalError(e.message ?: "Unknown error"))
    toolEventBus.emit(ToolExecutedEvent(toolName, tool.category, error))
    error
}
```

注意：NotFound 分支（tool 不存在）也需 emit，在 `?: return` 之前。

#### 1.4 修改 `HomeViewModel`

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/viewmodel/HomeViewModel.kt`

变更点：
- 构造函数新增 `toolEventBus: ToolEventBus`
- init 块新增事件监听协程：

```kotlin
init {
    viewModelScope.launch {
        toolEventBus.events.collect { event ->
            when (event.category) {
                ToolCategory.TIMING, ToolCategory.BEHAVIOR -> {
                    // 刷新行为列表、活跃行为状态
                }
                ToolCategory.ACTIVITY -> {
                    // 刷新活动列表
                }
                ToolCategory.TAG -> {
                    // 刷新标签列表
                }
                else -> { /* CATEGORY, DATA, SEARCH 不影响首页 */ }
            }
        }
    }
}
```

具体刷新操作：调用 ViewModel 中已有的数据加载方法（如 refreshBehaviors、refreshActivities 等内部方法，或让对应的 StateFlow 重新从 Repository 获取数据）。

### 验证标准

- ToolEventBus 编译通过
- ToolRegistry 构造函数注入 ToolEventBus 无报错
- HomeViewModel 收到 TIMING/BEHAVIOR 类事件后，首页行为列表自动刷新

---

## 工作包 2：暴露 ProcessNoteTool

### 需求

将现有的 NoteDirectiveParser + ApplyNoteDirectivesUseCase + NoteMatcher 三层管线封装为一个 ToolDefinition，让 AI 能调用与 UI 一致的备注解析逻辑。

### 交付物

#### 2.1 新增 `ProcessNoteTool`

**文件**: `core/tools/src/main/java/com/nltimer/core/tools/match/ProcessNoteTool.kt`

```kotlin
package com.nltimer.core.tools.match

@Singleton
class ProcessNoteTool @Inject constructor(
    private val noteMatcher: NoteMatcher,
    private val applyNoteDirectivesUseCase: ApplyNoteDirectivesUseCase,
    private val activityRepository: ActivityRepository,
    private val tagRepository: TagRepository,
) : ToolDefinition {

    override val name = "processNote"
    override val description = "解析用户备注文本，自动匹配或创建活动和标签。支持 @活动名、#标签名 指令和关键词匹配。"
    override val category = ToolCategory.SEARCH
    override val accessLevel = AccessLevel.WRITE
    override val returnType = Map::class

    override val parameters = listOf(
        ToolParameter(
            name = "note",
            type = ParameterType.STRING,
            description = "用户输入的备注文本",
            required = true,
            constraints = ParameterConstraint(minLength = 1, maxLength = 500),
        ),
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val note = args["note"] as? String
            ?: return ToolResult.Error(name, ToolError.ValidationError("note 必须是字符串"))

        val activities = activityRepository.getAllActive().first()
        val tags = tagRepository.getAllActive().first()

        val parsed = NoteDirectiveParser.parse(note)
        val directive = applyNoteDirectivesUseCase(parsed.directives, activities, tags)
        val scan = noteMatcher.scan(parsed.cleanedNote, activities, tags)

        val finalActivityId = directive.lastActivityId ?: scan.activityId
        val finalTagIds = (directive.addedTagIds + scan.tagIds).toList()

        return ToolResult.Success(
            name = name,
            data = mapOf(
                "activityId" to finalActivityId,
                "tagIds" to finalTagIds,
                "cleanedNote" to parsed.cleanedNote,
                "createdActivities" to directive.createdActivityNames,
                "createdTags" to directive.createdTagNames,
                "matchedActivities" to directive.matchedActivityNames,
                "matchedTags" to directive.matchedTagNames,
            ),
        )
    }

    override fun getDocumentation(): ToolDocumentation = ToolDocumentation(
        name = name,
        description = description,
        category = category,
        accessLevel = accessLevel,
        parameters = parameters,
        returnExample = """{"activityId":1,"tagIds":[2,3],"cleanedNote":"开会","createdActivities":[],"createdTags":[],"matchedActivities":["工作"],"matchedTags":["会议"]}""",
        errorExamples = listOf(
            ErrorExample("VALIDATION_ERROR", "note 必须是字符串", "note 参数缺失或非字符串"),
        ),
        usageExamples = listOf(
            """processNote(note="@工作 开会 #会议")""",
            """processNote(note="下午在写代码")""",
        ),
    )
}
```

#### 2.2 注册到 MatchToolsModule

**文件**: `core/tools/src/main/java/com/nltimer/core/tools/match/MatchToolsModule.kt`

新增：

```kotlin
@Binds
@IntoSet
abstract fun bindProcessNoteTool(impl: ProcessNoteTool): ToolDefinition
```

### 验证标准

- ProcessNoteTool 编译通过
- AI 工具列表中出现 `processNote`
- 调用 `processNote(note="@工作 开会")` 正确返回 activityId + tagIds

---

## 工作包 3：FAB 拖拽 → AI 快捷输入弹窗

### 需求

用户拖拽首页 FAB 选择 "AI" 选项，弹出 ModalBottomSheet，内含文本输入框。输入自然语言后，AI 通过 SSE 流式调用工具完成操作，执行结果实时反映到首页 UI。

### 架构决策：AI 层搬迁

当前 `AiInterApiClient`、`AiChatToolHelper`、`StreamEvent`、`ToolCallBuffer`、`ToolCallRecord`、`ToolSchemaJson` 在 `app` 模块的 `experimental/ai_inter/` 包下。`feature:home` 无法直接引用 `app` 模块（会循环依赖）。

**方案**：新增 `core:ai` Gradle 模块，将 AI 网络层和 Tool Calling 循环逻辑搬迁至此。`feature:home` 和 `app` 都依赖 `core:ai`。

### 交付物

#### 3.1 新增 `core:ai` 模块

**目录**: `core/ai/`

**build.gradle.kts**:

```kotlin
plugins {
    id("nltimer.android.library")
    id("nltimer.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nltimer.core.ai"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)

    implementation(projects.core.tools)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

#### 3.2 搬迁文件到 `core:ai`

从 `app/.../experimental/ai_inter/network/` 和 `app/.../experimental/ai_inter/viewmodel/` 搬入：

| 原路径 | 新路径 | 变更 |
|--------|--------|------|
| `network/StreamEvent.kt` | `core/ai/.../network/StreamEvent.kt` | 改 package 为 `com.nltimer.core.ai.network` |
| `network/AiNetModels.kt` | `core/ai/.../network/AiNetModels.kt` | 改 package |
| `network/AiInterApiClient.kt` | `core/ai/.../network/AiInterApiClient.kt` | 改 package |
| `network/ToolSchemaJson.kt` | `core/ai/.../network/ToolSchemaJson.kt` | 改 package |
| `viewmodel/AiChatToolHelper.kt` | `core/ai/.../toolcall/AiChatToolHelper.kt` | 改 package 为 `com.nltimer.core.ai.toolcall` |
| `viewmodel/AiChatToolHelper.kt` 中的 `ToolCallBuffer` | 同文件 | 随文件搬迁 |
| `viewmodel/AiInterViewModel.kt` 中的 `ToolCallRecord` | `core/ai/.../toolcall/ToolCallRecord.kt` | 提取为独立文件，改 package |

#### 3.3 更新 `app` 模块引用

`app/build.gradle.kts` 新增：`implementation(projects.core.ai)`

`app/.../experimental/ai_inter/viewmodel/AiInterViewModel.kt` 和 `AiAssistantChatViewModel.kt` 中：
- 删除原文件中的 `ToolCallRecord` 定义（已迁到 core:ai）
- import 路径从 `com.nltimer.app.experimental.ai_inter.network.*` 改为 `com.nltimer.core.ai.network.*`
- import 路径从 `AiChatToolHelper` 改为 `com.nltimer.core.ai.toolcall.*`

#### 3.4 更新 `feature:home` 依赖

`feature/home/build.gradle.kts` 新增：
```kotlin
implementation(projects.core.ai)
implementation(libs.okhttp)
implementation(libs.kotlinx.serialization.json)
```

#### 3.5 扩展 FAB 选项

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/ui/HomeScreen.kt`

变更点：
- `DragOptionsWithoutActive` 改为 `listOf("完成", "目标", "当前", "AI", "+自定义")`
- `DragOptionsWithActive` 改为 `listOf("完成", "放弃", "特记", "AI", "+自定义")`
- `onOptionSelected` 分发新增 `"AI" -> onShowAiQuickInput()`

#### 3.6 HomeUiState 新增状态

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/model/HomeUiState.kt`

新增字段：
```kotlin
val showAiQuickInput: Boolean = false,
```

#### 3.7 HomeViewModel 新增 AI 快捷输入逻辑

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/viewmodel/HomeViewModel.kt`

新增注入：
```kotlin
private val aiInterRepository: AiInterRepository,
private val apiClient: AiInterApiClient,
private val toolRegistry: ToolRegistry,
```

注意：`ToolRegistry` 可能已经通过其他路径间接可用，需要检查。`AiInterRepository` 在 `app` 模块中定义——**这会造成循环依赖**。

**修正方案**：将 `AiInterRepository` 和 `AiInterConfig` 也迁入 `core:ai` 模块。但 `AiInterRepository` 依赖 `AiCallLogDao`（Room DAO 在 app 模块），解耦需要额外工作。

**最终方案**：不在 HomeViewModel 中直接注入 AI 依赖，而是创建一个独立的 `AiQuickInputViewModel`，通过 Hilt `@HiltViewModel` 在 `AiQuickInputSheet` 中独立注入。HomeViewModel 只负责控制 `showAiQuickInput` 状态位的开关。

```kotlin
// HomeViewModel 新增
fun showAiQuickInput() { _uiState.update { it.copy(showAiQuickInput = true) } }
fun hideAiQuickInput() { _uiState.update { it.copy(showAiQuickInput = false) } }
```

#### 3.8 新增 `AiQuickInputViewModel`

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/viewmodel/AiQuickInputViewModel.kt`

```kotlin
@HiltViewModel
class AiQuickInputViewModel @Inject constructor(
    private val repository: AiInterRepository,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry,
) : ViewModel() {

    val config: StateFlow<AiInterConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiInterConfig(),
    )

    private val _state = MutableStateFlow<AiQuickInputState>(AiQuickInputState.Idle)
    val state: StateFlow<AiQuickInputState> = _state

    fun send(text: String) { /* tool calling loop */ }
    fun dismiss() { _state.value = AiQuickInputState.Idle }
}

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
```

**问题**：`AiInterRepository` 在 `app` 模块，`AiQuickInputViewModel` 在 `feature:home`，会有循环依赖。

**解决方案**：将 `AiInterRepository` + `AiInterConfig` + `AiCallLogDao` 迁入 `core:ai` 模块。`AiCallLogDao` 依赖 Room，所以 `core:ai` 也需要 Room 依赖。

**最终架构**：

```
core:ai
├── network/     (AiInterApiClient, StreamEvent, AiNetModels, ToolSchemaJson)
├── toolcall/    (AiChatToolHelper, ToolCallBuffer, ToolCallRecord)
├── data/        (AiInterConfig, AiInterRepository, AiCallLogDao, AiCallLogEntity, AiInterDatabase)
└── di/          (AiModule)
```

`app` 模块通过 `implementation(projects.core.ai)` 引用，删除原有的 `ai_inter/data/` 和 `ai_inter/network/` 和 `ai_inter/viewmodel/AiChatToolHelper.kt`（保留 AiInterViewModel 和 AiAssistantChatViewModel 因为它们是 UI 层的）。

#### 3.9 新增 `AiQuickInputSheet`

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/ui/components/AiQuickInputSheet.kt`

UI 结构：
- `ModalBottomSheet`
- 顶部：标题 "AI 助手" + 关闭按钮
- 中部：流式回复展示区（LazyColumn），显示 AI 文本 + 工具调用卡片（可折叠）
- 底部：文本输入框（最大 3 行）+ 发送按钮
- 状态：
  - Idle → 只显示输入框
  - Streaming → 显示输入框 + 正在生成的回复
  - Done → 显示回复 + 1.5s 后自动关闭
  - Error → 显示错误消息

#### 3.10 在 HomeScreen 中挂载弹窗

**文件**: `feature/home/src/main/java/com/nltimer/feature/home/ui/HomeScreen.kt`

在 Box 层级（与 BottomBarDragFab 同级）新增：

```kotlin
if (uiState.showAiQuickInput) {
    AiQuickInputSheet(
        onDismiss = { viewModel.hideAiQuickInput() },
    )
}
```

`AiQuickInputSheet` 内部通过 `hiltViewModel()` 获取 `AiQuickInputViewModel`。

### 验证标准

- FAB 拖拽出现 "AI" 选项
- 选择 AI 后弹出 AiQuickInputSheet
- 输入 "开始阅读" → AI 调用 startBehavior → 首页活跃行为卡片出现
- 输入 "结束" → AI 调用 endBehavior → 活跃行为消失
- 输入 "今天做了什么" → AI 调用 getDailySummary → 回复统计
- 工具执行后首页 UI 自动刷新（通过 ToolEventBus）

---

## 执行顺序

1. **工作包 1**：ToolEventBus（独立，无依赖）
2. **工作包 2**：ProcessNoteTool（独立，无依赖）
3. **工作包 3.1-3.4**：core:ai 模块创建 + 文件搬迁
4. **工作包 3.5-3.8**：FAB 选项 + ViewModel 逻辑
5. **工作包 3.9-3.10**：UI 组件 + 挂载
6. **集成测试**：端到端验证

## 技术风险

| 风险 | 缓解措施 |
|------|---------|
| core:ai 搬迁导致 app 模块编译失败 | 搬迁后立即编译验证 |
| AiInterRepository 迁移需要 Room 依赖 | core:ai build.gradle.kts 添加 room 依赖 |
| AiInterDatabase 在两个模块同时存在 | 将 AiInterDatabase 完全迁入 core:ai，app 模块通过 core:ai 访问 |
| ToolEventBus.emit 在 executeTool 内部调用可能导致递归 | emit 在 executeTool 返回值确定后调用，不递归 |
| SharedFlow 事件丢失（ViewModel 未收集时） | 使用 replay=0 + extraBufferCapacity=64，配合 Room Flow 作为最终一致性兜底 |
