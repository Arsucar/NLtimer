# AI 助手对话页设计

- **日期**：2026-05-18
- **作者**：AI Inter 模块负责人
- **状态**：设计已批准，待写实现计划
- **关联模块**：`app/src/main/java/com/nltimer/app/experimental/ai_inter/`

---

## 1. 背景与目标

### 1.1 背景

NLtimer 当前的 `AI Inter` 板块下已有"测试对话"（`AI_TEST_CHAT`）路由，承担调试 SSE 流、工具调用、reasoning 折叠等 AI 接入基础能力验证；UI 朴素，无抽屉/多会话/毛玻璃/Markdown 表格/导出能力，定位是开发期联调工具。

参考项目 `docs/reference/rikkahub` 是一个生产级 LLM 对话客户端，具备完整的对话界面交互（毛玻璃输入栏、抽屉式会话切换、消息气泡、自动跟随键盘、跳转浮动按钮、错误卡片栈、Markdown+代码高亮渲染等）。

### 1.2 目标

在 AI Inter 入口页新增一个独立路由 `AI_ASSISTANT_CHAT`，导向一个可以**正式上线、面向最终用户**的 AI 对话界面：

- UI/交互完整复刻 rikkahub 风格（抽屉 + 毛玻璃 + 圆角气泡 + 跳转浮动按钮 + 错误卡 + 加载动画）
- 支持多会话管理（创建/切换/重命名/删除）
- 文本 SSE 流式对话，复用现有 OpenAI 兼容 `AiInterApiClient`
- 工具调用多轮循环（复用现有 `ToolRegistry` + `AiInterViewModel.sendMessage` 工具循环逻辑）
- Markdown 渲染包含 GFM + 代码高亮 + 表格 + 图片
- 导出对话（Markdown / JSON，含工具调用详细信息）

### 1.3 非目标

- **不**做多模态附件（图片/视频/音频/文档上传）
- **不**引入 Assistant 抽象层；复用全局 `AiInterConfig`（API/Key/Model/promptChat）
- **不**做消息分叉树（`MessageNode.selectIndex`），消息平铺存储
- **不**做多 Provider（仅 OpenAI 兼容），不做 TTS/翻译/搜索/MCP/压缩上下文
- **不**移植 LaTeX (`MathBlock`) / Mermaid 渲染
- **不**删除旧的 `AI_TEST_CHAT`，将其作为开发调试视图继续保留共存

---

## 2. 架构

### 2.1 路由结构

```
AiInterRoute  (AI_INTER, 现有)
  ├── 提供商配置        (AI_PROVIDER_CONFIG, 现有)
  ├── 现有工具          (AI_TOOLS_LIST,      现有)
  ├── 调用日志          (AI_CALL_LOGS,       现有)
  ├── 提示词配置        (AI_PROMPT_CONFIG,   现有)
  ├── 测试对话          (AI_TEST_CHAT,       现有, 保留)
  └── AI 助手对话       (AI_ASSISTANT_CHAT,  新增) ★
        └── AiAssistantChatRoute → ChatScreen + Drawer
```

`AI_ASSISTANT_CHAT` 加入 `NLtimerRoutes.SETTINGS_FULLSCREEN_ROUTES` 集合，沉浸式无底部导航。

### 2.2 文件树（新增/修改）

```
app/src/main/java/com/nltimer/app/
├─ navigation/
│   ├─ NLtimerRoutes.kt                              [修改]
│   └─ NLtimerNavHost.kt                             [修改]
└─ experimental/ai_inter/
    ├─ AiInterScreen.kt                              [修改] +"AI 助手对话"入口卡片
    ├─ data/
    │   └─ AiInterDatabase.kt                        [修改] v2→v3，新增 2 张表，写 migration
    └─ chat/                                          [新增包]
        ├─ AiAssistantChatRoute.kt                   入口 + Scaffold + ModalNavigationDrawer
        ├─ AiAssistantChatViewModel.kt               独立 Hilt VM
        ├─ AiAssistantChatUiState.kt                 UI 状态聚合
        ├─ components/
        │   ├─ ChatTopBar.kt                         会话标题（点击改名）+ 模型名 + 抽屉/新建/导出按钮
        │   ├─ ChatDrawer.kt                         会话列表 + 长按重命名/删除
        │   ├─ ChatInput.kt                          毛玻璃圆角输入栏 + 模型快切 + 发送/中断
        │   ├─ ChatList.kt                           LazyColumn + 自动滚动 + 跳转按钮 + 错误浮卡
        │   ├─ ChatMessage.kt                        消息气泡 + ActionRow（复制/重生成/删除）
        │   ├─ ReasoningBlock.kt                     折叠思考过程
        │   ├─ ToolCallsBlock.kt                     折叠工具调用列表
        │   ├─ MessageJumper.kt                      左/右悬浮跳顶/翻页/跳底
        │   ├─ ChatBackground.kt                     主题渐变背景（无图）
        │   └─ ExportSheet.kt                        导出对话弹窗（Markdown / JSON）
        ├─ export/
        │   └─ ConversationExporter.kt               纯 Kotlin 序列化器
        ├─ markdown/                                  rikkahub markdown 子集移植
        │   ├─ MarkdownBlock.kt                      composable 入口
        │   ├─ HtmlBlockRenderer.kt                  Jsoup HTML → Compose blocks
        │   ├─ HtmlInlineRenderer.kt                 AnnotatedString builder
        │   ├─ CssStyleParser.kt                     style 属性解析
        │   ├─ DataTable.kt                          表格组件（rikkahub 移植）
        │   ├─ HighlightCodeBlock.kt                 接 highlight 模块
        │   ├─ ZoomableAsyncImage.kt                 Coil3 + 缩放手势
        │   └─ MarkdownPreprocess.kt                 代码块识别等预处理
        ├─ highlight/                                 rikkahub highlight 模块移植
        │   ├─ Highlighter.kt                        QuickJS 调度
        │   ├─ HighlightText.kt                      高亮结果 → AnnotatedString
        │   └─ assets/highlight.min.js               highlight.js bundle
        └─ data/                                      Room schema
            ├─ ConversationEntity.kt
            ├─ ConversationMessageEntity.kt
            ├─ ConversationDao.kt
            └─ ConversationMessageDao.kt

gradle/libs.versions.toml                              [修改] +haze/coil3/intellij-markdown/jsoup/quickjs
app/build.gradle.kts                                   [修改] +依赖
```

### 2.3 模块依赖图

```
AiAssistantChatRoute ──→ AiAssistantChatViewModel
                            ├── AiInterRepository    (复用，读 config + 写 ai_call_log)
                            ├── ConversationDao      (新增)
                            ├── ConversationMessageDao (新增)
                            ├── AiInterApiClient     (复用)
                            └── ToolRegistry         (复用，core/tools)

ChatList ──→ ChatMessage ──→ MarkdownBlock ──→ HtmlBlockRenderer ──→ Highlighter
                                          └──→ DataTable
                                          └──→ ZoomableAsyncImage (Coil3)

ChatInput / ChatList / ChatDrawer ──→ Haze (毛玻璃)
```

---

## 3. 数据模型

### 3.1 Conversation 实体

```kotlin
@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey val id: String,         // UUID
    val title: String,                  // 默认"新对话"；首条用户消息后由首句前 24 字符自动填入
    val createdAt: Long,                // 创建时间 epoch millis
    val updatedAt: Long,                // 最后一次消息时间 epoch millis，抽屉按此倒序
)
```

无 `pinned` 字段（MVP 范围外）。

### 3.2 ConversationMessage 实体

```kotlin
@Entity(
    tableName = "conversation_message",
    foreignKeys = [ForeignKey(
        ConversationEntity::class,
        ["id"], ["conversationId"],
        onDelete = CASCADE
    )],
    indices = [Index("conversationId")]
)
data class ConversationMessageEntity(
    @PrimaryKey val id: String,         // UUID
    val conversationId: String,         // 外键
    val order: Int,                     // 同会话内单调递增，UI 排序键
    val role: String,                   // "user" | "assistant"
    val content: String,                // 主要文本（assistant 含 Markdown）
    val reasoning: String = "",         // 推理链文本，空字符串表示无
    val toolCallsJson: String = "",     // 序列化 List<ToolCallRecord>，空串表示无
    val createdAt: Long,                // epoch millis
)
```

`toolCallsJson` 复用现有 `AiInterViewModel.serializeToolCalls` 同构 JSON 结构（id/name/arguments/result/success/durationMs）。

### 3.3 Migration

`AiInterDatabase` 从 v2 升 v3：

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conversation (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conversation_message (
                id TEXT NOT NULL PRIMARY KEY,
                conversationId TEXT NOT NULL,
                `order` INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                reasoning TEXT NOT NULL DEFAULT '',
                toolCallsJson TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(conversationId) REFERENCES conversation(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_conversation_message_conversationId
            ON conversation_message(conversationId)
        """.trimIndent())
    }
}
```

旧 `ai_call_log` 表与现有路由无任何变动。

### 3.4 DAO

```kotlin
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conv: ConversationEntity)

    @Query("UPDATE conversation SET title = :title, updatedAt = :ts WHERE id = :id")
    suspend fun rename(id: String, title: String, ts: Long)

    @Query("UPDATE conversation SET updatedAt = :ts WHERE id = :id")
    suspend fun touch(id: String, ts: Long)

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_message WHERE conversationId = :id ORDER BY `order` ASC")
    fun observeByConversation(id: String): Flow<List<ConversationMessageEntity>>

    @Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM conversation_message WHERE conversationId = :id")
    suspend fun nextOrder(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ConversationMessageEntity)

    @Query("DELETE FROM conversation_message WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversation_message WHERE conversationId = :id AND `order` >= :fromOrder")
    suspend fun deleteFromOrder(id: String, fromOrder: Int)
}
```

---

## 4. UI 设计

### 4.1 整体结构

```
ModalNavigationDrawer
├── drawerContent: ChatDrawer
│     ├── 头部："会话" 标题 + "新建对话" 按钮
│     └── LazyColumn: 会话条目
│           - 单击：切换当前会话
│           - 长按：弹出 ActionSheet（重命名/删除）
└── content:
    Surface (containerColor = transparent, ChatBackground 在底)
    └── Scaffold (containerColor = transparent)
        ├── topBar: ChatTopBar
        │     ├── leadingIcon: 抽屉按钮
        │     ├── title: Column { 会话标题(点击改名), 模型名 small }
        │     └── actions: 导出 / 清空当前会话 / 新建
        ├── bottomBar: ChatInput (毛玻璃)
        └── content: ChatList
              ├── LazyColumn(messages + streaming)
              ├── MessageJumper (right side, 浮动)
              └── ErrorCard (bottom center, animated)
```

### 4.2 ChatTopBar

- `colors = topAppBarColors(containerColor = Color.Transparent)`
- 标题：会话标题（点击弹 AlertDialog 改名）；副标题：模型名 + provider 简称，labelSmall 8sp 0.65 alpha
- 右侧按钮：`Export`、`MoreVert` 下拉菜单（清空当前会话 / 全部清空）
- 滚动时 TopBar 与 Haze 联动，背景透明，靠 ChatList 上方留 16dp 透气

### 4.3 ChatInput（核心）

```
┌───────────────────────────────────────────┐
│  [TextField 多行，最多 5 行]    [全屏按钮] │
├───────────────────────────────────────────┤
│  [模型快切胶囊]              [+] [⬆/■]    │
└───────────────────────────────────────────┘
  ↑ Surface, largeIncreased 圆角, hazeEffect
```

- 输入栏：`Surface(shape = MaterialTheme.shapes.largeIncreased)` + `Modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())`
- 字段：`TextField` with `TextFieldLineLimits.MultiLine(maxHeightInLines = 5)`, `unfocusedIndicatorColor = Transparent`
- 模型快切胶囊：点击弹 `ModalBottomSheet`，列出 `fetchModels()` 缓存或现拉
- 发送按钮：30dp 圆形，状态机：空 → `surfaceContainerHigh` + alpha38；非空 → `primary`；loading → `errorContainer` + Cancel 图标
- `imePadding()` + `navigationBarsPadding()` 自动避让键盘和系统手势

### 4.4 ChatList

- `LazyColumn(state = listState, contentPadding = PaddingValues(16.dp) + bottom innerPadding)` 16dp 边距 + 12dp item 间距
- `Modifier.hazeSource(hazeState)` 作为 ChatInput 模糊源
- 自动滚动：流式生成时若用户滚动到底部，新 token 自动推动到底；用户上滚后不再强制滚动（参考 rikkahub `enableAutoScroll` 逻辑，但去掉音量键滚动）
- 浮动 `MessageJumper`：右侧悬浮，4 按钮（顶/上页/下页/底），仅在最近 1.5s 内有滚动操作时显示
- 错误浮卡：底部中心，slide-in，可逐条 dismiss
- 加载指示：流式过程末尾 item 显示 `CircularProgressIndicator + 处理状态文本`（不移植 rikkahub 兔子动画，避免增加资源）

### 4.5 ChatMessage 气泡

```
User 消息（右侧对齐）：
┌────────────────────────┐
│ Surface primaryContainer│
│ shape RoundedCorner 16  │
│  └─ MarkdownBlock       │
└────────────────────────┘
点击气泡 → 进入编辑状态（填回输入框）

Assistant 消息（左侧 / 全宽）：
┌─ ReasoningBlock (折叠)
│
├─ ToolCallsBlock (折叠，逐条卡片)
│
└─ MarkdownBlock (全宽，无气泡)

最下方 ActionRow（最后一条非加载时显示）：
[复制] [重生成] [删除]
```

- 角色判定：`role == "user"` 右侧对齐，最大宽 340dp，`primaryContainer`
- assistant 默认无气泡（rikkahub `showAssistantBubble=false` 风格），更直观显示长文
- ActionRow `AnimatedVisibility` 慢入慢出

### 4.6 ChatDrawer

- 顶部："对话" + IconButton(MessageAdd) 新建
- LazyColumn：会话条目，每条 `Surface(shape = RoundedCornerShape(12.dp))`，已选中用 `secondaryContainer`，未选中 `Color.Transparent`
- 长按弹出 ModalBottomSheet：重命名 / 删除（删除前 ConfirmDialog）
- 底部固定区："AI Inter 设置"链接，跳回 `AiInterScreen`

### 4.7 ChatBackground

- MVP 不引入背景图。`Brush.verticalGradient(listOf(surface, surfaceContainerLow.copy(alpha = 0.5f)))` 全屏填充，给 Haze 提供基底
- 留接口 `background: Brush?`，未来可扩展为图片+遮罩

### 4.8 ExportSheet

```
┌───────────────────────────────────┐
│ 导出当前对话                       │
│                                   │
│ 格式：( ) Markdown   ( ) JSON      │
│                                   │
│ ☐ 包含工具调用详细信息（参数/结果）│
│ ☐ 包含 reasoning 思考过程          │
│                                   │
│ [预览]              [复制] [分享]   │
└───────────────────────────────────┘
```

- ModalBottomSheet，2 个 RadioButton + 2 个 Checkbox（默认全勾选）
- "复制"：调用 `clipboardManager.setText` 并 toast 提示
- "分享"：`Intent.ACTION_SEND` text/plain（Markdown）或 application/json
- 预览：显示前 2000 字符滚动 Text，用于确认格式

---

## 5. Markdown 渲染管线

### 5.1 数据流

```
Markdown text
    │
    ▼
preProcess()                  // 转换 \(...\) 为 $...$（保留，但不渲染 LaTeX；为未来扩展留位）
    │
    ▼
MarkdownParser(GFM)           // org.jetbrains:markdown
    │
    ▼
HtmlGenerator.generateHtml()  // → HTML 字符串
    │
    ▼
Jsoup.parse()                 // → Document
    │
    ▼
HtmlBlockRenderer (Compose)
    │
    ├─ <p>            → Text(AnnotatedString)（HtmlInlineRenderer 构建）
    ├─ <h1>~<h6>      → Text + 调整字号/边距
    ├─ <ul>/<ol>      → 自绘 bullet + 缩进 + 任务列表
    ├─ <pre><code>    → HighlightCodeBlock (QuickJS + highlight.js)
    ├─ <blockquote>   → 左侧边条 + 浅底背景
    ├─ <table>        → DataTable（rikkahub 移植）
    ├─ <hr>           → HorizontalDivider
    ├─ <img>          → ZoomableAsyncImage（Coil3）
    ├─ <details>      → 自定义折叠
    └─ <a>/<strong>/<em>/<code>/... → AnnotatedString span
```

### 5.2 移植边界

| 文件 | 移植 | 备注 |
|---|---|---|
| `MarkdownNew.kt`              | ✓ 改名 `MarkdownBlock.kt` | 主入口 |
| `Markdown.kt`                 | ✗ | 由 MarkdownNew 取代 |
| `HighlightCodeBlock.kt`       | ✓ | 接 highlight 模块 |
| `ZoomableAsyncImage.kt`       | ✓ | Coil3 + zoomable |
| `DataTable.kt`                | ✓ | 表格 |
| `MathBlock.kt` / `MathInline` | ✗ | 不做 LaTeX |
| `Mermaid.kt`                  | ✗ | 不做图表 |
| `SimpleHtmlBlock.kt`          | ✗ | 不做 inline HTML |
| `MarkdownWeb.kt`              | ✗ | 不用 WebView |

### 5.3 与 rikkahub 的关键去除项

- `LocalSettings.displaySetting.enableLatexRendering` → 删除分支，永远走非 LaTeX 路径
- `replaceRegexes(assistant, scope, visual)` → 删除，content 直接进入渲染
- `Citation` 内联占位 / `Favicon` → 删除（不做搜索）
- `JetbrainsMono` 字体引用 → 改为 `FontFamily.Monospace`
- `me.rerere.rikkahub.utils.toDp` → 复制实现到本地 utils

### 5.4 highlight 模块移植

直接复制 `docs/reference/rikkahub/highlight/` 下两个 Kotlin 文件 + assets 到 `chat/highlight/` 包：

- `Highlighter.kt`：包装 QuickJS 调用 highlight.js 的 `highlight(lang, code)`
- `HighlightText.kt`：JSON → SpanStyle 列表 → AnnotatedString
- `assets/highlight.min.js`：复制 rikkahub assets 同名文件到 `app/src/main/assets/`

依赖：`wang.harlon.quickjs:wrapper-android`（或 rikkahub 使用的同款）。若坐标不可用，降级方案是 `org.mozilla:rhino-runtime`（包体较大但稳定）。

---

## 6. ViewModel 与状态

### 6.1 接口

```kotlin
@HiltViewModel
class AiAssistantChatViewModel @Inject constructor(
    private val repository: AiInterRepository,
    private val conversationDao: ConversationDao,
    private val messageDao: ConversationMessageDao,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry,
) : ViewModel() {

    val config: StateFlow<AiInterConfig>
    val conversations: StateFlow<List<ConversationEntity>>
    val currentConversationId: StateFlow<String?>
    val currentMessages: StateFlow<List<ConversationMessageEntity>>
    val streamingState: StateFlow<StreamingState>      // 复用现有结构
    val isSending: StateFlow<Boolean>
    val chatError: StateFlow<String?>
    val availableModels: StateFlow<List<String>>

    fun selectConversation(id: String)
    fun newConversation()                              // 创建新会话并切换
    fun renameConversation(id: String, title: String)
    fun deleteConversation(id: String)
    fun clearCurrent()                                  // 删除当前会话所有消息但保留会话
    fun sendMessage(text: String)
    fun stopStreaming()
    fun regenerateLastAssistant()                      // 删最后一条 assistant + sendMessage(上一条 user.content)
    fun deleteMessage(id: String)
    fun copyMessage(id: String): String                // 返回拼接好的文本
    fun exportConversation(
        id: String,
        format: ExportFormat,
        includeTools: Boolean,
        includeReasoning: Boolean,
    ): String
    fun refreshModels()
}

enum class ExportFormat { MARKDOWN, JSON }
```

### 6.2 sendMessage 复用现有工具循环

直接复用 `AiInterViewModel.sendMessage` 中已经实现的：
- wireHistory 构建（system prompt + 历史 user/assistant + tool 回执）
- `apiClient.streamChat(...).collect { StreamEvent... }` 流式收集
- `MAX_TOOL_ROUNDS = 5` 工具调用多轮循环
- `executeToolCall` 工具执行 + 结果写回
- `TOOLS_SYSTEM_PROMPT` 强制规则 + `cfg.promptChat` 用户追加
- `repository.addLog(AiCallLogEntity(...))` 调用日志双写

差异点：
- 历史来源从 `_chatMessages` 改为 `messageDao.observeByConversation(currentId).first()`
- 写入消息走 `messageDao.insert(ConversationMessageEntity(...))` + `conversationDao.touch(id, now)`
- 首条用户消息后自动 `conversationDao.rename(id, title=firstLineTake(24), now)`

### 6.3 流式状态机

```
state                  isSending  streamingState
─────────────────────  ─────────  ──────────────────
Idle                   false      empty
PreparingRequest       true       empty
Streaming              true       { reasoning?, content?, toolCalls? }
ToolExecuting          true       { ..., toolCalls += record }
ContinuingAfterTool    true       streaming round N+1
Completed              false      empty  (消息已 insert)
Errored                false      empty  (chatError 非空 + 已 insert 部分内容)
Cancelled              false      empty  (已 insert "(已中断)")
```

---

## 7. 导出（核心增量）

### 7.1 ConversationExporter

```kotlin
class ConversationExporter {

    fun exportMarkdown(
        conversation: ConversationEntity,
        messages: List<ConversationMessageEntity>,
        includeTools: Boolean,
        includeReasoning: Boolean,
    ): String

    fun exportJson(
        conversation: ConversationEntity,
        messages: List<ConversationMessageEntity>,
        includeTools: Boolean,
        includeReasoning: Boolean,
    ): String
}
```

### 7.2 Markdown 格式

````markdown
# 新对话标题

- 创建时间：2026-05-18 21:30:15
- 导出时间：2026-05-18 22:05:42
- 消息数：8

---

## 用户 · 21:30:15

请帮我从今天 10 点到 14 点记录一段"看书"活动。

---

## 助手 · 21:30:18

<details><summary>思考过程</summary>

用户指定了明确的时间区间……
（reasoning 全文）

</details>

<details><summary>工具调用 (1)</summary>

### 1. recordBehavior · 142ms · 成功

**参数：**
```json
{"activityName":"看书","startTime":"2026-05-18T10:00:00+08:00","endTime":"2026-05-18T14:00:00+08:00"}
```

**结果：**
```json
{"id":42,"status":"recorded"}
```

</details>

已为你记录"看书"活动，时间 10:00 - 14:00。

---
````

### 7.3 JSON 格式

```json
{
  "conversation": {
    "id": "<uuid>",
    "title": "新对话标题",
    "createdAt": 1763438215000,
    "updatedAt": 1763441142000
  },
  "messages": [
    {
      "id": "<uuid>",
      "order": 0,
      "role": "user",
      "content": "请帮我从今天 10 点到 14 点记录一段「看书」活动。",
      "createdAt": 1763438215000
    },
    {
      "id": "<uuid>",
      "order": 1,
      "role": "assistant",
      "content": "已为你记录「看书」活动……",
      "reasoning": "用户指定了明确的时间区间……",
      "toolCalls": [
        {
          "id": "call_<n>",
          "name": "recordBehavior",
          "arguments": "{...}",
          "result": "{...}",
          "success": true,
          "durationMs": 142
        }
      ],
      "createdAt": 1763438218000
    }
  ],
  "exportedAt": 1763441142000,
  "schemaVersion": 1
}
```

`schemaVersion: 1` 字段为未来工具多合一分析做兼容预留。时间戳统一使用 epoch milliseconds（UTC）。

### 7.4 导出动作

- ExportSheet 内：
  - **复制到剪贴板**：`ClipboardManager.setPrimaryClip(ClipData.newPlainText("AI 对话", text))`，Toast "已复制"
  - **分享**：`Intent.ACTION_SEND` + `type = "text/plain"` 或 `"application/json"`
- 不写到文件系统，避免权限问题；JSON 通过 EXTRA_TEXT 携带，超大对话由用户自己保存到文件

---

## 8. 依赖引入

### 8.1 libs.versions.toml 新增

```toml
[versions]
haze = "1.6.0"
coil3 = "3.0.4"
intellijMarkdown = "0.7.3"
jsoup = "1.18.3"
quickjs = "1.1.0"

[libraries]
haze                 = { module = "dev.chrisbanes.haze:haze",                version.ref = "haze" }
haze-materials       = { module = "dev.chrisbanes.haze:haze-materials",      version.ref = "haze" }
coil3-compose        = { module = "io.coil-kt.coil3:coil-compose",           version.ref = "coil3" }
coil3-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp",    version.ref = "coil3" }
intellij-markdown    = { module = "org.jetbrains:markdown",                  version.ref = "intellijMarkdown" }
jsoup                = { module = "org.jsoup:jsoup",                         version.ref = "jsoup" }
quickjs-android      = { module = "wang.harlon.quickjs:wrapper-android",     version.ref = "quickjs" }
```

HugeIcons 不引入，使用 material-icons-extended（已有依赖）。

### 8.2 app/build.gradle.kts 新增

```kotlin
dependencies {
    // ... 现有依赖

    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.intellij.markdown)
    implementation(libs.jsoup)
    implementation(libs.quickjs.android)
}
```

### 8.3 资源新增

- `app/src/main/assets/highlight.min.js` （从 rikkahub 同名文件复制）
- `app/src/main/assets/highlight-styles/atom-one-dark.css` 等（按需）

---

## 9. 关键交互流程

### 9.1 首次进入

1. `AiInterScreen` → 点击"AI 助手对话"卡片
2. 路由 `AI_ASSISTANT_CHAT` 加载 `AiAssistantChatRoute`
3. ViewModel 初始化：
   - `conversations.observe` → 抽屉
   - 若历史会话非空，自动选中 `updatedAt` 最新的一条
   - 若无任何会话，调用 `newConversation()` 创建空会话
4. UI 显示空 ChatList + 输入栏 + 当前模型名（若未配置，TopBar 副标题红色提示"未选模型"）

### 9.2 发送消息

1. 用户在输入框输入文本，按发送按钮
2. ViewModel.sendMessage(text)：
   - 立即 `messageDao.insert(ConversationMessageEntity(role="user", ...))`
   - 立即 `_isSending.value = true`
   - 协程内拼 wireHistory，调 `apiClient.streamChat`
3. 流式 token：累积到 `_streamingState`，ChatList 显示 "正在生成…" 卡片 + 实时渲染
4. 工具调用：检测到 tool_calls 时执行工具，把结果写回 wireHistory，继续下一轮 SSE，最多 5 轮
5. 完成：
   - `messageDao.insert(ConversationMessageEntity(role="assistant", content, reasoning, toolCallsJson, ...))`
   - `conversationDao.touch(id, now)`
   - 若该会话此前 title 仍是默认"新对话"，调用 `conversationDao.rename(id, title=firstUserMsg.take(24), now)`
   - `_streamingState = empty`, `_isSending = false`
   - `repository.addLog(...)` 双写 ai_call_log

### 9.3 中断

- 用户在加载中按发送按钮（变 Cancel 图标）→ `currentStreamJob.cancel()`
- ViewModel finally 块：
  - 若已有部分内容，insert 一条 `(已中断)` 后缀的 assistant 消息
  - 否则不 insert
  - `_isSending = false`

### 9.4 重生成

- ActionRow 点"重生成"：
  - 取最后一条 assistant 消息：`messageDao.delete(lastAssistantId)`
  - 取倒数第二条 user 消息的 content
  - 调用 `sendMessage(content)`（不创建新 user，因为已存在）—— 实际实现中走 `regenerateLastAssistant` 路径，跳过 user insert，直接发起流式

### 9.5 删除消息

- ActionRow 点"删除"：ConfirmDialog → `messageDao.delete(id)`
- UI 自动通过 Flow 重组刷新

### 9.6 切换会话

- 抽屉点击某个会话：
  - `_currentConversationId.value = id`
  - `currentMessages` 触发新的 Flow collection
  - 关闭 Drawer
  - ChatList 自动滚到底

### 9.7 导出

- TopBar Export 按钮 → 打开 ExportSheet
- 选格式 + 勾选项 → 调 `viewModel.exportConversation(currentId!!, format, ...)`
- 显示预览（前 2000 字符）
- 复制 / 分享按钮

---

## 10. 错误处理

| 场景 | 处理 |
|---|---|
| `cfg.apiAddress` 为空 | 发送按钮 disabled；TopBar 副标题红字"请先配置 API" + 点击跳 `AI_PROVIDER_CONFIG` |
| `cfg.modelName` 为空 | 发送时弹 Toast "请先选择模型"，不发起请求 |
| SSE 流抛异常 | finally 块捕获，`chatError = message`，UI 显示错误浮卡（可 dismiss），已 insert 的部分内容保留 |
| 工具执行失败 | `ToolResult.Error` 序列化为 result 字符串，标记 `success = false`，回传给模型继续推理 |
| 工具循环超 5 轮 | 跳出循环，最后一段 content 作为 assistant 消息保存 |
| 空响应 | 写入提示文本"(空响应 — 服务端正常关闭流但未发送任何 token...)" |
| Migration 失败 | `fallbackToDestructiveMigration` 不开启；崩溃时由 Room 抛出，要求用户重装或清缓存 |
| QuickJS 加载失败 | HighlightCodeBlock 降级为纯 `Text + FontFamily.Monospace`，不阻塞 Markdown 渲染 |

---

## 11. 测试策略

### 11.1 单元测试

- `ConversationDao` / `ConversationMessageDao`：使用 Room in-memory DB，验证 CRUD、外键级联删除、order 单调
- `ConversationExporter`：固定输入 → 验证 Markdown / JSON 输出字符串（snapshot test）
- `MarkdownPreprocess`：验证代码块识别 + LaTeX 占位符替换（即使不渲染也要正确预处理）

### 11.2 集成测试

- `AiAssistantChatViewModel.sendMessage`：用 FakeApiClient + FakeToolRegistry 模拟一次完整流式 + 一轮工具调用，验证最终 `currentMessages` 里有 user + assistant 两条，且 toolCallsJson 非空
- Migration v2→v3：构造 v2 schema 数据，跑 migration，验证新表存在且旧 `ai_call_log` 数据完整

### 11.3 UI smoke

- Compose UI test：
  - 启动 `AiAssistantChatRoute` → 检查 ChatInput / TopBar 渲染
  - 输入文本 + 点击发送（Fake VM）→ 检查 message item 出现
  - 抽屉打开 / 切换会话

---

## 12. 性能与可访问性

- **Markdown 渲染**：长消息（>2k 字符）通过 `LaunchedEffect + Flow.mapLatest` 在 Dispatchers.Default 转换 HTML，避免主线程卡顿（与 rikkahub 同策略）
- **Haze 模糊**：受 `settings.displaySetting.enableBlurEffect` 控制（接现有设置；如未实现则硬编码开启），低端机可关闭。MVP 默认开启
- **代码高亮**：QuickJS 调用在 IO 线程，结果缓存到 `remember(code, language)`，重组不重新跑 JS
- **自动滚动**：仅在用户位于底部时自动跟随，避免打断阅读
- **图片**：Coil3 默认 LRU 缓存 + 磁盘缓存
- **A11y**：所有 IconButton 都有 `contentDescription`；TopBar 标题点击区域 ≥ 48dp；消息复制走系统剪贴板，无障碍可访问

---

## 13. 安全与隐私

- API Key 已加密存储在 DataStore（沿用 `AiInterRepository`），导出对话**不包含** Key
- 导出 JSON 包含完整 reasoning / tool args / results，用户需自行注意隐私（弹窗提示一次"导出内容可能包含敏感信息"）
- 不写本地文件，避免外部存储权限

---

## 14. 升级与回滚

- DB 升级：v2→v3，新增 2 张表，**不会**影响 v1→v2 已有迁移（如有）
- 回滚：删除 `chat/` 包 + 撤销 `NLtimerRoutes`/`NavHost` 的 AI_ASSISTANT_CHAT 即可，新增的表不会影响其他功能；如需彻底清除可手动 DROP TABLE
- 依赖回滚：移除 haze/coil3/jsoup/intellij-markdown/quickjs 即可，对其他模块零侵入

---

## 15. 未实现的扩展位（不在本次范围）

- LaTeX 渲染：保留 `MarkdownPreprocess.kt` 中的占位符转换，后续接入 `MathBlock` 即可
- 图片附件上传：Coil3 已引入，加 FilePicker + 多模态请求体即可
- 助手多管理：保留扩展位 `ChatBackground(background: Brush?)`，可演化
- 分支重生成：当前 schema 没有 `selectIndex`，未来要加分叉需 migration v3→v4
- 多 Provider：ApiClient 可抽象 `ChatProvider` 接口，未来扩展

---

## 附录 A：rikkahub 与本设计差异速查

| 维度 | rikkahub | 本设计 |
|---|---|---|
| DI | Koin | Hilt |
| 助手 | Assistant 实体，多助手切换 | 复用全局 AiInterConfig |
| 消息模型 | MessageNode 分叉树 | 平铺列表 |
| 多模态 | 图/视/音/文档 | 纯文本 |
| Provider | 多 Provider 抽象 | 单 OpenAI 兼容 |
| Markdown | GFM + LaTeX + Mermaid + Table + Citation | GFM + Code + Table（核心子集） |
| 图标 | HugeIcons | Material Icons Extended |
| 模糊 | Haze | Haze（一致） |
| 字体 | JetbrainsMono | Monospace 系统字体 |
| 设置 | LocalSettings.displaySetting | 不引入设置上下文（硬编码默认） |
| 导出 | 长截图 + Markdown | Markdown + JSON（含工具详细信息） |
| TTS / 翻译 / 搜索 | 有 | 无 |
| 抽屉自适应 | 平板 PermanentDrawer | 固定 ModalDrawer |

---

## 附录 B：分阶段实施建议（给 writing-plans 参考）

1. **阶段 1**：依赖 + Migration + DAO + ViewModel 骨架（无 UI）
2. **阶段 2**：highlight 模块移植 + Markdown 核心（不含表格）
3. **阶段 3**：Markdown 表格 + 图片
4. **阶段 4**：ChatScreen + ChatList + ChatMessage + ChatInput（无毛玻璃，纯逻辑）
5. **阶段 5**：Haze 毛玻璃 + ChatDrawer + 跳转浮动按钮
6. **阶段 6**：ExportSheet + ConversationExporter
7. **阶段 7**：端到端 smoke 测试 + 入口接通
