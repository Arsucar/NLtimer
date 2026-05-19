# Tools Inventory

> Auto-generated from source code on 2026-05-19

## Summary

| Category | Count |
|----------|-------|
| TIMING | 8 |
| ACTIVITIES | 18 |
| STATISTICS | 0 |
| GOALS | 0 |
| REMINDERS | 0 |
| SETTINGS | 0 |
| **Total** | **26** |

---

## TIMING (8 tools)

| # | Name | Description | Access | Parameters | Return |
|---|------|-------------|--------|------------|--------|
| 1 | `queryCurrentBehavior` | 查询当前正在进行的行为记录；没有则返回 null | READ | *(none)* | Behavior? |
| 2 | `startBehavior` | 为指定活动开始一段计时（写入 ACTIVE 状态的 Behavior） | WRITE | `activityId` (Number, **required**, min=1), `note` (String, opt, maxLen=500) | Long |
| 3 | `endBehavior` | 结束当前正在进行的计时（ACTIVE → COMPLETED） | WRITE | *(none)* | Long |
| 4 | `recordBehavior` | 补录已结束行为；起止时间用 ISO 8601。冲突时返回 JSON 含 conflicts 数组 | WRITE | `activityId` (Number, **required**, min=1), `startTime` (String, **required**, ISO 8601), `endTime` (String, **required**, ISO 8601), `tagIds` (Array, opt), `note` (String, opt, maxLen=500) | Long |
| 5 | `createGoal` | 创建 PENDING 目标（不立即开始计时），可指定预计用时（分钟）与标签 | WRITE | `activityId` (Number, **required**, min=1), `tagIds` (Array, opt), `estimatedDurationMinutes` (Number, opt, min=1), `note` (String, opt, maxLen=500) | Long |
| 6 | `deleteBehavior` | 按 id 删除行为记录；常用于冲突覆盖前先删旧记录 | FULL | `id` (Number, **required**, min=1) | Long |
| 7 | `listBehaviors` | 按时间范围查询行为记录（默认今天），返回精简摘要列表 | READ | `startTime` (String, opt, ISO 8601), `endTime` (String, opt, ISO 8601) | JSON String |
| 8 | `getDailySummary` | 获取指定日期的行为统计汇总，按活动分组计算总时长；默认今天 | READ | `date` (String, opt, YYYY-MM-DD) | JSON String |

---

## ACTIVITIES (18 tools)

| # | Name | Description | Access | Parameters | Return |
|---|------|-------------|--------|------------|--------|
| 1 | `listActivities` | 列出当前所有未归档的活动，供开始计时时挑选 | READ | *(none)* | List\<Activity\> |
| 2 | `listTags` | 列出标签；默认仅未归档，可选包含归档 | READ | `includeArchived` (Boolean, opt, default=false) | List\<Tag\> |
| 3 | `createActivityCategory` | 创建活动分类（写入 ActivityGroup 表，sortOrder 自增） | WRITE | `name` (String, **required**, minLen=1, maxLen=50) | String |
| 4 | `createTagCategory` | 声明标签分类名（占位语义，未写物理表；待首个标签写入后通过 distinct 体现） | WRITE | `name` (String, **required**, minLen=1, maxLen=50) | String |
| 5 | `createActivity` | 创建活动；可指定分组名（默认'预制菜'）、图标 key、颜色（缺省自动生成莫奈色） | WRITE | `name` (String, **required**, minLen=1, maxLen=50), `groupName` (String, opt, default="预制菜"), `iconKey` (String, opt), `color` (Number, opt) | Long |
| 6 | `createTag` | 创建标签；可指定分类名（默认'预制菜'）、图标 key（默认'#'）、颜色（缺省自动生成莫奈色） | WRITE | `name` (String, **required**, minLen=1, maxLen=50), `category` (String, opt, default="预制菜"), `iconKey` (String, opt, default="#"), `color` (Number, opt) | Long |
| 7 | `batchCreateActivities` | 批量创建活动；传入活动列表，自动去重并创建不存在的活动，返回 created/skipped 结果 | WRITE | `activities` (Array, **required**) — each: name(**req**), groupName(opt), iconKey(opt), color(opt) | JSON String |
| 8 | `batchCreateTags` | 批量创建标签；传入标签列表，自动去重并创建不存在的标签，返回 created/skipped 结果 | WRITE | `tags` (Array, **required**) — each: name(**req**), category(opt), iconKey(opt), color(opt) | JSON String |
| 9 | `batchCreateActivityCategories` | 批量创建活动分类；传入分类名列表，自动去重，返回 created/skipped 结果 | WRITE | `names` (Array, **required**) | JSON String |
| 10 | `batchCreateTagCategories` | 批量声明标签分类；传入分类名列表，自动去重，返回 created/skipped 结果 | WRITE | `names` (Array, **required**) | JSON String |
| 11 | `batchDeleteBehaviors` | 批量删除行为记录，同时清理标签关联，返回已删除和未找到的 ID 列表 | FULL | `ids` (Array, **required**) | JSON String |
| 12 | `batchDeleteActivities` | 批量删除活动（含关联行为记录和标签绑定），返回已删除和未找到的 ID 列表 | FULL | `ids` (Array, **required**) | JSON String |
| 13 | `bulkUpdateActivities` | 批量修改活动属性（分组、图标、颜色、名称），每个活动可独立指定不同字段，仅更新传入字段 | WRITE | `updates` (Array, **required**) — each: id(**req**), groupId(opt), iconKey(opt), color(opt), name(opt) | JSON String |
| 14 | `setBehaviorTag` | 管理行为记录的标签关联；支持追加(add)、移除(remove)、替换(replace)三种模式 | FULL | `behaviorId` (Number, **required**, min=1), `tagIds` (Array, **required**), `mode` (String, **required**, enum: add/remove/replace) | JSON String |
| 15 | `exportData` | 按日期范围导出行为、活动、标签数据为 JSON 格式 | READ | `startDate` (DateTime, **required**), `endDate` (DateTime, **required**) | JSON String |
| 16 | `importData` | 导入 JSON 格式数据，支持 SMART（合并）和 OVERWRITE（覆盖）两种模式 | WRITE | `data` (String, **required**), `mode` (String, **required**, enum: SMART/OVERWRITE) | JSON String |
| 17 | `selectActivitiesAndTags` | 选择（token 精确相等）：输入「计划」只命中名为计划的活动与 keywords 含计划的标签 | READ | `query` (String, **required**, minLen=1, maxLen=100), `useRegex` (Boolean, opt), `caseSensitive` (Boolean, opt), `scope` (String, opt, enum: activities/tags/both), `includeArchived` (Boolean, opt) | Map |
| 18 | `searchActivitiesAndTags` | 搜索（substring 子串）：输入「天」会同时命中今天/明天/后天；适合搜索框 | READ | `query` (String, **required**, minLen=1, maxLen=100), `useRegex` (Boolean, opt), `caseSensitive` (Boolean, opt), `scope` (String, opt, enum: activities/tags/both), `includeArchived` (Boolean, opt) | Map |

---

## STATISTICS (0 tools)

*(no tools implemented)*

## GOALS (0 tools)

*(no tools implemented)*

## REMINDERS (0 tools)

*(no tools implemented)*

## SETTINGS (0 tools)

*(no tools implemented)*

---

## Notes

- All batch tools have a `MAX_BATCH_SIZE = 20` limit.
- `batchDeleteBehaviors` and `batchDeleteActivities` use `AccessLevel.FULL` (destructive ops).
- `setBehaviorTag` uses `AccessLevel.FULL` (modifies tag associations).
- `deleteBehavior` (single) also uses `AccessLevel.FULL`.
- `exportData` is `READ` despite writing output — it only reads data.
- `importData` uses `WRITE` with two modes: `SMART` (merge, skip existing) and `OVERWRITE` (clear then replace).
- `createTagCategory` is a stub/placeholder — Tag.category is a string field, no physical TagCategory table.
- `ListActivitiesTool` is categorized as `ACTIVITIES` (not `TIMING`).
