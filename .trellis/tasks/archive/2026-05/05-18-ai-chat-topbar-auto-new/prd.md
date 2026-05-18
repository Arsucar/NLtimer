# AI助手对话界面顶部栏复用与自动新对话

## Goal

将 AI 助手对话界面的顶部栏改为复用主页的自动折叠效果（enterAlwaysScrollBehavior），并在每次进入对话界面时自动创建新对话而非复用旧对话。

## Requirements

### R1: 顶部栏折叠效果复用

- `ChatTopBar` 改为支持 `TopAppBarScrollBehavior`，使用 Material 3 的 `enterAlwaysScrollBehavior()`
- 滚动时顶部栏折叠，向上滚动时立即展开——与主页 `AppCollapsedTopAppBar` 行为一致
- 保持现有的 `ChatTopBar` 文字（标题+模型名）、按钮样式（抽屉菜单、导出、新建、溢出菜单）及所有逻辑不变
- 在 `AiAssistantChatRoute` 中创建 scroll behavior 并通过 `nestedScroll` 连接到 Scaffold

### R2: 进入时自动新建对话

- ViewModel init 改为每次都创建新对话，而不是加载最近一条旧对话
- 用户仍可通过抽屉侧栏切换到历史对话

## What I already know

- 主页折叠顶部栏使用 `TopAppBarDefaults.enterAlwaysScrollBehavior()` + `nestedScroll` 修饰符
- 折叠由 `NLtimerScaffold` 控制，`useCollapsed` 条件排除 `isAiAssistantChat`
- `AiAssistantChatRoute` 拥有独立 Scaffold + `ChatTopBar`（当前是固定高度 `TopAppBar`，无 scroll behavior）
- ViewModel init 逻辑：有旧对话就选最新的，没有就 `newConversation()`
- `ChatTopBar` 70 行，结构简单：navigationIcon + title + actions

## Technical Approach

### R1 实现

1. 修改 `ChatTopBar` 签名，增加 `scrollBehavior: TopAppBarScrollBehavior?` 参数
2. 将内部 `TopAppBar` 的 `scrollBehavior` 传入该参数
3. 在 `AiAssistantChatRoute` 中：
   - 创建 `val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()`
   - 将 `topBarScrollBehavior` 传给 `ChatTopBar`
   - 在 Scaffold 的 modifier 上加 `.nestedScroll(topBarScrollBehavior.nestedScrollConnection)`

### R2 实现

1. 修改 `AiAssistantChatViewModel.init`：去掉加载旧对话逻辑，直接调用 `newConversation()`

## Acceptance Criteria

- [ ] AI 对话界面滚动时顶部栏可折叠/展开，效果与主页一致
- [ ] 顶部栏文字、按钮、交互逻辑不变（标题点击重命名、抽屉、导出、新建、清空）
- [ ] 每次进入 AI 对话界面自动创建新对话
- [ ] 抽屉侧栏仍可切换到历史对话
- [ ] 编译通过，无 lint 错误

## Out of Scope

- 不改变 `NLtimerScaffold` 对 chat 页面的处理逻辑（chat 仍用独立 Scaffold）
- 不改变对话持久化或数据库结构
- 不涉及主题设置中 `TopBarMode` 的联动（chat 页始终使用折叠模式，不读配置）

## Technical Notes

- 关键文件：
  - `app/.../ai_inter/chat/components/ChatTopBar.kt` — 需改
  - `app/.../ai_inter/chat/AiAssistantChatRoute.kt` — 需改
  - `app/.../ai_inter/chat/AiAssistantChatViewModel.kt` — 需改 init
  - `app/component/AppTopAppBar.kt` — 参考 `AppCollapsedTopAppBar` 的 scroll behavior 用法
  - `app/NLtimerScaffold.kt` — 参考折叠条件逻辑
