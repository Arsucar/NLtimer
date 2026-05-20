# AI 对话输入框背景统一与全屏按钮

## Goal

修复 ChatInput 背景色与父容器割裂问题，并添加输入框右侧全屏展开按钮（对标 rikkahub）。

## Requirements

### B5. 输入框背景色与父容器不一致导致割裂

- 文件: `app/.../ai_inter/chat/components/ChatInput.kt`
- 根因: 上次 B4 修复将 TextField containerColor 设为 `surfaceContainerHigh.copy(alpha = 0.6f)`，但外层 Surface 用 `Color.Transparent` + hazeEffect 毛玻璃，两者视觉上不融合
- 用户要求: 不想要边框，也不想要不一样的背景色
- rikkahub 方案:
  - 外层 Surface: `Color.Transparent` + `hazeEffect(style = HazeMaterials.ultraThin(containerColor = hazeTintColor))`
  - 内层 TextField: containerColor 同样用 `surfaceContainerHigh.copy(alpha = 0.6f)`，但因为外层 hazeEffect 使用 `ultraThin`（而非当前项目的 `ultraThin(MaterialTheme.colorScheme.surfaceContainerLow)`），视觉效果更统一
- 修复方向（方案 A 确认）: TextField containerColor 全部改回 `Color.Transparent`，让外层 hazeEffect 统一处理背景。需验证边框问题不复现（上次 B4 的边框闪现可能由 containerColor 非透明引起，全透明反而可能解决）。

### F1. 输入框右侧添加全屏展开按钮

- 文件: `app/.../ai_inter/chat/components/ChatInput.kt`
- 参考: rikkahub `ChatInput.kt:784-792`
- 实现:
  - 在 TextField 的 `trailingIcon` 位置添加全屏按钮
  - 当输入框获得焦点（`isFocused`）时显示全屏图标
  - 点击切换全屏编辑模式（FullScreenEditor dialog）
  - 全屏编辑器: 一个 Dialog/FullScreen 弹窗，包含大文本编辑区，点击返回/完成关闭
- 需要新增状态: `isFocused`, `isFullScreen`
- 需要新增组件: `FullScreenEditor` composable（简单的大文本编辑 Dialog）

## Acceptance Criteria

- [ ] B5: 输入框背景与外层容器视觉一致，无边框、无背景色差异、无割裂感
- [ ] F1: 输入框获得焦点时，右侧出现全屏展开按钮
- [ ] F1: 点击全屏按钮弹出全屏编辑器
- [ ] F1: 全屏编辑器可正常编辑文本，关闭后内容同步回输入框

## Definition of Done

- 编译通过 (`./gradlew :app:compileDebugKotlin --no-daemon`)
- Detekt 无新增告警

## Out of Scope

- 不修改 rikkahub 参考文件
- 不涉及 AI 对话的其他功能

## Technical Notes

### rikkahub 关键实现片段

外层容器 (line 425-438):
```kotlin
Surface(
    modifier = Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.largeIncreased)
        .then(
            if (settings.displaySetting.enableBlurEffect) Modifier.hazeEffect(
                state = hazeState,
                style = HazeMaterials.ultraThin(containerColor = hazeTintColor)
            ) else Modifier
        ),
    shape = MaterialTheme.shapes.largeIncreased,
    tonalElevation = 0.dp,
    color = if (settings.displaySetting.enableBlurEffect) Color.Transparent else hazeTintColor,
)
```

TextField colors (line 778-783):
```kotlin
colors = TextFieldDefaults.colors().copy(
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
),
```

全屏按钮 (line 784-792):
```kotlin
trailingIcon = {
    if (isFocused) {
        IconButton(onClick = { isFullScreen = !isFullScreen }) {
            Icon(HugeIcons.FullScreen, null)
        }
    }
},
```

全屏编辑器 (line 800-804):
```kotlin
if (isFullScreen) {
    FullScreenEditor(state = state) { isFullScreen = false }
}
```

### 关键文件

| 文件 | 用途 |
|------|------|
| `app/.../ai_inter/chat/components/ChatInput.kt` | 主修改文件 |
| `docs/reference/rikkahub/.../ChatInput.kt` | 参考实现 |
