# 标签图标优化：分组iconKey、全局图标开关、图标bug修复

## Goal

优化标签管理页面的图标系统：修复标签选择弹窗中图标显示❓的bug，为标签分类新增 iconKey 字段支持统一管理图标，添加全局标签图标显示开关，并将默认图标改为 HugeIcons tag 图标。

## What I already know

* `TagCategorizable.iconKey` 硬编码为 `null`，导致选择弹窗中标签图标显示 ❓
* `CategoryGroupCard.ItemChip` 当 `iconKey == null` 时 fallback 到 `defaultEmoji = "❓"`
* 标签分类 (`TagEntity.category`) 是纯 String，没有 iconKey 字段
* `ActivityGroupEntity` 也没有 `iconKey` 字段
* `TagPicker` 组件完全不显示图标（纯文本）
* SettingsPrefs 没有全局图标显示/隐藏开关
* 左下角 `BottomBarDragFab` 仅有"添加分类"、"添加标签"两个选项
* IconRenderer 支持 `hi:` (HugeIcons)、`mi:` (Material Icons)、emoji 三种图标格式

## Requirements

### Bug 修复

1. **修复 TagCategorizable.iconKey**：将 `override val iconKey: String? = null` 改为 `override val iconKey: String? = tag.iconKey`
2. **修复分类选择弹窗中分组图标显示❓**：分组（分类）没有 iconKey，不应显示图标占位符
3. **修复 TagPicker 不显示图标**：TagPicker 应渲染标签图标

### 新功能

4. **标签分类新增 iconKey 字段**：分类下的标签统一由分类管理图标（待确认数据模型方案）
5. **活动分组新增 iconKey 字段**：活动分组同样支持 iconKey（同上）
6. **用户可更改图标**：在分类/分组的编辑界面提供 IconPickerSheet
7. **全局标签图标显示开关**：在 NLtimerScaffold 全局设置 FAB 的 TAG_MANAGEMENT 路由菜单中添加"✓ 显示图标" / "显示图标" toggle 项（与现有"标签：背景色"等并列），存入 SettingsPrefs/DisplayColorConfig，影响所有标签显示处（管理页、添加弹窗、选择弹窗）
8. **默认标签图标改为 HugeIcons tag 图标**：将 IconRenderer 中标签默认 emoji 从 ❓ 改为 `hi:` 前缀的 tag 图标

### UI 调整

9. **标签管理页面中每个标签的图标缩小**：CategoryGroupCard ItemChip 中图标从 20dp → 16dp

## Acceptance Criteria

- [ ] TagCategorizable.iconKey 正确传递 tag.iconKey
- [ ] 分类选择弹窗中，分组无图标时不显示图标占位符
- [ ] TagPicker 显示标签图标
- [ ] 标签分类有 iconKey 字段，编辑时可选择图标
- [ ] 活动分组有 iconKey 字段，编辑时可选择图标
- [ ] 全局开关控制标签图标显示/隐藏
- [ ] 默认标签图标为 HugeIcons tag 图标
- [ ] 标签管理页面中标签图标尺寸缩小

## Decision (ADR-lite)

**Context**: 标签分类目前是 `TagEntity.category: String?` 虚拟字段，无法存储 iconKey 等元数据，需选择存储方案。
**Decision**: 新建 `TagGroupEntity` 表（对齐 `ActivityGroupEntity`），`TagEntity` 新增 `groupId` 外键，废弃 `category` 字段。
**Consequences**: 数据库版本升级 + 迁移；分类 CRUD 逻辑从 TagRepository/Prefs 迁移到 TagGroupDao；与活动分组架构一致，维护性好。

## Technical Notes

* Bug 根因：`CategoryPickerModels.kt:53` — `TagCategorizable.iconKey = null`
* IconRenderer: `core/designsystem/.../icon/IconRenderer.kt`
* CategoryGroupCard.ItemChip defaultEmoji = "❓"
* 标签分类管理：TagRepository 中通过 category String 字段分组
* 新表 `tag_groups`：字段 id, name, iconKey, sortOrder, isArchived, archivedAt, createdAt
* TagEntity 新增 `groupId: Long?` 外键 → tag_groups.id，保留 `category` 做迁移兼容后废弃
* ActivityGroupEntity 新增 `iconKey: String?` 字段
* 全局图标开关：DisplayColorConfig 新增 `showTagIcon: Boolean = true`，存入 SettingsPrefs DataStore
* 开关位置：NLtimerScaffold settingsDragOptions TAG_MANAGEMENT 路由，与"标签：背景色"等并列
* 图标缩小：CategoryGroupCard ItemChip iconSize 20dp → 16dp，Box 容器同步缩小
* 默认图标：CategoryGroupCard ItemChip 的 defaultEmoji "❓" → 改为 HugeIcons tag 图标 key（如 "hi:tag"）
* 全局开关控制：CategoryGroupCard.ItemChip、TagPicker、ManagementTagItem 等所有标签显示处读取 showTagIcon
