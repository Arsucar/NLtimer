# 标签图标架构研究

## 1. 数据模型

### TagEntity (core/data/.../entity/TagEntity.kt)
`
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long? = null,
    val iconKey: String? = null,       // <-- 图标键，存储格式见下
    val category: String? = null,      // <-- 标签所属分类（字符串，非独立实体）
    val priority: Int = 0,
    val usageCount: Int = 0,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val keywords: String? = null,
)
`

### Tag 模型 (core/data/.../model/Tag.kt)
`
data class Tag(
    val id: Long, val name: String, val color: Long?,
    val iconKey: String?, val category: String?,
    val priority: Int, val usageCount: Int, val sortOrder: Int,
    val keywords: String?, val isArchived: Boolean, val archivedAt: Long?
)
`
- iconKey 格式：
  - null -> 使用默认 emoji
  - "hi:IconName" -> HugeIcons 图标（如 hi:Timer01）
  - "mi:style:Name" -> Material Icons 图标（如 mi:filled:Star）
  - 纯文本/emoji -> 直接显示（如 "标签emoji" 或 "苹果emoji"）
- category -> 纯字符串，无独立的 TagGroupEntity。分类由 DataStore 持久化顺序。

**关键发现：没有 TagGroupEntity。** 分类只是 TagEntity 上的 category 字段（String?）。分类顺序存储在 SettingsPrefs.saveTagCategoriesOrder() 中。

---

## 2. 图标系统

### IconKeyResolver (core/designsystem/.../icon/IconKeyResolver.kt)
- isHugeIcon(key) -> 以 "hi:" 开头
- isMaterialIcon(key) -> 以 "mi:" 开头
- resolveImageVector(key) -> 通过 HugeIconCatalog 或 MaterialIconCatalog 解析
- iconKeyToDisplayText(key) -> 提取显示名

### IconRenderer (core/designsystem/.../icon/IconRenderer.kt)
`
fun IconRenderer(
    iconKey: String?,
    defaultEmoji: String = "pushpin",
    tint: Color,
    iconSize: Dp = 24.dp,
    emojiFontSize: TextUnit,
) {
    when {
        iconKey == null -> Text(defaultEmoji)
        isHugeIcon(iconKey) -> Icon(resolveImageVector(iconKey)) // fallback -> defaultEmoji
        isMaterialIcon(iconKey) -> Icon(resolveImageVector(iconKey)) // fallback -> defaultEmoji
        else -> Text(iconKey)  // 直接显示（emoji/文本）
    }
}
`

### IconPickerSheet (core/designsystem/.../icon/IconPickerSheet.kt)
- 三个 Tab：HugeIcons / Material Icons / Emoji
- 搜索、手动输入（最多4个code points）、重置
- 输出格式："hi:Name" / "mi:style:Name" / 纯emoji文本

---

## 3. 标签管理页面 (TagManagement)

### TagManagementScreen (feature/tag_management/.../TagManagementScreen.kt)
- 使用 CategoryGroupCard 显示分组
- ManagementTagItem (私有类) 实现 CategorizableItem：
  override val iconKey: String? = tag.iconKey  // 正确传递了 iconKey
- BottomBarDragFab（左下角）-> 选项："添加分类"、"添加标签"
- 右下角：FilledTonalIconButton（一键展开/收纳）

### CategoryGroupCard (core/behaviorui/.../CategoryGroupCard.kt)
`
fun <T : CategorizableItem> CategoryGroupCard(
    showItemIcon: Boolean = true,  // <-- 全局图标开关参数！
    ...
)
`
- ItemChip 内部调用 IconRenderer(iconKey = item.iconKey, defaultEmoji = "?emoji", ...)
- Bug 来源：当 iconKey == null 且 defaultEmoji = "?" 时显示问号

### TagPicker (core/behaviorui/.../sheet/TagPicker.kt)
`
fun TagPicker(tags: List<Tag>, selectedTagIds: Set<Long>, onTagToggle: (Long) -> Unit)
`
- **没有显示图标**！只显示 tag.name 文本
- 只是一个 FlowRow 的 Surface + Text，不使用 IconRenderer

---

## 4. 关键 Bug：TagCategorizable 的 iconKey 硬编码为 null

### CategoryPickerModels (core/behaviorui/.../CategoryPickerModels.kt)
`
data class TagCategorizable(
    val tag: Tag,
    override val lastUsedTimestamp: Long? = null,
) : CategorizableItem {
    override val iconKey: String? = null    // BUG! 应该是 tag.iconKey
    override val color: Long? = tag.color
}
`

**这是核心 bug！** TagCategorizable 将 iconKey 硬编码为 null，而不是 tag.iconKey。
这意味着所有使用 CategoryGroupCard 配合 TagCategorizable 的地方，标签图标都不会显示，而是显示 defaultEmoji "?"。

但在 TagManagementScreen 中的 ManagementTagItem 是正确的：
`
override val iconKey: String? = tag.iconKey  // 正确
`

---

## 5. 图标开关机制

### showItemIcon 参数
CategoryGroupCard 有 showItemIcon: Boolean = true 参数，控制 ItemChip 内部是否显示图标。

当前使用情况：
- TagManagementScreen -> 通过 CategoryGroupCard，showItemIcon 默认 true -> 显示图标 OK
- CategoriesScreen -> showItemIcon = false -> 不显示图标
- TagPicker -> 完全独立的实现，不使用 CategoryGroupCard -> 不显示图标

### 无全局"图标开关"设置
在 SettingsPrefs / SettingsPrefsImpl 中，没有 tag icon show/hide 的开关设置。
现有设置：
- DisplayColorConfig（activityIconColorMode / tagDisplayColorMode）-> 控制颜色模式（BACKGROUND/TEXT/NORMAL），不控制图标显示
- TagDisplayConfig（displayMode/layoutMode/columnLines/horizontalLines/useColorForText）-> 控制布局，不涉及图标

---

## 6. 表单中的图标编辑

### ActivityFormSpecs (core/designsystem/.../form/ActivityFormSpecs.kt)
createTag 和 editTag 使用 FormRow.IconColor(iconKey = "icon", colorKey = "color", initialEmoji = "tagEmoji")

### FormRowRenderers (core/designsystem/.../form/FormRowRenderers.kt)
- iconColorRenderer 使用 IconRenderer 渲染当前图标预览
- 点击打开 IconPickerSheet 选择图标
- 编辑标签时："icon" to (tag.iconKey ?: "tagEmoji") -> 正确传入 iconKey

---

## 7. 标签选择对话框中的图标

### ActivityFormSheets (feature/management_activities/.../ActivityFormSheets.kt)
使用 TagPicker 组件 -> 不显示图标。

### CategoryPickerDialog (core/behaviorui/.../CategoryPickerDialog.kt)
通用选择对话框，使用 CategoryGroupCard 显示项目。
- 如果传入的 item 是 TagCategorizable -> iconKey 为 null -> 显示 ?
- 如果传入的 item 是 ManagementTagItem -> iconKey 正确 -> 显示实际图标

---

## 8. 总结：发现的问题

| # | 问题 | 位置 | 严重性 |
|---|------|------|--------|
| 1 | TagCategorizable.iconKey 硬编码为 null，导致标签图标在 CategoryPickerDialog 中显示为 ? | CategoryPickerModels.kt:53 | 高 |
| 2 | TagPicker 组件完全不显示图标，只有文本 | TagPicker.kt | 中 |
| 3 | 没有全局图标显示/隐藏开关 | 无（需新增） | 需求 |
| 4 | 分类（category）没有 iconKey 字段 | TagEntity.category 是纯 String | 需求 |

---

## 9. 文件路径索引

| 文件 | 绝对路径 |
|------|----------|
| TagEntity | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\database\entity\TagEntity.kt |
| Tag model | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\model\Tag.kt |
| TagRepository | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\repository\TagRepository.kt |
| SettingsPrefs | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\SettingsPrefs.kt |
| SettingsPrefsImpl | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\SettingsPrefsImpl.kt |
| DisplayColorConfig | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\model\DisplayColorConfig.kt |
| TagDisplayConfig | D:\2026Code\Group_android\NLtimer\core\data\src\main\java\com\nltimer\core\data\model\TagDisplayConfig.kt |
| IconRenderer | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\icon\IconRenderer.kt |
| IconKeyResolver | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\icon\IconKeyResolver.kt |
| IconPickerSheet | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\icon\IconPickerSheet.kt |
| MaterialIconCatalog | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\icon\MaterialIconCatalog.kt |
| HugeIconCatalog | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\icon\HugeIconCatalog.kt |
| CategoryGroupCard | D:\2026Code\Group_android\NLtimer\core\behaviorui\src\main\java\com\nltimer\core\behaviorui\sheet\CategoryGroupCard.kt |
| CategoryPickerModels | D:\2026Code\Group_android\NLtimer\core\behaviorui\src\main\java\com\nltimer\core\behaviorui\sheet\CategoryPickerModels.kt |
| CategoryPickerDialog | D:\2026Code\Group_android\NLtimer\core\behaviorui\src\main\java\com\nltimer\core\behaviorui\sheet\CategoryPickerDialog.kt |
| TagPicker | D:\2026Code\Group_android\NLtimer\core\behaviorui\src\main\java\com\nltimer\core\behaviorui\sheet\TagPicker.kt |
| TagManagementScreen | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\TagManagementScreen.kt |
| TagManagementViewModel | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\viewmodel\TagManagementViewModel.kt |
| TagManagementUiState | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\model\TagManagementUiState.kt |
| TagManagementSheetRouter | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\TagManagementSheetRouter.kt |
| AddTagFormSheet | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\components\dialogs\AddTagFormSheet.kt |
| EditTagFormSheet | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\components\dialogs\EditTagFormSheet.kt |
| MoveTagDialogWrapper | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\MoveTagDialogWrapper.kt |
| ActivityFormSpecs | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\form\ActivityFormSpecs.kt |
| FormRowRenderers | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\form\FormRowRenderers.kt |
| FormSpec | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\form\FormSpec.kt |
| AppTagChip | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\component\AppTagChip.kt |
| GroupCard | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\component\GroupCard.kt |
| BottomBarDragFab | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\component\BottomBarDragFab.kt |
| DisplayColorMode | D:\2026Code\Group_android\NLtimer\core\designsystem\src\main\java\com\nltimer\core\designsystem\theme\DisplayColorMode.kt |
| TagChip (management) | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\components\TagChip.kt |
| CategoryCard (management) | D:\2026Code\Group_android\NLtimer\feature\tag_management\src\main\java\com\nltimer\feature\tag_management\ui\components\CategoryCard.kt |
| CategoriesScreen | D:\2026Code\Group_android\NLtimer\feature\categories\src\main\java\com\nltimer\feature\categories\ui\CategoriesScreen.kt |
