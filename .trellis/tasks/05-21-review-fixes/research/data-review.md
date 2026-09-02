# core:data 数据层审查与深度优化报告

> 生成时间：2026-08-01
> 范围：`core/data/src/main/java/com/nltimer/core/data/`（database/dao、database/entity、database/migration、model、repository、repository/impl、usecase、util、di）
> 分支：dev-v6
> 约束：不改 Room schema/migration 已有文件、不改 schema json、不编译、不改公共 API 签名（除非安全等价）

---

## 概览

| 级别 | 修复 | 记录 |
|------|------|------|
| P0（bug） | 1 | 0 |
| P1（性能/并发/正确性） | 5 | 3 |
| P2（整洁/死代码/detekt） | 5 | 11 |

---

## P0 — 正确性 Bug（已修复）

### 1. 导入后活动侧标签绑定丢失（source 方向错误）

- 文件：`repository/impl/DataExportImportRepositoryImpl.kt`
- 位置：`importAllSmart`（~L181）、`importAllOverwrite`（~L229）、`importActivitiesSmart`（~L266）、`importActivitiesOverwrite`（~L297）
- 问题：4 条导入路径创建 `ActivityTagBindingEntity` 时未指定 source，使用实体默认值 `"tag"`；而活动侧查询 `TagDao.getByActivityId`、`ActivityDao.getTagIdsForActivitySync` 均过滤 `source = 'activity'`。导致：导入（覆盖或智能）后，活动的标签在活动管理页 / HomeViewModel / ListActivitiesTool 中不可见，等于静默丢数据。
- 修复：显式指定 `source = "activity"`（导出端 `toExported` 把两种 source 的绑定统一压平成活动的 tagNames，回导时应还原为活动侧绑定）。

---

## P1 — 性能 / 正确性（已修复）

### 2. exportByDateRange N+1 查询

- 文件：`repository/impl/DataExportImportRepositoryImpl.kt` `exportByDateRange`
- 问题：`activities = activityIds.mapNotNull { activityDao.getById(it) }` 对每个 activity 发一条查询（N+1），活动多时慢。
- 修复：改为 `activityDao.getByIds(activityIds.toList()).associateBy { it.id }` 一次批量查询。

### 3. completeCurrentAndStartNext 返回过期实体

- 文件：`repository/impl/BehaviorRepositoryImpl.kt` `completeCurrentAndStartNext`
- 问题：把下一个 PENDING 置为 ACTIVE 并更新 startTime 后，返回的仍是更新前取到的 `nextPending` 实体（status=pending、startTime=0），与数据库状态不一致。
- 修复：更新后 `behaviorDao.getById(nextPending.id)` 重新读取再转换返回。

### 7. 编辑行为缺少冲突/吸附检查 ✅ 已修复（续轮）

- 文件：`usecase/AddBehaviorUseCase.kt` `executeEdit` / `performSnapAndConflictCheck`
- 问题：编辑路径不执行 `performSnapAndConflictCheck`，把已存在行为改到与其他行为重叠的时间段不会报冲突。
- 修复：`performSnapAndConflictCheck` 增加 `ignoreBehaviorId`；`executeEdit` 校验后调用并以自身 id 忽略；冲突时返回 `Result.Conflict`；成功时使用吸附后的 `finalStart`/`finalEnd` 写入。

### 8. SMART 导入非事务 ✅ 已修复（续轮）

- 文件：`repository/impl/DataExportImportRepositoryImpl.kt` `importAllSmart` / `importActivitiesSmart` / `importTagsSmart`
- 问题：逐个 getByName/insert/update，未包事务；中途异常留下部分导入数据。
- 修复：三处 SMART 导入整体包 `database.withTransaction`（与 OVERWRITE 路径一致，Room 可重入）。

### 9. 编辑计划行为后 achievementLevel 不重算 ✅ 已修复（续轮）

- 文件：`repository/impl/BehaviorRepositoryImpl.kt` `updateBehavior`
- 问题：重算 actualDuration 但不重算 achievementLevel；编辑结束时间后成就等级仍是旧值。
- 修复：COMPLETED 时读取实体 `wasPlanned`/`estimatedDuration`，用 `BehaviorCalculator.calculateCompletion` 同时写 `actualDuration` 与 `achievementLevel`（有值时）。

---

## P1 — 记录（建议后续处理，本次未改）

### 4. Migration14To15 source 默认值导致历史绑定方向丢失

- 文件：`database/migration/Migration14To15.kt`
- 问题：`ALTER TABLE activity_tag_binding ADD COLUMN source TEXT NOT NULL DEFAULT 'tag'` 会把 v15 之前所有既有绑定标记为 `source='tag'`。升级后 `TagDao.getByActivityId`（source='activity'）与 `ActivityDao.getTagIdsForActivitySync`（source='activity'）对历史活动→标签绑定返回空，活动侧 UI 静默丢失标签展示。
- 建议：发布 v15 时对历史数据做一次性修正（如将既有绑定更新为 `source='activity'`，或同步调整查询不区分 source）；需产品确认历史绑定归属哪一侧。因约束禁止修改已有 migration，未改。

### 5. fallbackToDestructiveMigration(true) 静默清库风险

- 文件：`di/DatabaseModule.kt` `provideDatabase`
- 问题：`fallbackToDestructiveMigration(true)` 在任何 migration 失败时直接删表重建，用户数据被静默清空。
- 建议：改为 `fallbackToDestructiveMigration(false)`（缺迁移时崩溃提示升级）或在发布前用测试库验证 3→15 全链迁移；`ALL_MIGRATIONS` 已覆盖 3→15。属产品决策，未改。

### 6. AddBehaviorUseCase 新增流程非事务

- 文件：`usecase/AddBehaviorUseCase.kt` `invoke`
- 问题：`endCurrentBehavior → snap/conflict 检查 → 后续 sequence 平移 → insert` 未包在单个事务中，并发添加时 sequence 可能交错错乱；中途异常留下已平移未插入的状态。
- 建议：将整个新增流程提升为 repository 级 `withTransaction`（需扩展 `BehaviorRepository` 公共入口，本轮未改签名故未做）。

---

## P2 — 已修复

### 10. DatabaseModule SpreadOperator（detekt）

- 文件：`di/DatabaseModule.kt`
- 问题：`addMigrations(*ALL_MIGRATIONS)` 触发 detekt SpreadOperator。
- 修复：`@Suppress("SpreadOperator")` + 注释说明 Room vararg 必需。

### 11. SettingsPrefsImpl.updateTheme 超长行（detekt MaxLineLength ×3）

- 文件：`SettingsPrefsImpl.kt` L122-124
- 问题：`val cornerScale = ...; if (...) ... else ...` 三行 >120 字符。
- 修复：拆成多行。

### 12. TimeSnapService 魔法数字（detekt MagicNumber）

- 文件：`util/TimeSnapService.kt` `snapAndCheckConflict`
- 问题：`+ 59_999` 魔法数字。
- 修复：改为 `+ MILLIS_PER_MINUTE - 1`。

### 13. StatsQueryUseCase String.format 缺 Locale（detekt ImplicitDefaultLocale ×4）

- 文件：`usecase/StatsQueryUseCase.kt`（avgAchievement / completionRate / planAdherenceRate / totalHours）
- 问题：`String.format` 未指定 Locale，受系统 locale 影响小数分隔符。
- 修复：统一 `String.format(Locale.US, ...)`。

### 18. hasTimeConflict currentTime 参数未使用 ✅ 已处理（续轮）

- 文件：`util/TimeConflictUtils.kt` `hasTimeConflict`
- 问题：参数 `currentTime`（含默认值）在函数体内未使用（ACTIVE 按 Long.MAX_VALUE 处理）。
- 处理：保留签名以兼容命名参数调用方；加 `@Suppress("UnusedParameter")` + KDoc 说明 ACTIVE 按设计为 `[start, +∞)` 不截断到 now（避免改语义/改公共签名）。

---

## P2 — 记录（本次未改）

### 14. 死代码 DAO 方法（删除需同步测试 fake，建议下次随测试清理一起做）

- `database/dao/BehaviorDao.kt`：`getUsageCount`、`getTotalDurationMs`、`getLastUsedTimestamp`（生产无调用，仅测试 fake override）
- `database/dao/ActivityDao.kt`：`getAllPresets`（Flow 版，生产只用 Sync 版）
- `ActivityDao.search` + `ActivityRepository.search` + `ActivityRepositoryImpl.search` 整链死代码（仅 `ActivityRepositoryImplTest` 直接调用）
- `TagDao.search` + `TagRepository.search` + `TagRepositoryImpl.search` 整链死代码（仅 `TagRepositoryImplTest` 直接调用）
- 提示：删除接口方法会使测试 fake 中的 `override fun` 报 "overrides nothing"，需同步清理 `core/data/src/test` 与 `feature/*/src/test` 中 6 处 fake。

### 15. TagGroup 领域模型未使用

- 文件：`model/TagGroup.kt`
- 问题：全仓库零引用（TagGroupEntity / TagGroupDao / tag_groups 表均被使用，仅模型未用）。
- 建议：后续 tag_groups UI 落地若仍不用则删除；本路为避免与并行分支冲突未删。

### 16. getActivityStatsSync 命名误导

- 文件：`database/dao/BehaviorDao.kt` `getActivityStatsSync`
- 问题：返回 `Flow<ActivityStatsRow>` 却命名 Sync，与 `getTagsForBehaviorSync`（真同步）混淆。
- 建议：改名 `getActivityStats`，同步改 `ActivityManagementRepositoryImpl` 与测试 fake。

### 17. 重叠查询重复实现

- 文件：`database/dao/BehaviorDao.kt` `getBehaviorsOverlappingRange` 与 `getByOverlappingTimeRange`
- 问题：相同 SQL（一个带 ORDER BY），可合并为一个带排序版本。
- 建议：保留带排序的，另一处改用之。

### 19. CategoryRepository parent 参数未使用

- 文件：`repository/CategoryRepository.kt` 与 `impl/CategoryRepositoryImpl.kt`
- 问题：`getDistinctActivityCategories(parent)`、`renameActivityCategory(old, new, parent)`、`getDistinctTagCategories(parent)`、`renameTagCategory(old, new, parent)` 的 `parent` 参数均未使用。
- 建议：删除参数或实现父子分类（当前是单层设计）。

### 20. detekt TooManyFunctions（复杂重构，仅记录）

- `database/dao/BehaviorDao.kt`：45 个函数（阈值 24）
- `repository/BehaviorRepository.kt`：32 个函数（阈值 24）
- 建议：按「查询/写入/统计/标签关联」拆接口，属超大重构。

### 21. detekt CyclomaticComplexMethod（复杂重构，仅记录）

- `SettingsPrefsImpl.kt`：`getDialogConfigFlow`（27）、`getHomeLayoutConfigFlow`（26）
- 建议：按配置块拆私有读取函数。

### 22. exportActivities 不导出分组定义

- 文件：`repository/impl/DataExportImportRepositoryImpl.kt` `exportActivities`
- 问题：活动带 `groupName` 导出但不包含分组实体；导入到空库时 `groupId` 解析不到 → 活动失去分组。
- 建议：与 `exportByDateRange` 一致，导出涉及分组（至少 involvedGroups）。

### 23. PENDING 创建 sequence 非事务

- 文件：`usecase/AddBehaviorUseCase.kt` `calculateSequence`
- 问题：`getMaxSequence() + 1` 无并发保护，两个并发创建可能拿到相同 sequence。
- 建议：并入整体事务（见 P1-6）。

### 24. 其他 detekt 低严重项

- `util/TimeFormatUtils.kt` `formatGridDurationHours` MagicNumber（`10` / `3_600_000`）
- `util/TimeConflictUtils.kt` currentTime 已 Suppress（见 18）
- `SettingsPrefsImpl.kt` MagicNumber（parseTimeLabelConfig 等）
- `StatsQueryUseCase.kt` 大函数（约 110 行 query，含 3 个私有聚合类）——可抽聚合逻辑，属中等重构。

---

## 审查过程中确认无问题/有意设计（不处理）

- `activity_tag_binding` 的 `source` 双向独立语义（PK 冲突时 IGNORE）是文档化设计。
- `TagDao.delete(tag)` 受 `behavior_tag_cross_ref` FK RESTRICT 限制是文档化设计。
- SMART 导入对已存在同名实体执行 `mergeFrom` 合并而非覆盖，符合 SMART 语义。
- `AddBehaviorUseCase` 对 COMPLETED 不自动吸附、只报冲突，是刻意行为（用户明确选择时间）。
- `SettingsPrefsImpl.getSavedTagCategories(Order)` 共享同一 DataStore key 是有意设计（Set 与 List 是同一份字符串的两种读法）。
- `CategoryRepositoryImpl.addTagCategoryStub` 只查重不落库是接口文档明示的占位语义（Tag.category 无独立表）。
- migration 3→15 链整体与当前实体结构一致（含 v15 source 列、v14 tag_groups/groupId/iconKey）；`PRAGMA foreign_keys` 开关包裹合理。
