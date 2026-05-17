# 活动/标签管理页面显示配置与图标渲染实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现活动/标签管理页面的图标渲染、显示颜色配置选项，并提取独立的 DisplayColorConfig 配置模块。

**架构：** 新建 DisplayColorMode 枚举和 DisplayColorConfig 数据类，从 DialogGridConfig 中提取颜色配置。SettingsPrefs 新增独立读写接口。CategoryGroupCard 和 ItemChip 根据 DisplayColorMode 渲染不同样式。管理页面通过 BottomBarDragFab 拖拽菜单切换配置。

**技术栈：** Kotlin, Jetpack Compose, Hilt, DataStore Preferences, Kotlin Coroutines Flow

---

## 文件清单

| 文件 | 操作 | 职责 |
|------|------|------|
| `core/designsystem/theme/DisplayColorMode.kt` | 新增 | 显示颜色模式枚举（BACKGROUND/TEXT/NORMAL） |
| `core/data/model/DisplayColorConfig.kt` | 新增 | 显示颜色配置数据类 |
| `core/data/model/DialogGridConfig.kt` | 修改 | 移除 activityUseColorForText/tagUseColorForText，引用 DisplayColorConfig |
| `core/data/SettingsPrefs.kt` | 修改 | 新增 getDisplayColorConfigFlow / updateDisplayColorConfig 接口 |
| `core/data/SettingsPrefsImpl.kt` | 修改 | 实现 DisplayColorConfig DataStore 持久化 |
| `core/behaviorui/sheet/CategoryPickerModels.kt` | 修改 | CategorizableItem 新增 color 字段，各实现类更新 |
| `core/behaviorui/sheet/CategoryGroupCard.kt` | 修改 | 新增 displayColorMode 参数，ItemChip 根据模式渲染 |
| `feature/management_activities/model/ActivityManagementUiState.kt` | 修改 | 新增 displayColorConfig 字段 |
| `feature/management_activities/viewmodel/ActivityManagementViewModel.kt` | 修改 | 注入 SettingsPrefs，收集配置，提供更新方法 |
| `feature/management_activities/ui/ActivityManagementScreen.kt` | 修改 | 传递配置给 CategoryGroupCard，拖拽菜单新增配置选项 |
| `feature/tag_management/model/TagManagementUiState.kt` | 修改 | 新增 displayColorConfig 字段 |
| `feature/tag_management/viewmodel/TagManagementViewModel.kt` | 修改 | 收集配置，提供更新方法 |
| `feature/tag_management/ui/TagManagementScreen.kt` | 修改 | 传递配置给 CategoryGroupCard，拖拽菜单新增配置选项 |

---

### 任务 1：新建 DisplayColorMode 枚举

**文件：**
- 创建：`core/designsystem/theme/DisplayColorMode.kt`

- [ ] **步骤 1：创建枚举文件**

```kotlin
package com.nltimer.core.designsystem.theme

enum class DisplayColorMode {
    BACKGROUND,
    TEXT,
    NORMAL,
}
```

- [ ] **步骤 2：Commit**

```bash
git add core/designsystem/theme/DisplayColorMode.kt
git commit -m "feat(designsystem): 添加 DisplayColorMode 枚举"
```

---

### 任务 2：新建 DisplayColorConfig 数据类

**文件：**
- 创建：`core/data/model/DisplayColorConfig.kt`

- [ ] **步骤 1：创建数据类文件**

```kotlin
package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.designsystem.theme.DisplayColorMode

@Immutable
data class DisplayColorConfig(
    val activityIconColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
    val tagDisplayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,
)
```

- [ ] **步骤 2：Commit**

```bash
git add core/data/model/DisplayColorConfig.kt
git commit -m "feat(data): 添加 DisplayColorConfig 数据类"
```

---

### 任务 3：修改 DialogGridConfig 引用 DisplayColorConfig

**文件：**
- 修改：`core/data/model/DialogGridConfig.kt`

- [ ] **步骤 1：修改 DialogGridConfig**

移除 `activityUseColorForText` 和 `tagUseColorForText` 字段，新增 `displayColorConfig` 字段：

```kotlin
package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode

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

- [ ] **步骤 2：Commit**

```bash
git add core/data/model/DialogGridConfig.kt
git commit -m "refactor(data): DialogGridConfig 移除旧颜色字段，引用 DisplayColorConfig"
```

---

### 任务 4：扩展 CategorizableItem 接口新增 color 字段

**文件：**
- 修改：`core/behaviorui/sheet/CategoryPickerModels.kt`

- [ ] **步骤 1：修改接口和实现类**

在 `CategorizableItem` 接口中新增 `color: Long?` 字段，并更新所有实现类：

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

data class ActivityCategorizable(
    val activity: Activity,
    override val lastUsedTimestamp: Long? = null,
) : CategorizableItem {
    override val itemId: Long = activity.id
    override val itemName: String = activity.name
    override val category: String? = null
    override val usageCount: Int = activity.usageCount
    override val iconKey: String? = activity.iconKey
    override val color: Long? = activity.color  // 新增
}

data class TagCategorizable(
    val tag: Tag,
    override val lastUsedTimestamp: Long? = null,
) : CategorizableItem {
    override val itemId: Long = tag.id
    override val itemName: String = tag.name
    override val category: String? = tag.category
    override val usageCount: Int = tag.usageCount
    override val iconKey: String? = null
    override val color: Long? = tag.color  // 新增
}

data class ActivityGroupCategorizable(
    val group: ActivityGroup,
) : CategorizableItem {
    override val itemId: Long = group.id
    override val itemName: String = group.name
    override val category: String? = null
    override val usageCount: Int = 0
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = null
    override val color: Long? = null  // 新增
}

data class StringCategoryCategorizable(
    val name: String,
) : CategorizableItem {
    override val itemId: Long = name.hashCode().toLong()
    override val itemName: String = name
    override val category: String? = null
    override val usageCount: Int = 0
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = null
    override val color: Long? = null  // 新增
}
```

- [ ] **步骤 2：Commit**

```bash
git add core/behaviorui/sheet/CategoryPickerModels.kt
git commit -m "feat(behaviorui): CategorizableItem 新增 color 字段"
```

---

### 任务 5：SettingsPrefs 新增 DisplayColorConfig 接口

**文件：**
- 修改：`core/data/SettingsPrefs.kt`

- [ ] **步骤 1：修改接口**

```kotlin
package com.nltimer.core.data

import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.DisplayColorConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import kotlinx.coroutines.flow.Flow

interface SettingsPrefs {
    fun getThemeFlow(): Flow<Theme>
    suspend fun updateTheme(theme: Theme)

    fun getSavedTagCategories(): Flow<Set<String>>
    fun getSavedTagCategoriesOrder(): Flow<List<String>>
    suspend fun saveTagCategories(categories: Set<String>)
    suspend fun saveTagCategoriesOrder(categories: List<String>)

    fun getDialogConfigFlow(): Flow<DialogGridConfig>
    suspend fun updateDialogConfig(config: DialogGridConfig)

    fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig>
    suspend fun updateTimeLabelConfig(config: TimeLabelConfig)

    fun getHomeLayoutConfigFlow(): Flow<HomeLayoutConfig>
    suspend fun updateHomeLayoutConfig(config: HomeLayoutConfig)

    fun getHasSeenIntroFlow(): Flow<Boolean>
    suspend fun setHasSeenIntro(seen: Boolean)

    // 新增
    fun getDisplayColorConfigFlow(): Flow<DisplayColorConfig>
    suspend fun updateDisplayColorConfig(config: DisplayColorConfig)
}
```

- [ ] **步骤 2：Commit**

```bash
git add core/data/SettingsPrefs.kt
git commit -m "feat(data): SettingsPrefs 新增 DisplayColorConfig 读写接口"
```

---

### 任务 6：SettingsPrefsImpl 实现 DisplayColorConfig 持久化

**文件：**
- 修改：`core/data/SettingsPrefsImpl.kt`

- [ ] **步骤 1：新增 DataStore key 和读写逻辑**

在 companion object 中新增：

```kotlin
private val activityIconColorModeKey = stringPreferencesKey("activity_icon_color_mode")
private val tagDisplayColorModeKey = stringPreferencesKey("tag_display_color_mode")
```

新增方法实现：

```kotlin
override fun getDisplayColorConfigFlow(): Flow<DisplayColorConfig> = dataStore.data.map { prefs ->
    DisplayColorConfig(
        activityIconColorMode = try {
            DisplayColorMode.valueOf(prefs[activityIconColorModeKey] ?: DisplayColorMode.NORMAL.name)
        } catch (_: IllegalArgumentException) {
            DisplayColorMode.NORMAL
        },
        tagDisplayColorMode = try {
            DisplayColorMode.valueOf(prefs[tagDisplayColorModeKey] ?: DisplayColorMode.NORMAL.name)
        } catch (_: IllegalArgumentException) {
            DisplayColorMode.NORMAL
        },
    )
}

override suspend fun updateDisplayColorConfig(config: DisplayColorConfig) {
    dataStore.edit { prefs ->
        prefs[activityIconColorModeKey] = config.activityIconColorMode.name
        prefs[tagDisplayColorModeKey] = config.tagDisplayColorMode.name
    }
}
```

- [ ] **步骤 2：修改 getDialogConfigFlow 和 updateDialogConfig**

`getDialogConfigFlow` 中移除 `activityUseColorForText` 和 `tagUseColorForText` 的读取，改为从 `getDisplayColorConfigFlow()` 获取或独立读取：

```kotlin
override fun getDialogConfigFlow(): Flow<DialogGridConfig> = dataStore.data.map { prefs ->
    DialogGridConfig(
        activityDisplayMode = try { ChipDisplayMode.valueOf(prefs[actDisplayModeKey] ?: ChipDisplayMode.Filled.name) } catch (_: IllegalArgumentException) { ChipDisplayMode.Filled },
        activityLayoutMode = try { GridLayoutMode.valueOf(prefs[actLayoutModeKey] ?: GridLayoutMode.Horizontal.name) } catch (_: IllegalArgumentException) { GridLayoutMode.Horizontal },
        activityColumnLines = prefs[actColumnLinesKey] ?: 2,
        activityHorizontalLines = prefs[actHorizontalLinesKey] ?: 2,
        tagDisplayMode = try { ChipDisplayMode.valueOf(prefs[tagDisplayModeKey] ?: ChipDisplayMode.Filled.name) } catch (_: IllegalArgumentException) { ChipDisplayMode.Filled },
        tagLayoutMode = try { GridLayoutMode.valueOf(prefs[tagLayoutModeKey] ?: GridLayoutMode.Horizontal.name) } catch (_: IllegalArgumentException) { GridLayoutMode.Horizontal },
        tagColumnLines = prefs[tagColumnLinesKey] ?: 2,
        tagHorizontalLines = prefs[tagHorizontalLinesKey] ?: 2,
        showBehaviorNature = prefs[showNatureKey] ?: true,
        pathDrawMode = try { PathDrawMode.valueOf(prefs[pathDrawModeKey] ?: PathDrawMode.StartToEnd.name) } catch (_: IllegalArgumentException) { PathDrawMode.StartToEnd },
        secondsStrategy = try { SecondsStrategy.valueOf(prefs[secondsStrategyKey] ?: SecondsStrategy.OPEN_TIME.name) } catch (_: IllegalArgumentException) { SecondsStrategy.OPEN_TIME },
        autoMatchNote = prefs[autoMatchNoteKey] ?: false,
        displayColorConfig = DisplayColorConfig(
            activityIconColorMode = try {
                DisplayColorMode.valueOf(prefs[activityIconColorModeKey] ?: DisplayColorMode.NORMAL.name)
            } catch (_: IllegalArgumentException) {
                DisplayColorMode.NORMAL
            },
            tagDisplayColorMode = try {
                DisplayColorMode.valueOf(prefs[tagDisplayColorModeKey] ?: DisplayColorMode.NORMAL.name)
            } catch (_: IllegalArgumentException) {
                DisplayColorMode.NORMAL
            },
        ),
    )
}

override suspend fun updateDialogConfig(config: DialogGridConfig) {
    dataStore.edit { prefs ->
        prefs[actDisplayModeKey] = config.activityDisplayMode.name
        prefs[actLayoutModeKey] = config.activityLayoutMode.name
        prefs[actColumnLinesKey] = config.activityColumnLines
        prefs[actHorizontalLinesKey] = config.activityHorizontalLines
        prefs[tagDisplayModeKey] = config.tagDisplayMode.name
        prefs[tagLayoutModeKey] = config.tagLayoutMode.name
        prefs[tagColumnLinesKey] = config.tagColumnLines
        prefs[tagHorizontalLinesKey] = config.tagHorizontalLines
        prefs[showNatureKey] = config.showBehaviorNature
        prefs[pathDrawModeKey] = config.pathDrawMode.name
        prefs[secondsStrategyKey] = config.secondsStrategy.name
        prefs[autoMatchNoteKey] = config.autoMatchNote
        prefs[activityIconColorModeKey] = config.displayColorConfig.activityIconColorMode.name
        prefs[tagDisplayColorModeKey] = config.displayColorConfig.tagDisplayColorMode.name
    }
}
```

- [ ] **步骤 3：Commit**

```bash
git add core/data/SettingsPrefsImpl.kt
git commit -m "feat(data): SettingsPrefsImpl 实现 DisplayColorConfig 持久化"
```

---

### 任务 7：修改 CategoryGroupCard 支持 displayColorMode

**文件：**
- 修改：`core/behaviorui/sheet/CategoryGroupCard.kt`

- [ ] **步骤 1：新增参数并修改 ItemChip**

`CategoryGroupCard` 新增 `displayColorMode` 参数：

```kotlin
@Composable
fun <T : CategorizableItem> CategoryGroupCard(
    index: Int,
    groupName: String,
    items: List<T>,
    selectedId: Long? = null,
    selectedIds: Set<Long> = emptySet(),
    multiSelect: Boolean = false,
    onItemSelected: (Long) -> Unit = {},
    onItemsSelected: (Set<Long>) -> Unit = {},
    onItemLongClick: (Long) -> Unit = {},
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    shiftOffset: Float = 0f,
    collapsed: Boolean,
    onToggleCollapsed: (() -> Unit)? = null,
    showDragHandle: Boolean = true,
    emptyText: String = "暂无项目",
    showItemIcon: Boolean = true,
    showHeader: Boolean = true,
    headerActions: @Composable (() -> Unit)? = null,
    onAddItem: (() -> Unit)? = null,
    displayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,  // 新增
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onPositioned: (index: Int, y: Float, height: Float) -> Unit = { _, _, _ -> },
)
```

修改 `ItemChip` 调用处，传递 `displayColorMode`：

```kotlin
ItemChip(
    item = item,
    isSelected = isSelected,
    showIcon = showItemIcon && item is ActivityCategorizable,
    displayColorMode = displayColorMode,  // 新增
    onClick = { ... },
    onLongClick = { ... },
)
```

- [ ] **步骤 2：修改 ItemChip 函数**

```kotlin
@Composable
private fun <T : CategorizableItem> ItemChip(
    item: T,
    isSelected: Boolean,
    showIcon: Boolean = true,
    displayColorMode: DisplayColorMode = DisplayColorMode.NORMAL,  // 新增
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val itemColor = item.color?.let { Color(it) }
    val themePrimary = MaterialTheme.colorScheme.primary

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        displayColorMode == DisplayColorMode.BACKGROUND && itemColor != null -> itemColor.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        displayColorMode == DisplayColorMode.TEXT && itemColor != null -> itemColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val iconTint = when (displayColorMode) {
        DisplayColorMode.BACKGROUND -> itemColor ?: MaterialTheme.colorScheme.onSurface
        DisplayColorMode.TEXT -> themePrimary
        DisplayColorMode.NORMAL -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
    ) {
        if (showIcon) {
            ChipContent(
                text = item.itemName,
                textColor = contentColor,
                iconSlot = {
                    IconRenderer(
                        iconKey = item.iconKey,
                        defaultEmoji = "❓",
                        iconSize = 20.dp,
                        tint = iconTint,  // 新增
                    )
                },
            )
        } else {
            ChipContent(
                text = item.itemName,
                textColor = contentColor,
                iconSlot = null,
            )
        }
    }
}
```

- [ ] **步骤 3：Commit**

```bash
git add core/behaviorui/sheet/CategoryGroupCard.kt
git commit -m "feat(behaviorui): CategoryGroupCard 支持 displayColorMode 参数"
```

---

### 任务 8：修改 ActivityManagementUiState 新增 displayColorConfig

**文件：**
- 修改：`feature/management_activities/model/ActivityManagementUiState.kt`

- [ ] **步骤 1：修改数据类**

```kotlin
package com.nltimer.feature.management_activities.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.DisplayColorConfig
import com.nltimer.core.data.model.Tag

@Immutable
data class ActivityManagementUiState(
    val uncategorizedActivities: List<Activity> = emptyList(),
    val groups: List<GroupWithActivities> = emptyList(),
    val allGroups: List<ActivityGroup> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val expandedGroupIds: Set<Long> = emptySet(),
    val dialogState: DialogState? = null,
    val displayColorConfig: DisplayColorConfig = DisplayColorConfig(),  // 新增
)
```

- [ ] **步骤 2：Commit**

```bash
git add feature/management_activities/model/ActivityManagementUiState.kt
git commit -m "feat(management_activities): ActivityManagementUiState 新增 displayColorConfig"
```

---

### 任务 9：修改 ActivityManagementViewModel 收集和更新配置

**文件：**
- 修改：`feature/management_activities/viewmodel/ActivityManagementViewModel.kt`

- [ ] **步骤 1：注入 SettingsPrefs 并收集配置**

构造函数新增 `settingsPrefs: SettingsPrefs` 参数：

```kotlin
@HiltViewModel
class ActivityManagementViewModel @Inject constructor(
    private val repository: ActivityManagementRepository,
    private val addActivityUseCase: AddActivityUseCase,
    private val tagRepository: TagRepository,
    private val settingsPrefs: SettingsPrefs,  // 新增
) : ViewModel() {
```

在 `init` 中收集配置：

```kotlin
init {
    loadData()
    loadTags()
    viewModelScope.launch {
        repository.initializePresets()
    }
    // 新增：收集 DisplayColorConfig
    settingsPrefs.getDisplayColorConfigFlow()
        .onEach { config ->
            _uiState.update { it.copy(displayColorConfig = config) }
        }
        .launchIn(viewModelScope)
}
```

新增更新方法：

```kotlin
fun updateActivityIconColorMode(mode: DisplayColorMode) {
    viewModelScope.launch {
        val currentConfig = _uiState.value.displayColorConfig
        settingsPrefs.updateDisplayColorConfig(
            currentConfig.copy(activityIconColorMode = mode)
        )
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add feature/management_activities/viewmodel/ActivityManagementViewModel.kt
git commit -m "feat(management_activities): ViewModel 收集 DisplayColorConfig 并提供更新方法"
```

---

### 任务 10：修改 ActivityManagementScreen 传递配置并添加拖拽菜单选项

**文件：**
- 修改：`feature/management_activities/ui/ActivityManagementScreen.kt`

- [ ] **步骤 1：修改 ManagementActivityItem 暴露 color**

```kotlin
private data class ManagementActivityItem(
    val activity: Activity,
) : CategorizableItem {
    override val itemId: Long = activity.id
    override val itemName: String = activity.name
    override val category: String? = null
    override val usageCount: Int = activity.usageCount
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = activity.iconKey
    override val color: Long? = activity.color  // 新增
}
```

- [ ] **步骤 2：传递 displayColorMode 给 CategoryGroupCard**

在 `CategoryGroupCard` 调用处新增 `displayColorMode` 参数：

```kotlin
CategoryGroupCard(
    index = 0,
    groupName = "未分类",
    items = items,
    collapsed = false,
    showDragHandle = false,
    emptyText = "暂无未分类活动",
    displayColorMode = uiState.displayColorConfig.activityIconColorMode,  // 新增
    // ... 其他参数
)
```

分组卡片同理：

```kotlin
CategoryGroupCard(
    index = index,
    groupName = groupWithActivities.group.name,
    items = items,
    collapsed = !uiState.expandedGroupIds.contains(groupWithActivities.group.id),
    showDragHandle = true,
    emptyText = "暂无活动",
    displayColorMode = uiState.displayColorConfig.activityIconColorMode,  // 新增
    // ... 其他参数
)
```

- [ ] **步骤 3：修改 BottomBarDragFab 拖拽菜单选项**

```kotlin
val activityColorMode = uiState.displayColorConfig.activityIconColorMode
val dragOptions = listOf(
    "添加活动",
    "添加分组",
    when (activityColorMode) {
        DisplayColorMode.BACKGROUND -> "✓ 图标：背景色模式"
        else -> "图标：背景色模式"
    },
    when (activityColorMode) {
        DisplayColorMode.TEXT -> "✓ 图标：文字色模式"
        else -> "图标：文字色模式"
    },
    when (activityColorMode) {
        DisplayColorMode.NORMAL -> "✓ 图标：正常模式"
        else -> "图标：正常模式"
    },
)

BottomBarDragFab(
    state = dragFabState,
    icon = Icons.Default.Add,
    dragOptions = dragOptions,
    onClick = { viewModel.showAddActivityDialog() },
    onOptionSelected = { option ->
        when {
            option.startsWith("添加活动") -> viewModel.showAddActivityDialog()
            option.startsWith("添加分组") -> viewModel.showAddGroupDialog()
            option.contains("背景色模式") -> viewModel.updateActivityIconColorMode(DisplayColorMode.BACKGROUND)
            option.contains("文字色模式") -> viewModel.updateActivityIconColorMode(DisplayColorMode.TEXT)
            option.contains("正常模式") -> viewModel.updateActivityIconColorMode(DisplayColorMode.NORMAL)
        }
    },
)
```

- [ ] **步骤 4：Commit**

```bash
git add feature/management_activities/ui/ActivityManagementScreen.kt
git commit -m "feat(management_activities): 活动管理页面支持图标颜色配置选项"
```

---

### 任务 11：修改 TagManagementUiState 新增 displayColorConfig

**文件：**
- 修改：`feature/tag_management/model/TagManagementUiState.kt`

- [ ] **步骤 1：修改数据类**

```kotlin
package com.nltimer.feature.tag_management.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.DisplayColorConfig
import com.nltimer.core.data.model.Tag

@Immutable
data class TagManagementUiState(
    val uncategorizedTags: List<Tag> = emptyList(),
    val categories: List<CategoryWithTags> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val expandedCategoryNames: Set<String> = emptySet(),
    val allActivities: List<Activity> = emptyList(),
    val activityGroups: List<ActivityGroup> = emptyList(),
    val isLoading: Boolean = true,
    val dialogState: DialogState? = null,
    val displayColorConfig: DisplayColorConfig = DisplayColorConfig(),  // 新增
)
```

- [ ] **步骤 2：Commit**

```bash
git add feature/tag_management/model/TagManagementUiState.kt
git commit -m "feat(tag_management): TagManagementUiState 新增 displayColorConfig"
```

---

### 任务 12：修改 TagManagementViewModel 收集和更新配置

**文件：**
- 修改：`feature/tag_management/viewmodel/TagManagementViewModel.kt`

- [ ] **步骤 1：收集配置并新增更新方法**

在 `init` 中新增配置收集：

```kotlin
init {
    viewModelScope.launch {
        _addedCategories.value = settingsPrefs.getSavedTagCategoriesOrder().first()
    }
    loadData()
    loadActivities()
    loadGroups()
    // 新增：收集 DisplayColorConfig
    settingsPrefs.getDisplayColorConfigFlow()
        .onEach { config ->
            _uiState.update { it.copy(displayColorConfig = config) }
        }
        .launchIn(viewModelScope)
}
```

新增更新方法：

```kotlin
fun updateTagDisplayColorMode(mode: DisplayColorMode) {
    viewModelScope.launch {
        val currentConfig = _uiState.value.displayColorConfig
        settingsPrefs.updateDisplayColorConfig(
            currentConfig.copy(tagDisplayColorMode = mode)
        )
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add feature/tag_management/viewmodel/TagManagementViewModel.kt
git commit -m "feat(tag_management): ViewModel 收集 DisplayColorConfig 并提供更新方法"
```

---

### 任务 13：修改 TagManagementScreen 传递配置并添加拖拽菜单选项

**文件：**
- 修改：`feature/tag_management/ui/TagManagementScreen.kt`

- [ ] **步骤 1：修改 ManagementTagItem 暴露 color**

```kotlin
private data class ManagementTagItem(
    val tag: Tag,
) : CategorizableItem {
    override val itemId: Long = tag.id
    override val itemName: String = tag.name
    override val category: String? = tag.category
    override val usageCount: Int = tag.usageCount
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = tag.iconKey
    override val color: Long? = tag.color  // 新增
}
```

- [ ] **步骤 2：传递 displayColorMode 给 CategoryGroupCard**

在 `CategoryGroupCard` 调用处新增 `displayColorMode` 参数：

```kotlin
CategoryGroupCard(
    index = 0,
    groupName = "默认",
    items = items,
    collapsed = false,
    showDragHandle = false,
    emptyText = "暂无标签",
    displayColorMode = uiState.displayColorConfig.tagDisplayColorMode,  // 新增
    // ... 其他参数
)
```

分类卡片同理：

```kotlin
CategoryGroupCard(
    index = index,
    groupName = category.categoryName,
    items = items,
    collapsed = category.categoryName !in uiState.expandedCategoryNames,
    showDragHandle = true,
    emptyText = "暂无标签",
    displayColorMode = uiState.displayColorConfig.tagDisplayColorMode,  // 新增
    // ... 其他参数
)
```

- [ ] **步骤 3：修改 BottomBarDragFab 拖拽菜单选项**

```kotlin
val tagColorMode = uiState.displayColorConfig.tagDisplayColorMode
val dragOptions = listOf(
    "添加分类",
    "添加标签",
    when (tagColorMode) {
        DisplayColorMode.BACKGROUND -> "✓ 标签：背景色模式"
        else -> "标签：背景色模式"
    },
    when (tagColorMode) {
        DisplayColorMode.TEXT -> "✓ 标签：文字色模式"
        else -> "标签：文字色模式"
    },
    when (tagColorMode) {
        DisplayColorMode.NORMAL -> "✓ 标签：正常模式"
        else -> "标签：正常模式"
    },
)

BottomBarDragFab(
    state = dragFabState,
    icon = Icons.Default.Add,
    dragOptions = dragOptions,
    onClick = { viewModel.showAddCategoryDialog() },
    onOptionSelected = { option ->
        when {
            option.startsWith("添加分类") -> viewModel.showAddCategoryDialog()
            option.startsWith("添加标签") -> viewModel.showAddTagDialog(null)
            option.contains("背景色模式") -> viewModel.updateTagDisplayColorMode(DisplayColorMode.BACKGROUND)
            option.contains("文字色模式") -> viewModel.updateTagDisplayColorMode(DisplayColorMode.TEXT)
            option.contains("正常模式") -> viewModel.updateTagDisplayColorMode(DisplayColorMode.NORMAL)
        }
    },
)
```

- [ ] **步骤 4：Commit**

```bash
git add feature/tag_management/ui/TagManagementScreen.kt
git commit -m "feat(tag_management): 标签管理页面支持显示颜色配置选项"
```

---

### 任务 14：编译验证

- [ ] **步骤 1：运行编译**

```bash
./gradlew :app:compileDebugKotlin
```

预期：编译通过，无错误。

- [ ] **步骤 2：运行测试**

```bash
./gradlew :core:data:test :feature:management_activities:test :feature:tag_management:test
```

预期：现有测试通过。如有失败，检查是否因移除 `activityUseColorForText` / `tagUseColorForText` 导致。

- [ ] **步骤 3：Commit（如修复了测试）**

```bash
git commit -m "test: 更新测试适配 DisplayColorConfig 变更"
```

---

## 自检

### 规格覆盖度

| 规格需求 | 实现任务 |
|----------|----------|
| 新建 DisplayColorMode 枚举 | 任务 1 |
| 新建 DisplayColorConfig 数据类 | 任务 2 |
| DialogGridConfig 引用 DisplayColorConfig | 任务 3 |
| CategorizableItem 新增 color 字段 | 任务 4 |
| SettingsPrefs 新增接口 | 任务 5 |
| SettingsPrefsImpl 实现持久化 | 任务 6 |
| CategoryGroupCard 支持 displayColorMode | 任务 7 |
| ActivityManagementUiState 新增配置 | 任务 8 |
| ActivityManagementViewModel 收集/更新配置 | 任务 9 |
| ActivityManagementScreen 传递配置、拖拽菜单 | 任务 10 |
| TagManagementUiState 新增配置 | 任务 11 |
| TagManagementViewModel 收集/更新配置 | 任务 12 |
| TagManagementScreen 传递配置、拖拽菜单 | 任务 13 |
| 编译验证 | 任务 14 |

### 占位符扫描

- 无 "TODO"、"待定"、"后续实现"
- 所有步骤包含实际代码
- 所有类型和方法在定义后使用

### 类型一致性

- `DisplayColorMode` 枚举值前后一致：BACKGROUND / TEXT / NORMAL
- `DisplayColorConfig` 字段名前后一致：activityIconColorMode / tagDisplayColorMode
- 文件路径和类名一致
