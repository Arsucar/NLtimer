# 顶部栏 Haze 模糊背景

## Goal

给主页顶部栏（AppTopAppBar / AppCollapsedTopAppBar）和 AI 对话顶部栏（ChatTopBar）添加 Haze 模糊背景效果，实现毛玻璃风格的沉浸式体验。

## Requirements

### R1: 主页顶部栏模糊

- 在 `NLtimerScaffold` 中创建 `HazeState`，将 `NLtimerNavHost` 的内容标记为 `hazeSource`
- 在 `AppTopAppBar` 和 `AppCollapsedTopAppBar` 上应用 `hazeEffect`，使用 `HazeMaterials.ultraThin` 风格
- 模糊效果在沉浸式和非沉浸式模式下均生效（替代当前的纯透明/纯色背景）
- 折叠模式下滚动时模糊效果动态跟随

### R2: AI 对话顶部栏模糊

- 复用 `AiAssistantChatRoute` 中已有的 `hazeState`（ChatList 已是 `hazeSource`）
- 在 `ChatTopBar` 上应用 `hazeEffect`，使用相同的 `HazeMaterials.ultraThin` 风格

## What I already know

- 项目使用 Haze 1.6.0，API 为 `hazeSource`（源）+ `hazeEffect`（模糊效果）
- AI 聊天页面已有 Haze 架构：`ChatList`（hazeSource）→ `ChatInput`（hazeEffect）
- `HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow)` 是当前使用的风格
- 顶部栏当前无模糊，沉浸式时用 `Color.Transparent`，非沉浸式用 `MaterialTheme.colorScheme.background`

## Technical Approach

### R1 实现

1. `NLtimerScaffold` 中创建 `val hazeState = rememberHazeState()`
2. 在 `NLtimerNavHost` 的 modifier 上加 `.hazeSource(state = hazeState)`
3. `AppTopAppBar` / `AppCollapsedTopAppBar` 新增 `hazeState` 参数
4. 在 TopAppBar 的 modifier 上加 `.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(...))`
5. TopAppBar 的 `containerColor` 和 `scrolledContainerColor` 都设为 `Color.Transparent`（让模糊透出）

### R2 实现

1. `ChatTopBar` 新增 `hazeState: HazeState` 参数
2. 在 TopAppBar 的 modifier 上加 `.hazeEffect`
3. `AiAssistantChatRoute` 传入已有的 `hazeState`

## Acceptance Criteria

- [ ] 主页顶部栏显示毛玻璃模糊背景效果
- [ ] AI 对话顶部栏显示毛玻璃模糊背景效果
- [ ] 滚动时模糊动态跟随内容
- [ ] 现有交互逻辑不受影响
- [ ] 编译通过

## Out of Scope

- 不改变底部栏的模糊效果
- 不改变 Haze 依赖版本
- 不涉及其他页面的模糊效果

## Technical Notes

- 关键文件：
  - `app/NLtimerScaffold.kt` — 创建 hazeState，传给 topBar 和 navHost
  - `app/component/AppTopAppBar.kt` — AppTopAppBar / AppCollapsedTopAppBar 加 hazeEffect
  - `app/.../chat/components/ChatTopBar.kt` — 加 hazeEffect
  - `app/.../chat/AiAssistantChatRoute.kt` — 传 hazeState 给 ChatTopBar
