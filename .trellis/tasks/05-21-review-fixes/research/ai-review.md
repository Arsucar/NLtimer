# AI 聊天链路审查报告（第 5 路）

> 审查范围：feature/ai（chat 全链路 + AiInter 旧链路）+ core/ai（config/network/toolcall）
> 时间：2026-08-01
> 说明：仅修复确定性高、风险低的项；复杂重构与依赖升级仅记录。未运行任何构建命令。

## 模块清单

- `core/ai/network/AiInterApiClient.kt` — SSE 流式客户端
- `core/ai/network/StreamEvent.kt` — 流事件
- `core/ai/toolcall/ToolCallBuffer.kt` / `ToolCallRecord.kt` / `AiChatToolHelper.kt` — 工具调用缓冲与执行
- `core/ai/config/AiInterConfig.kt` / `AiConfigProvider.kt` — 配置
- `feature/ai/chat/AiAssistantChatViewModel.kt` — 新对话助手 VM（持久化）
- `feature/ai/viewmodel/AiInterViewModel.kt` — 旧测试对话 VM
- `feature/ai/chat/markdown/*` — 自研 HTML/Markdown 渲染（Jsoup 树 → Compose 文本）
- `feature/ai/chat/highlight/*` — 代码高亮（QuickJS + prism.js）
- `feature/ai/chat/data/*` — Room 持久化（conversation/conversation_message）
- `feature/ai/chat/export/*` — 导出

---

## P0（已修复 — bug）

### 1. 停止生成时重复追加"已中断"消息
- 文件：`feature/ai/viewmodel/AiInterViewModel.kt:392`（stopStreaming）
- 问题：`stopStreaming()` 先把部分内容以"(已中断)"追加到 `_chatMessages`，同时被取消协程的 `finally`（wasCancelled 分支）又追加一次同一内容 → 界面出现两条重复消息。
- 修复：`stopStreaming()` 只 `cancel()`，消息与状态重置统一由 finally 处理（AiAssistantChatViewModel 无此问题，未动）。

### 2. 清空对话后部分消息"复活"
- 文件：`feature/ai/viewmodel/AiInterViewModel.kt:155`（clearChat）+ `finally`
- 问题：`clearChat()` 取消任务并清空 `_chatMessages`，但被取消协程的 finally 仍会追加"(已中断)"消息 → 清空后又冒出一条。
- 修复：新增 `clearRequested` 标记 + 任务身份守卫 `if (currentStreamJob === job)`；finally 在 wasCancelled 分支跳过消息追加；`sendMessage` 启动时复位标记。旧任务在已被新任务接管时不再改共享状态。

### 3. 重生成时重复插入用户消息
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt:354`（regenerateLastAssistant）
- 问题：先删除最后一条 assistant，再 `sendMessage(precedingUser.content)` 会再插入一条相同内容的 user 消息 → 对话中出现两条连续重复用户消息。
- 修复：`sendMessage(text, persistUserMessage: Boolean = true)` 增加参数（默认值保持既有调用点不变），重生成时传 `persistUserMessage = false`，不再重复插入 user 消息。

### 4. 流式发送期间删除会话 → finally 抛 FK 约束异常崩溃
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt` finally（assistant 持久化 + addLog）
- 问题：发送中从抽屉删除当前会话，`finally` 里 `messageDao.insert`（FK 指向已删除的 conversation）抛 `SQLiteConstraintException`，从 finally 冒泡 → viewModelScope 未捕获 → 应用崩溃。
- 修复：assistant 插入 + `conversationDao.touch` + `addLog` 用 `runCatching` 包裹（状态重置仍在 try 外，必然执行）。

### 5. 模型未下发 tool_call id 时，assistant 与 tool 回执 id 不一致
- 文件：`core/ai/toolcall/AiChatToolHelper.kt:103`（buildAssistantToolMessage）
- 问题：`buildAssistantToolMessage` 用 `buf.id ?: ""`，而 `executeToolCall` 用 `buf.id ?: "call_${System.nanoTime()}"`。模型全程未给 id 时 assistant.tool_calls[].id = ""，而 tool 回执 tool_call_id = "call_xxx" → 多轮工具调用时 API 报 id 不匹配。
- 修复：构建 assistant 消息时若 `buf.id == null`，生成一次 `call_<nanoTime>` 并写回 buffer，后续 executeToolCall 复用同一 id。

### 6. Highlighter 初始化 crash 隐患
- 文件：`feature/ai/chat/highlight/Highlighter.kt:27`
- 问题：`init { executor.submit { context } }` 在后台线程触发 lazy 初始化（QuickJS 创建 + 读 assets），任何初始化失败（缺 prism.js、native 加载失败）都会在 executor 线程上抛出未捕获异常 → 直接崩溃；且所有代码块都会立即创建 JS 上下文。
- 修复：删除该 init 预热块；lazy 改由 `highlight()`（已包 runCatching）触发。

---

## P1（已修复 — 性能 / 泄漏 / 安全）

### 7. SSE 多行/美化 JSON 事件被拆行解析
- 文件：`core/ai/network/AiInterApiClient.kt:173`（onEvent）
- 问题：`data.trim().split("\n")` 把整块事件按行拆开逐行解析，若 provider 输出多行/美化 JSON（一个事件一行 JSON），每行都不是完整 JSON → 全部解析失败 → 内容静默丢失，直到超时。
- 修复：先整块解析，失败才按行回退；每个 chunk 的处理仍包 try/catch，坏 chunk 不影响整条流。

### 8. 代码块高亮线程/JS 上下文泄漏
- 文件：`feature/ai/chat/highlight/Highlighter.kt:91`、`feature/ai/chat/markdown/HighlightCodeBlock.kt:36`
- 问题：每个代码块 `remember { Highlighter(context) }` 创建独立单线程池（非 daemon 线程）+ QuickJS 上下文，滚动出视口后无人调用 `destroy()` → 每个代码块永久泄漏一条线程 + 原生内存。
- 修复：
  - `HighlightCodeBlock` 增加 `DisposableEffect`，离开组合时 `destroy()`；
  - `destroy()` 增加 `destroyed` 标记 + 把上下文销毁任务排到单线程池队尾再 `shutdown()`，避免与进行中的高亮任务竞争；任务内补 `continuation.isActive` 检查，避免取消后 `resume` 抛 AlreadyResumed。
  - 后续更优做法（未做，需改动调用链）：全屏共享单个 Highlighter（经 `LocalHighlighter` 提供），避免每代码块一个 JS 引擎。

### 9. Markdown 渲染 URL/图片协议未过滤（prompt 注入面）
- 文件：`feature/ai/chat/markdown/HtmlInlineRenderer.kt`（a/img 分支）、`HtmlBlockRenderer.kt`（img 分支）
- 问题：模型输出（可被用户输入诱导）可包含 `<a href="javascript:...">`、`<a href="intent://...">`、`<img src="file:///...">`、`<img src="content://...">`；`LinkAnnotation.Url` 点击经系统 UriHandler 拉起、Coil 可读取本地 file/content 资源 → 越权拉起应用 / 本地文件读取面。
- 修复：新增 `isSafeWebLink`（仅 http/https/mailto/tel 渲染为可点击）与 `isSafeImageSource`（仅 http/https 或相对路径加载）；其余按纯文本输出。无 WebView，脚本无法执行，但资源读取面已收敛。

---

## P2（本轮续修 — 已修复）

### 13. `refreshModels` 失败静默 ✅ 已修复
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt`、`AiAssistantChatRoute.kt`
- 修复：增加 `modelsError: StateFlow<String?>` + `clearModelsError()`；失败时 `Log.e` + 写入可读错误文案；空列表成功也提示；模型选择 bottom sheet 优先展示 `modelsError`（error 色）。

### 14. `newConversation()` 在 init 无条件新建会话 ✅ 已修复
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt`
- 修复：
  - `init`：已有会话则选最近一条（`observeAll` 按 updatedAt DESC），仅在无会话时 `createBlankConversation()`。
  - `newConversation()`：优先复用「标题=新对话 且 无消息」（`nextOrder == 0`）的空会话，否则才真正插入。
  - `deleteConversation` 删光后走 `createBlankConversation()`，不经复用逻辑。

### 15. `clearCurrent()` 流式中会复活消息 ✅ 已修复（实为 bug）
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt`
- 修复：仿 `AiInterViewModel` 增加 `clearRequested` + `streamJob === thisJob` 守卫；`clearCurrent` 置位并 cancel、立刻重置 streaming/isSending；finally 在 `wasCancelled && clearRequested` 时跳过 assistant 插入；`sendMessage` 启动时复位标记。

---

## P2（记录，未修复 — 建议后续处理）

### 10. `sendMessage` 复杂度过高（cyclomatic 28-30）
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt`、`feature/ai/viewmodel/AiInterViewModel.kt`
- 问题：多轮循环 + 流收集 + finally 分支使函数超长（detekt LongMethod / CyclomaticComplexMethod）。
- 建议：拆为 `runToolLoop() / persistAssistant() / buildAssistantFallback()` 等私有方法；两 VM 可共享同一工具循环实现。本轮未拆（低风险优先，避免引入回归）。

### 11. SSE 流式 usage（token 计数）未采集
- 文件：`core/ai/network/AiInterApiClient.kt`（onEvent）
- 问题：`stream_options.include_usage` 已请求，但终包 `choices: [] + usage` 被 `if (choices.isEmpty()) return@forEach` 丢弃；`AiCallLogEntity.requestTokens/responseTokens` 恒为 0。
- 建议：在 StreamEvent 增加 `Usage(promptTokens, completionTokens)`（或回调），在 finally 写入日志。本轮未做（需扩展 StreamEvent + 两端 VM 写日志，改动面略大）。

### 12. API Key 明文存储
- 文件：`feature/ai/data/AiInterRepository.kt:27`
- 问题：`api_key` 明文存 DataStore preferences，可被备份/root 读取。
- 建议：迁移到 Android Keystore 加密（EncryptedSharedPreferences 已废弃，用 Tink/Keystore 自实现）。属于数据迁移，本轮不改。

### 16. 流式状态每 token 全量更新
- 文件：`feature/ai/chat/AiAssistantChatViewModel.kt`（emitStreaming）
- 问题：每个 Content 事件重建 StreamingState（全文 StringBuilder→String），UI 端 MarkdownBlock 每 token 重解析。
- 建议：节流（如 50-100ms）或增量更新；MarkdownBlock 侧按变更合并。

### 17. 提供商配置页每键一次 DataStore 写盘
- 文件：`feature/ai/AiInterSubScreens.kt`（AiProviderConfigRoute）
- 问题：OutlinedTextField onValueChange 直接调 `updateConfig(...)` → 每次按键一次 DataStore edit。
- 建议：本地 state + 失焦/按钮保存。

### 18. detekt 已知项（非新增）
- `AiInterApiClient`：`catch (Exception)` 吞异常（onEvent/onFailure 解析兜底，已有注释说明）；`throw Exception(...)`（TooGenericExceptionThrown）— 建议引入带 message 的专用异常，保持 UI 文案友好。
- `CssStyleParser.kt`：`parseColor`/`parseFontSize` 等 when 分支多导致 ComplexCondition — 属解析器固有复杂度，不建议强行拆。
- `MarkdownBlock.kt:48`：`catch { it.printStackTrace() }` — 建议接入统一日志。
- `Highlighter.kt:83`：`onFailure { it.printStackTrace() }` — 同上。

### 19. 并发与线程模型（现状确认）
- 所有 `_chatMessages/_streamingState/_isSending` 均在 viewModelScope（Main dispatcher）修改；流收集也在 Main，无跨线程 MutableStateFlow 竞争。
- `Highlighter` 的 QuickJS 全部经单线程 executor 串行，无并发访问（修复后销毁也排队）。

---

## 修改文件清单

| 文件 | 修改 |
|------|------|
| `core/ai/network/AiInterApiClient.kt` | SSE 整块优先解析 + 按行回退（P1-7） |
| `core/ai/toolcall/AiChatToolHelper.kt` | tool_call id 统一兜底写回 buffer（P0-5） |
| `feature/ai/viewmodel/AiInterViewModel.kt` | stopStreaming 去重、clearRequested + job 守卫、addLog runCatching（P0-1/2） |
| `feature/ai/chat/AiAssistantChatViewModel.kt` | persistUserMessage、regenerate 去重、finally runCatching（P0-3/4）；clearRequested + job 守卫（P2-15）；modelsError+Log.e（P2-13）；init/newConversation 复用空会话（P2-14） |
| `feature/ai/chat/AiAssistantChatRoute.kt` | 模型 sheet 展示 modelsError（P2-13） |
| `feature/ai/chat/markdown/HtmlInlineRenderer.kt` | 链接/图片协议白名单（P1-9） |
| `feature/ai/chat/markdown/HtmlBlockRenderer.kt` | 图片协议白名单（P1-9） |
| `feature/ai/chat/highlight/Highlighter.kt` | 删除 init 预热、destroy 排队+shutdown、isActive 守卫（P0-6 / P1-8） |
| `feature/ai/chat/markdown/HighlightCodeBlock.kt` | DisposableEffect destroy（P1-8） |
