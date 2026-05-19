# 新增四个数据管理工具

## Goal

在 `core:tools` 模块中新增四个批量/数据管理工具，供 AI Agent 和 UI 统一调用：

1. **batchDeleteBehaviors** — 批量删除行为记录
2. **bulkUpdateActivities** — 批量修改活动属性（分组、图标、颜色等）
3. **setBehaviorTag** — 给行为记录增/删/替换标签
4. **exportData / importData** — 按日期范围导入导出数据

## What I already know

* 工具系统架构：`ToolDefinition` 接口 → `@Singleton` 实现 → Hilt `@Binds @IntoSet` 注册 → `ToolRegistry` 统一管理
* 现有模式参考：`DeleteBehaviorTool`（单条删除）、`BatchCreateActivitiesTool`（批量创建）
* DAO 已有：`BehaviorDao.delete(id)`、`BehaviorDao.insertTagCrossRefs`、`BehaviorDao.removeTagCrossRefs`、`BehaviorDao.deleteTagsForBehavior`、`BehaviorDao.getByTimeRangeSync`
* 导出/导入已存在：`DataExportImportRepository`（全量导出 `exportAll` / `importAll`，但不支持日期范围过滤）
* 标签关联表：`behavior_tag_cross_ref`，DAO 已有 `insertTagCrossRefs`、`removeTagCrossRefs`、`deleteTagsForBehavior`
* Activity 更新：`ActivityRepository.update(activity)`、`ActivityManagementRepository.moveActivityToGroup`
* `ToolCategory` 现有枚举：`TIMING, STATISTICS, GOALS, ACTIVITIES, REMINDERS, SETTINGS`
* `AccessLevel` 现有枚举：`NONE, READ, WRITE, FULL`
* 工具文件位置模式：`core/tools/src/main/java/com/nltimer/core/tools/<分组>/`
* DI 注册模式：每个分组一个 `*ToolsModule.kt`，用 `@Binds @IntoSet` 逐个绑定

## Assumptions (temporary)

* 这些工具归入 `ACTIVITIES` category（已有分类，最贴切）
* batchDeleteBehaviors / setBehaviorTag 权限为 FULL（涉及删除/修改）
* bulkUpdateActivities 权限为 WRITE
* exportData / importData 权限为 WRITE
* 导出格式沿用现有 JSON 格式（`ExportData` 模型），暂不新增 CSV
* 导入复用现有 `DataExportImportRepository.importAll`，但需新增按日期范围过滤的导出能力

## Open Questions

1. **导出日期范围**：`DataExportImportRepository` 目前只有 `exportAll()`，没有按日期范围过滤。新工具的 `exportData` 是否需要新增一个 `exportByDateRange(start, end)` 方法到 Repository？还是直接在 Tool 层用 `BehaviorDao.getByTimeRangeSync` + 其他 DAO 查询自行组装？
2. **batchDeleteBehaviors 上限**：`BatchCreateActivitiesTool` 限制 20 条，批量删除是否也设上限？考虑到可能清理大量旧数据，上限可以更大（如 100？）
3. **bulkUpdateActivities 聚合操作**：是每个活动可以设置不同属性值（数组里每个元素独立），还是批量统一设同一个值（如"所有活动移到同一分组"）？

## Requirements (evolving)

### 1. batchDeleteBehaviors

- 传入 `ids: List<Long>`，批量删除对应行为记录
- 同时删除 `behavior_tag_cross_ref` 中的关联
- 单次上限 20 条（与 `BatchCreateActivitiesTool` 一致）
- 返回 `{deleted: [id1, id2, ...], notFound: [id3, ...]}`

### 2. bulkUpdateActivities

- 传入 `updates: List<{id, groupId?, iconKey?, color?, name?}>`，每个元素独立指定活动 ID 和要修改的字段
- 灵活模式：每个活动可改不同字段，统一模式是其子集（多个元素指向同一变更）
- 仅更新传入的字段（部分更新），未传的字段保持不变
- 单次上限 20 条
- 返回 `{updated: [id1, id2, ...], notFound: [id3, ...]}`

### 3. setBehaviorTag

- 传入 `behaviorId: Long`、`tagIds: List<Long>`、`mode: "add" | "remove" | "replace"`
- `add`：在现有标签基础上追加
- `remove`：移除指定标签
- `replace`：替换全部标签为指定列表
- 返回操作后的完整标签列表

### 4. exportData / importData

- 导出：按日期范围（`startDate`, `endDate`）导出行为、活动、标签数据为 JSON
  - 在 `DataExportImportRepository` 新增 `exportByDateRange(start, end): ExportData`
  - 导出指定时间范围内的 behaviors，并自动包含关联的 activities、tags、groups
- 导入：接收 JSON 数据导入，复用现有 `importAll(data, mode)`
  - 支持 `SMART` / `OVERWRITE` 模式
- 拆为两个独立工具：`exportData` 和 `importData`

## Acceptance Criteria (evolving)

- [ ] 四个工具均实现 `ToolDefinition` 接口
- [ ] Hilt DI 注册完成（新增 `DataManagementToolsModule` 或追加到现有 Module）
- [ ] 参数校验完备（必填、类型、上限）
- [ ] 错误处理返回 `ToolResult.Error`（不抛异常）
- [ ] 编译通过 + Detekt 无新增 warning

## Definition of Done

- 代码编译通过 (`./gradlew :core:tools:compileDebugKotlin --no-daemon`)
- Detekt 静态检查无新增 error
- 工具文档（`getDocumentation()`）完整

## Out of Scope (explicit)

- CSV 格式支持（仅 JSON）
- UI 层集成（本次仅实现 Tool 层）
- 增量导出/导入优化
- 导入时的数据冲突 UI 提示

## Technical Notes

- 现有 `DataExportImportRepository` 的 `exportAll()` 返回 `ExportData` 模型，需确认模型结构
- `BehaviorDao` 没有 `deleteByIds(ids: List<Long>)` 批量删除方法，需新增或循环调用
- `ActivityDao` 有 `@Update suspend fun update(activity: ActivityEntity)`，部分更新需先查出完整 entity 再修改字段后更新
- `BehaviorDao` 已有标签关联操作：`insertTagCrossRefs`、`removeTagCrossRefs`、`deleteTagsForBehavior`
- 工具放在 `core/tools/src/main/java/com/nltimer/core/tools/library/` 或新建 `management/` 分组
