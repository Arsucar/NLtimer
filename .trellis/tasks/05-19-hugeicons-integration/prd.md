# 添加 HugeIcons 图标库支持

## Goal

在活动创建/编辑的图标选择器中，新增 HugeIcons 图标库作为推荐首选选项卡，提供更美观、更丰富的图标选择，改善用户体验。

## Requirements

* Tab 顺序：**HugeIcons → Material → Emoji**（HugeIcons 排第一作为推荐首选）
* 精选约 **350 个** HugeIcons 图标，覆盖活动/时间管理场景
* 分类精简为 **~12-15 个大类**（将官方 62 个类别合并）
* iconKey 格式：`hi:<PascalCaseName>`（如 `hi:Earth`、`hi:AiSearch`）
* 扩展 `IconKeyResolver` 支持 `hi:` 前缀解析
* 扩展 `IconRenderer` 支持 HugeIcons 渲染
* 新建 `HugeIconCatalog` 管理精选图标列表（与 `MaterialIconCatalog` 同构）
* 添加 JitPack 仓库和 `hugeicons-compose:1.3` 依赖
* 向后兼容现有 `mi:` 和 emoji 格式 iconKey 数据

## Technical Approach

### 数据流

```
IconPickerSheet
  ├─ Tab 0: HugeIconTab → HugeIconCatalog → 选中 → "hi:<name>"
  ├─ Tab 1: MaterialIconTab → MaterialIconCatalog → "mi:<style>:<name>"
  └─ Tab 2: EmojiTab → EmojiCatalog → emoji 字符串

IconKeyResolver.resolveImageVector(iconKey)
  ├─ "hi:..." → HugeIconCatalog.resolve(name) → ImageVector
  └─ "mi:..." → MaterialIconCatalog.resolve(style, name) → ImageVector

IconRenderer
  ├─ iconKey == null → Text(defaultEmoji)
  ├─ "hi:..." → Icon(HugeIconCatalog.resolve)
  ├─ "mi:..." → Icon(MaterialIconCatalog.resolve)
  └─ else → Text(emoji)
```

### 新建/修改文件

1. **新建 `HugeIconCatalog.kt`** — 精选 ~350 个图标的目录，含分类枚举 `HugeIconCategory`（~12-15 个）
2. **修改 `IconKeyResolver.kt`** — 新增 `hi:` 前缀识别和解析逻辑
3. **修改 `IconRenderer.kt`** — 新增 HugeIcons 渲染分支
4. **修改 `IconPickerSheet.kt`** — 新增 HugeIconTab，调整 Tab 顺序为 3 个
5. **修改 `core/designsystem/build.gradle.kts`** — 添加 hugeicons-compose 依赖
6. **修改 `settings.gradle.kts`** — 添加 JitPack 仓库（如未添加）
7. **修改字符串资源** — 新增 HugeIcons 相关的中文/英文分类标签

### HugeIconCategory 分类方案（~12 类）

| 类别 | 合并自官方类别 | 预估图标数 |
|------|--------------|-----------|
| 通用操作 | editing, add-remove, check, filter-sorting | ~40 |
| 箭头导航 | arrows, navigation, menu | ~35 |
| 通讯 | communications | ~35 |
| 媒体 | media, image-camera, animation | ~30 |
| 文件文档 | files-folders, bookmark, download-upload | ~30 |
| 商务办公 | business, dashboard, presentation | ~30 |
| 用户 | users, hands | ~25 |
| 日期时间 | date-time, calendar | ~20 |
| 设备 | devices, energy, wifi | ~20 |
| 地图位置 | maps, logistics, buildings | ~20 |
| 天气自然 | weather | ~20 |
| 教育 | education, science-technology | ~15 |
| 医疗健康 | medical | ~15 |
| 其他 | security, e-commerce, search, settings | ~25 |
| **合计** | | **~350** |

### HugeIconCatalog 结构

仿照 `MaterialIconCatalog`，使用 `data class HugeIconEntry(name, category, imageVectorProvider, keywords)` + `enum class HugeIconCategory`，手动维护精选列表。

## Acceptance Criteria

* [ ] IconPickerSheet 展示 3 个 Tab：HugeIcons / Material / Emoji
* [ ] HugeIcons Tab 支持分类浏览和搜索（中英文关键词）
* [ ] 选中的 HugeIcons 图标保存为 `hi:<name>` 格式的 iconKey
* [ ] `hi:` 格式 iconKey 在 IconRenderer 中正确渲染为 HugeIcons ImageVector
* [ ] 已有活动的 `mi:` 和 emoji 格式 iconKey 不受影响
* [ ] 构建通过

## Definition of Done

* Detekt 通过
* 构建通过（`./gradlew :core:designsystem:compileDebugKotlin --no-daemon`）
* 新旧 iconKey 格式向后兼容

## Out of Scope

* 不修改 Room 数据库 schema
* 不移除 Material Icons 选项卡
* 不迁移现有活动的 iconKey
* 不使用 HugeIcons 官方 API 动态获取图标列表（纯静态目录）

## Research References

* [`research/hugeicons-library-analysis.md`](research/hugeicons-library-analysis.md) — 5,131 图标，扩展属性模式，62 官方类别，建议精选 ~350

## Technical Notes

* HugeIcons Compose: `com.github.rikkahub:hugeicons-compose:1.3` (JitPack)
* 包名：`me.rerere.hugeicons`
* HugeIcons 对象是空 object，所有图标是扩展属性（`val HugeIcons.Earth: ImageVector`）
* 每个图标 lazy-loaded + 缓存
* 描边颜色硬编码黑色，通过 Compose `tint` 参数着色
* 关键文件：
  - `core/designsystem/.../icon/IconPickerSheet.kt` — Tab 布局
  - `core/designsystem/.../icon/IconKeyResolver.kt` — iconKey 解析
  - `core/designsystem/.../icon/IconRenderer.kt` — 图标渲染
  - `core/designsystem/.../icon/MaterialIconCatalog.kt` — Material 图标目录（参考模式）
  - `core/designsystem/build.gradle.kts` — 依赖配置
