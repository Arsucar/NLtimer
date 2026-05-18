# 主题配置顶栏模糊开关

## Goal

在主题配置中添加「顶栏模糊」开关，让用户可以控制顶部栏的 Haze 模糊效果。

## Requirements

### R1: Theme 数据类新增字段

- `Theme` data class 新增 `val topBarHaze: Boolean = true`
- `SettingsPrefsImpl` 添加 DataStore key `top_bar_haze`，读写该字段

### R2: ViewModel 新增方法

- `ThemeSettingsViewModel` 新增 `onTopBarHazeToggle(enabled: Boolean)`

### R3: 主题配置界面

- `ThemeSettingsScreen` 中添加开关 UI（ListItem + Switch）
- 放在现有顶部栏相关设置附近

### R4: 消费端条件判断

- `NLtimerScaffold` 中根据 `LocalTheme.current.topBarHaze` 决定是否传递 `hazeState`
- `AiAssistantChatRoute` 中同样根据配置决定是否传递 `hazeState`

## Technical Approach

按照 `isImmersive` 的现有模式：
1. `Theme` 加字段
2. `SettingsPrefsImpl` 加 key + 读写
3. `ThemeSettingsViewModel` 加方法
4. `ThemeSettingsScreen` 加 UI
5. 消费端用 `theme.topBarHaze` 条件判断

## Acceptance Criteria

- [ ] 主题配置页面有「顶栏模糊」开关
- [ ] 关闭后主页顶部栏无模糊效果
- [ ] 关闭后 AI 对话顶部栏无模糊效果
- [ ] 开启后恢复正常模糊
- [ ] 设置持久化，重启后保留

## Out of Scope

- 不改变模糊样式或强度
- 不涉及底部栏

## Technical Notes

- 关键文件：
  - `core/designsystem/.../theme/ThemeConfig.kt`
  - `core/data/.../SettingsPrefsImpl.kt`
  - `feature/settings/.../ThemeSettingsViewModel.kt`
  - `feature/settings/.../ThemeSettingsScreen.kt`
  - `app/NLtimerScaffold.kt`
  - `app/.../chat/AiAssistantChatRoute.kt`
