# 顶部栏内容区域圆角模糊容器

## Goal

将顶部栏的全宽 hazeEffect 改为只在有内容的区域（标题、按钮组）应用圆角(pill)模糊容器，提升视觉精致度。

## Requirements

### R1: 主页顶部栏 (AppTopAppBar / AppCollapsedTopAppBar)

- 移除整个 TopAppBar 上的 hazeEffect
- 给 title 区域包裹一个 pill 形状（RoundedCornerShape(50%)）的容器，容器上应用 hazeEffect
- 给 actions 区域包裹一个 pill 形状的容器，容器上应用 hazeEffect
- TopAppBar 的 containerColor/scrolledContainerColor 保持 Color.Transparent
- navigationIcon 区域不加模糊容器（主页为空，AI Inter 为返回按钮）

### R2: AI 对话顶部栏 (ChatTopBar)

- 同样移除整个 TopAppBar 上的 hazeEffect
- 标题区域（title）加 pill + hazeEffect
- 操作按钮区域（actions：导出、新建、更多）加 pill + hazeEffect
- 抽屉菜单按钮（navigationIcon）不加模糊容器

### R3: 模糊风格

- 统一使用 `HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow)`
- pill 容器用 `RoundedCornerShape(50.percent)` 实现完全圆角

## Technical Approach

### 实现

1. TopAppBar 本身不加 hazeEffect，保持透明
2. 在 TopAppBar 的 `title` slot 内部，用 `Box(modifier = Modifier.hazeEffect(...).clip(RoundedCornerShape(50.percent)).padding(horizontal, vertical))` 包裹标题内容
3. 在 TopAppBar 的 `actions` slot 内部，用同样的 `Box` 包裹所有按钮的 `Row`
4. hazeEffect 需要 clip 才能裁剪为圆角形状——注意 `hazeEffect` + `clip` 的顺序

### 关键文件
- `app/component/AppTopAppBar.kt` — AppTopAppBar + AppCollapsedTopAppBar
- `app/.../chat/components/ChatTopBar.kt`
- `app/NLtimerScaffold.kt` — hazeState 传递不变

## Acceptance Criteria

- [ ] 主页标题区域有圆角模糊背景 pill
- [ ] 主页操作按钮区域有圆角模糊背景 pill
- [ ] AI 对话标题区域有圆角模糊背景 pill
- [ ] AI 对话操作按钮区域有圆角模糊背景 pill
- [ ] 顶部栏其余区域（空白处）无模糊，完全透明
- [ ] 编译通过

## Out of Scope

- 不改变底部栏
- 不改变 Haze 版本
- navigationIcon 不加模糊容器
