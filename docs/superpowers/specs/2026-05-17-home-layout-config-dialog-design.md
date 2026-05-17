# 主页布局配置弹窗化设计

- **日期**: 2026-05-17
- **关联 Issue**: #2 重构主页布局配置：各选项独立为弹窗并支持半透明即时预览
- **作者**: Arsucar
- **状态**: 已批准（待实现）

## 1. 背景

当前所有主页布局参数集中在 `feature/settings/.../HomeLayoutConfigScreen.kt`，用户必须离开主页进入设置页才能调整布局参数，且无法在调整时即时观察主页效果。

`HomeLayoutConfig` 已经通过 `DialogConfigViewModel.homeLayoutConfig` 以 Flow 注入主页，**数据通路已天然支持即时预览**，缺的只是「主页内可触发的配置入口」。

## 2. 目标

1. 在主页内提供按当前布局上下文打开的配置弹窗（4 个布局各一个）
2. 弹窗背景半透明，调整数值时底层布局立即重绘
3. 配置变更即时生效并持久化（与现有设置页同源同步）
4. 不引入新的导航/页面切换（遵循"主页拖拽是产品特色、不做上下文切换"的产品约束）

## 3. 非目标

- 不为每个字段单独拆弹窗（颗粒度按布局划分）
- 不增加确认/取消按钮（即时生效）
- 不删除 `feature/settings` 中的集中设置页（保留作为完整编辑入口）
- 不改动 `HomeLayoutConfig` 数据结构或现有 DragFab 的「完成/目标/当前/特记」选项

## 4. 用户流程

1. 用户在主页处于任一布局（GRID / TIMELINE_REVERSE / LOG / MOMENT）
2. 拖拽右下角 `BottomBarDragFab`，弹出选项菜单
3. 菜单末尾出现一个**绑定当前布局**的配置项（例如 GRID 时为「网格设置」，LOG 时为「日志设置」）
4. 选中后，居中弹出半透明配置面板，背后主页布局可见
5. 用户用 `±` 步进器或点击数字直接输入修改数值，每次变动都立刻反映到背后的主页上
6. 点击「恢复默认」可重置当前布局的默认值；点击关闭按钮或弹窗外区域关闭弹窗

## 5. 架构

### 5.1 数据流

```
DialogConfigViewModel.homeLayoutConfig (Flow)
        │
        ▼
HomeRoute → HomeScreen(homeLayoutConfig, onHomeLayoutConfigChange)
        │
        ├──→ HomeLayoutContent (实时渲染)
        │
        └──→ LayoutConfigDialog(layout, config, onConfigChange = onHomeLayoutConfigChange, onDismiss)
```

弹窗回调直接走现有 `onHomeLayoutConfigChange`，无需新管线。

### 5.2 文件变更

| 文件 | 操作 | 说明 |
|---|---|---|
| `core/designsystem/component/ConfigStepper.kt` | 新增 | 抽出现有 `ConfigStepper` 公共组件 |
| `feature/home/.../ui/components/LayoutConfigDialog.kt` | 新增 | 半透明居中配置弹窗 |
| `feature/home/.../ui/HomeScreen.kt` | 修改 | DragFab 选项追加 + 弹窗状态与渲染 |
| `feature/settings/.../HomeLayoutConfigScreen.kt` | 修改 | 移除内部私有 `ConfigStepper`，引用 designsystem 版本 |

## 6. 组件设计

### 6.1 `ConfigStepper`（公共组件）

从 `HomeLayoutConfigScreen.kt:257-358` 原样抽出到 `core/designsystem/component/ConfigStepper.kt`。

签名保持不变：

```kotlin
@Composable
fun ConfigStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int = 1,
    suffix: String = "",
    onValueChange: (Int) -> Unit,
)
```

### 6.2 `LayoutConfigDialog`

```kotlin
@Composable
fun LayoutConfigDialog(
    layout: HomeLayout,
    config: HomeLayoutConfig,
    onConfigChange: (HomeLayoutConfig) -> Unit,
    onDismiss: () -> Unit,
)
```

**实现要点：**
- 用 `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`
- 通过 `LocalView.current.parent as DialogWindowProvider` 拿到 Window，调用 `clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)` 关闭默认 scrim
- 外层 `Box(Modifier.fillMaxSize(), contentAlignment = Center)` + 点击空白关闭
- 面板 `Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f), tonalElevation = 6.dp)`，宽度约 340dp，最大高度 540dp
- 顶部：标题 +「×」关闭按钮
- 中部：`LazyColumn` 渲染对应布局的配置项
- 底部：`TextButton(onClick = { onConfigChange(config.copyResetting(layout)) })` 显示"恢复默认"

**内部分支（私有 Composable）：**
- `GridConfigSection(grid, onChange)`
- `LogConfigSection(log, onChange)`
- `TimelineConfigSection(timeline, onChange)`
- `MomentConfigSection(moment, onChange)`

每个 Section 直接搬运 `HomeLayoutConfigScreen` 中对应布局的字段列表（列数、行高、间距等），共享 `ConfigStepper`。

### 6.3 `HomeScreen.kt` 修改

新增本地状态：

```kotlin
var configDialogLayout: HomeLayout? by remember { mutableStateOf(null) }
```

DragFab 选项构造改为：

```kotlin
val layoutSettingsLabel = when (layout) {
    HomeLayout.GRID -> "网格设置"
    HomeLayout.TIMELINE_REVERSE -> "时间轴设置"
    HomeLayout.LOG -> "日志设置"
    HomeLayout.MOMENT -> "当前时刻设置"
}
val baseOptions = if (uiState.hasActiveBehavior) DragOptionsWithActive else DragOptionsWithoutActive
val dragOptions = baseOptions + layoutSettingsLabel
```

`onOptionSelected` 分支追加：

```kotlin
layoutSettingsLabel -> configDialogLayout = layout
```

在 `Box` 末尾渲染：

```kotlin
configDialogLayout?.let { l ->
    LayoutConfigDialog(
        layout = l,
        config = homeLayoutConfig,
        onConfigChange = onHomeLayoutConfigChange,
        onDismiss = { configDialogLayout = null },
    )
}
```

## 7. 测试策略

- 单元/UI 测试：
  - 新增 `LayoutConfigDialogTest`：4 种布局下的标题正确、修改 stepper 触发 `onConfigChange` 且 patch 到正确字段、"恢复默认"将对应子配置重置为默认实例
  - `HomeScreen` 现有测试不破坏；如已有 DragFab 选项测试，补一条「当前布局对应的设置选项存在于 options 列表」
- 手测：
  - 4 个布局依次切换 → 拖拽 FAB → 选「<布局名>设置」→ 弹窗居中半透明 → 滑动 stepper → 主页背景同步变化
  - 设置页打开 → 调整某字段 → 返回主页 → 主页效果一致；反之亦然
  - 旋转 / 深浅色主题 / 不同 DPI 验证弹窗布局未破

## 8. 风险与缓解

| 风险 | 缓解 |
|---|---|
| `FLAG_DIM_BEHIND` 兼容性差 | 兜底：反射失败时退化为默认 scrim（功能不受影响，只是背景暗一些） |
| DragFab 选项过多影响可读 | 当前 4 + 新增 1 = 5 项，符合 BottomBarDragFab 设计上限；如未来超限再做分组 |
| `ConfigStepper` 抽出后 import 漏改 | 编译会立即失败，回归风险低 |

## 9. 验收标准

- [ ] 4 个布局下，拖拽菜单中能且只能看到当前布局对应的「<布局名>设置」选项
- [ ] 选中后弹出半透明居中弹窗，背景主页可见
- [ ] 修改任一字段 → 主页立即更新且持久化
- [ ] 「恢复默认」仅重置当前布局的字段，其他布局配置不受影响
- [ ] 关闭弹窗 → 主页恢复正常交互
- [ ] `feature/settings` 集中设置页仍可正常打开并与主页配置同步
