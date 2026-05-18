# 活动/标签管理页面显示配置与图标渲染设计文档

## 背景

当前活动管理页面和标签管理页面使用 `CategoryGroupCard` 展示项目，但存在以下问题：

1. 活动管理页面未显示活动图标（`iconKey` 已存在但未被渲染）
2. 标签/活动 Chip 的颜色显示方式不可配置
3. `DialogGridConfig` 中混入了标签样式配置（`activityUseColorForText`、`tagUseColorForText`），命名上带有 "Dialog" 语义，却被主页面管理页面需要复用

## 目标

1. 活动管理页面显示活动图标，并支持图标颜色配置
2. 活动/标签管理页面支持通过拖拽菜单配置显示颜色模式
3. 将标签样式配置从 `DialogGridConfig` 中提取为独立的 `DisplayColorConfig`
4. 实现主页面与弹窗配置的联动

## 方案概述

采用 **方案 B：提取独立配置模块**。新建 `DisplayColorConfig` 数据类，包含 `activityIconColorMode` 和 `tagDisplayColorMode` 两个字段。`DialogGridConfig` 移除原有的布尔字段，改为引用 `DisplayColorConfig`。

## 数据模型

### DisplayColorMode 枚举

位于 `core/designsystem/theme/DisplayColorMode.kt`：

```kotlin
enum class DisplayColorMode {
    BACKGROUND, // 显示颜色为背景颜色（图标使用字段颜色）
    TEXT,       // 显示颜色为文字颜色（图标使用强调色）
    NORMAL,     // 显示为正常颜色（图标正常黑白）
}
```

### DisplayColorConfig 数据类

位于 `core/data/model/DisplayColorConfig.kt`：

```kotlin
@Immutable
data class DisplayColorConfig(
    val activityIconColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
    val tagDisplayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
)
```

### DialogGridConfig 修改

移除 `activityUseColorForText` 和 `tagUseColorForText`，新增 `displayColorConfig` 字段：

```kotlin
@Immutable
data class DialogGridConfig(
    val activityDisplayMode: ChipDisplayMode = ChipDisplayMode.Filled,
    val activityLayoutMode: GridLayoutMode = GridLayoutMode.Horizontal,
    val activityColumnLines: Int = 2,
    val activityHorizontalLines: Int = 2,
    val tagDisplayMode: ChipDisplayMode = ChipDisplayMode.Filled,
    val tagLayoutMode: GridLayoutMode = GridLayoutMode.Horizontal,
    val tagColumnLines: Int = 2,
    val tagHorizontalLines: Int = 2,
    val showBehaviorNature: Boolean = true,
    val pathDrawMode: PathDrawMode = PathDrawMode.StartToEnd,
    val secondsStrategy: SecondsStrategy = SecondsStrategy.OPEN_TIME,
    val autoMatchNote: Boolean = false,
    val displayColorConfig: DisplayColorConfig = DisplayColorConfig(),
)
```

## 配置持久化

### SettingsPrefs 接口新增

```kotlin
/** 以 Flow 形式监听显示颜色配置 */
fun getDisplayColorConfigFlow(): Flow<DisplayColorConfig>
/** 更新显示颜色配置并持久化 */
suspend fun updateDisplayColorConfig(config: DisplayColorConfig)
```

### SettingsPrefsImpl 实现

新增 DataStore key：

```kotlin
private val activityIconColorModeKey = stringPreferencesKey("activity_icon_color_mode")
private val tagDisplayColorModeKey = stringPreferencesKey("tag_display_color_mode")
```

实现读写逻辑，并将 `getDialogConfigFlow` / `updateDialogConfig` 修改为使用 `DisplayColorConfig`。

### 兼容性处理

- 旧版 `actUseColorKey` 和 `tagUseColorKey` 不再写入新数据
- 读取 `DialogGridConfig` 时，`displayColorConfig` 从新的独立 key 读取
- 旧 key 保留在 DataStore 中，不影响功能

## UI 组件修改

### CategoryGroupCard

新增 `displayColorMode` 参数：

```kotlin
@Composable
fun <T : CategorizableItem> CategoryGroupCard(
    // ... 现有参数
    displayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
)
```

### ItemChip 渲染逻辑

| 模式 | Chip 背景 | Chip 文字颜色 | 图标 Tint |
|------|----------|--------------|----------|
| `BACKGROUND` | `item.color` 或默认表面色 | 默认内容色 | `item.color`（字段颜色） |
| `TEXT` | 默认表面色 | `item.color` 或默认内容色 | 主题强调色（primary） |
| `NORMAL` | 默认表面色 | 默认内容色 | 默认（不设置 tint） |

> 注：`item.color` 通过 `CategorizableItem` 接口获取。`ActivityCategorizable` 返回 `activity.color`，`TagCategorizable` 返回 `tag.color`。

### CategorizableItem 接口扩展

新增 `color` 字段：

```kotlin
interface CategorizableItem {
    val itemId: Long
    val itemName: String
    val category: String?
    val usageCount: Int
    val lastUsedTimestamp: Long?
    val iconKey: String?
    val color: Long?  // 新增
}
```

各实现类相应更新。

## 页面修改

### ActivityManagementScreen

1. `ManagementActivityItem` 暴露 `color` 字段
2. 从 ViewModel 获取 `DisplayColorConfig`，将 `activityIconColorMode` 传递给 `CategoryGroupCard`
3. `BottomBarDragFab` 的 `dragOptions` 新增配置选项：
   - "图标：背景色模式"
   - "图标：文字色模式"
   - "图标：正常模式"
4. 选择后调用 `viewModel.updateActivityIconColorMode(mode)`

### TagManagementScreen

1. `ManagementTagItem` 暴露 `color` 字段（已有 `tag.color`）
2. 从 ViewModel 获取 `DisplayColorConfig`，将 `tagDisplayColorMode` 传递给 `CategoryGroupCard`
3. `BottomBarDragFab` 的 `dragOptions` 新增配置选项：
   - "标签：背景色模式"
   - "标签：文字色模式"
   - "标签：正常模式"
4. 选择后调用 `viewModel.updateTagDisplayColorMode(mode)`

## ViewModel 修改

### ActivityManagementViewModel

- 注入 `SettingsPrefs`
- 收集 `DisplayColorConfig` Flow，更新到 `ActivityManagementUiState`
- 新增 `updateActivityIconColorMode(mode: DisplayColorMode)` 方法

### TagManagementViewModel

- 已注入 `SettingsPrefs`，复用即可
- 收集 `DisplayColorConfig` Flow，更新到 `TagManagementUiState`
- 新增 `updateTagDisplayColorMode(mode: DisplayColorMode)` 方法

## 配置联动机制

弹窗中的标签配置（`DialogGridConfig.displayColorConfig`）和管理页面的配置共享同一个 `DisplayColorConfig` 数据源：

1. `DialogGridConfig` 中的 `displayColorConfig` 从 `SettingsPrefs.getDisplayColorConfigFlow()` 读取
2. 管理页面修改配置时，调用 `SettingsPrefs.updateDisplayColorConfig()`
3. DataStore 更新后，所有订阅者（包括弹窗）自动收到新配置

## 单选逻辑实现

拖拽菜单中的三个选项为互斥单选。实现方式：

- 菜单选项显示时，根据当前 `DisplayColorMode` 高亮选中项
- 用户选择某一项后，ViewModel 更新配置为对应模式
- 三个选项同时只能有一个处于激活状态

## 错误处理与边界条件

1. **DataStore 读取失败**：使用默认值 `DisplayColorConfig()`
2. **非法枚举值**：使用 `try-catch` 回退到 `NORMAL`
3. **item.color 为 null**：回退到默认主题色
4. **配置更新并发**：DataStore 编辑操作天然线程安全

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `core/designsystem/theme/DisplayColorMode.kt` | 新增 | 显示颜色模式枚举 |
| `core/data/model/DisplayColorConfig.kt` | 新增 | 显示颜色配置数据类 |
| `core/data/model/DialogGridConfig.kt` | 修改 | 移除旧字段，引用 DisplayColorConfig |
| `core/data/SettingsPrefs.kt` | 修改 | 新增 DisplayColorConfig 读写接口 |
| `core/data/SettingsPrefsImpl.kt` | 修改 | 实现 DisplayColorConfig 持久化 |
| `core/behaviorui/sheet/CategoryPickerModels.kt` | 修改 | CategorizableItem 新增 color 字段 |
| `core/behaviorui/sheet/CategoryGroupCard.kt` | 修改 | 支持 displayColorMode 参数 |
| `feature/management_activities/model/ActivityManagementUiState.kt` | 修改 | 新增 displayColorConfig 字段 |
| `feature/management_activities/viewmodel/ActivityManagementViewModel.kt` | 修改 | 收集配置，提供更新方法 |
| `feature/management_activities/ui/ActivityManagementScreen.kt` | 修改 | 传递配置，添加拖拽菜单选项 |
| `feature/tag_management/model/TagManagementUiState.kt` | 修改 | 新增 displayColorConfig 字段 |
| `feature/tag_management/viewmodel/TagManagementViewModel.kt` | 修改 | 收集配置，提供更新方法 |
| `feature/tag_management/ui/TagManagementScreen.kt` | 修改 | 传递配置，添加拖拽菜单选项 |

## 测试要点

1. `SettingsPrefsImpl` 正确读写 `DisplayColorConfig`
2. `DialogGridConfig` 序列化/反序列化兼容
3. `CategoryGroupCard` 在不同 `DisplayColorMode` 下正确渲染
4. ViewModel 配置更新后 UI 自动刷新
5. 单选逻辑正确（同时只能选中一个选项）
