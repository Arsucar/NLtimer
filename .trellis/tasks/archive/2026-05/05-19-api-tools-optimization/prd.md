# API 工具新增与优化：批量创建、行为查询、日结统计

## Goal

基于 AI Agent 在实际对话中的使用痛点，新增和优化 ToolDefinition 工具，让 AI Agent 能在 1-2 轮工具调用内完成常见操作（批量创建、历史查询、日结汇总），而不是 5+ 轮循环单条调用或直接告知"无法完成"。

## 痛点来源（从对话历史提取）

1. **无法查询历史行为** — 用户问"总结今天的行为"时，AI 只能 `queryCurrentBehavior` 返回当前 ACTIVE 记录，无法按日期范围拉取已完成的行为列表，直接告知用户"没有这个接口"。
2. **无法批量创建活动** — 用户想一次性创建多个活动时，AI 只能逐条 `createActivity`，浪费工具调用轮次。
3. **无法获取统计汇总** — 用户想看"今天各活动花了多少时间"，没有现成接口，AI 无法直接回答。

## 已有代码能力（可复用）

- `BehaviorRepository.getByDayRange(dayStart, dayEnd)` — 已有按天查询 Flow
- `BehaviorRepository.getBehaviorsWithDetailsByTimeRange(startTime, endTime)` — 已有时间范围查详情
- `BehaviorRepository.getBehaviorsWithDetailsByTimeRangeSync(startTime, endTime)` — 同步版本
- `BehaviorWithDetails` — 包含 behavior + activity + tags 的完整模型
- `BehaviorRepository.getTotalDurationAllBehaviors()` — 总时长
- Hilt `@Binds @IntoSet` 注册模式 — 新增工具只需新建类 + Module 加一行

## Requirements

### R1: 新增 `listBehaviors` 工具

按时间范围查询行为记录，返回**精简摘要**列表（不暴露 pomodoroCount/sequence/estimatedDuration 等内部字段，减少 AI token 消耗）。

**参数**：
- `startTime` (STRING, optional, default=今天 00:00 本地时区) — 起始时间 ISO 8601
- `endTime` (STRING, optional, default=今天 23:59:59 本地时区) — 结束时间 ISO 8601

**返回**：精简摘要列表 JSON：
```json
[
  {
    "id": 42,
    "activityId": 7,
    "activityName": "本职工作",
    "iconKey": "💼",
    "startTime": "2026-05-19T09:00:00+08:00",
    "endTime": "2026-05-19T12:30:00+08:00",
    "durationMinutes": 210,
    "status": "COMPLETED",
    "note": "需求评审",
    "tags": ["后端", "重点"]
  }
]
```

**实现**：
- 调用 `BehaviorRepository.getBehaviorsWithDetailsByTimeRangeSync()` 获取原始数据
- 映射为精简 DTO（只保留 AI/用户关心的字段）
- durationMinutes 由 `endTime - startTime` 计算（ACTIVE 用 `now - startTime`），除以 60000 取整
- tags 只取 `name` 列表

**约束**：
- 查询范围上限 31 天，超出返回 ValidationError
- 默认查询今天（无参数时使用 today 00:00 ~ 23:59:59）
- 时间解析复用 `RecordBehaviorTool` 中的 `parseIsoToMillis` 逻辑（提取为共享工具函数）

### R2: 新增 `getDailySummary` 工具

按日期返回行为统计汇总，按活动分组计算总时长。

**参数**：
- `date` (STRING, optional, default=today) — 日期，格式 `YYYY-MM-DD`

**返回**：结构化 JSON：
```json
{
  "date": "2026-05-19",
  "totalDurationMinutes": 320,
  "activities": [
    {
      "activityId": 7,
      "activityName": "本职工作",
      "iconKey": "💼",
      "durationMinutes": 240,
      "behaviorCount": 3
    },
    {
      "activityId": 4,
      "activityName": "主动学习",
      "iconKey": "📖",
      "durationMinutes": 80,
      "behaviorCount": 2
    }
  ],
  "activeBehavior": { ... }  // 当前进行中的行为（如果有）
}
```

**实现**：
- 复用 `getBehaviorsWithDetailsByTimeRangeSync` 获取数据
- 在内存中按 activityId 分组累加时长
- COMPLETED 行为用 `endTime - startTime`；ACTIVE 行为用 `now - startTime`
- 时长转换分钟取整

### R3: 新增 `batchCreateActivities` 工具

批量创建活动，一次调用创建多条。

**参数**：
- `activities` (ARRAY, required) — 活动列表，每个元素包含：
  - `name` (STRING, required)
  - `groupName` (STRING, optional, default="预制菜")
  - `iconKey` (STRING, optional)
  - `color` (NUMBER, optional)

**返回**：结构化 JSON：
```json
{
  "created": [
    {"id": 14, "name": "阅读"},
    {"id": 15, "name": "跑步"}
  ],
  "skipped": [
    {"name": "编程", "reason": "活动已存在"}
  ]
}
```

**逻辑**：
- 遍历列表，逐条调用 `ActivityRepository.getByName` 查重 + `insert` 创建
- 分组不存在则自动创建（复用 `CreateActivityTool.ensureActivityGroupId` 逻辑）
- 不做事务性（部分成功部分失败也返回已创建部分）
- 单次上限 20 条

### R4: 提取共享时间解析工具

将 `RecordBehaviorTool.parseIsoToMillis` 和 `formatIso` 提取为顶层工具函数，放在 timing 包下的 `TimeUtils.kt`，供 `listBehaviors`、`getDailySummary` 和 `RecordBehaviorTool` 共用。

## Acceptance Criteria

- [ ] `listBehaviors` 能按时间范围返回行为列表（含活动名 + 标签）
- [ ] `getDailySummary` 能返回按活动分组的时长统计
- [ ] `batchCreateActivities` 能一次创建多条活动并返回结果
- [ ] 时间解析函数已提取为共享工具
- [ ] 新工具在对应 `*ToolsModule` 中注册
- [ ] 编译通过，无 lint 错误

## Definition of Done

- 代码编译通过
- Lint / typecheck green
- 新工具在 `TimingToolsModule` / `LibraryToolsModule` 正确注册
- ToolDefinition 的 getDocumentation() 返回完整文档

## Out of Scope

- 批量创建标签（`batchCreateTags`）— 留到下一迭代
- 批量结束/删除行为 — 留到下一迭代
- recordBehaviorWithOverwrite 冲突覆盖 — 留到下一迭代
- 统一 search/select 接口 — 留到下一迭代
- UI 层修改 — 本次只改 Tool 层

## Technical Notes

### 文件影响清单

**新增文件**：
- `core/tools/src/main/java/com/nltimer/core/tools/timing/ListBehaviorsTool.kt`
- `core/tools/src/main/java/com/nltimer/core/tools/timing/GetDailySummaryTool.kt`
- `core/tools/src/main/java/com/nltimer/core/tools/timing/TimeUtils.kt`
- `core/tools/src/main/java/com/nltimer/core/tools/library/BatchCreateActivitiesTool.kt`

**修改文件**：
- `core/tools/src/main/java/com/nltimer/core/tools/timing/TimingToolsModule.kt` — 注册 2 个新工具
- `core/tools/src/main/java/com/nltimer/core/tools/library/LibraryToolsModule.kt` — 注册 1 个新工具
- `core/tools/src/main/java/com/nltimer/core/tools/timing/RecordBehaviorTool.kt` — 改用 TimeUtils

### 注册模式

每个新工具在对应 Module 中加一行 `@Binds @IntoSet`：
```kotlin
@Binds @IntoSet
abstract fun bindListBehaviors(tool: ListBehaviorsTool): ToolDefinition
```

### Repository 方法复用

- `listBehaviors` → `BehaviorRepository.getBehaviorsWithDetailsByTimeRangeSync()`
- `getDailySummary` → `BehaviorRepository.getBehaviorsWithDetailsByTimeRangeSync()` + `getCurrentBehavior()`
- `batchCreateActivities` → `ActivityRepository.getByName()` + `insert()` + `CategoryRepository.addActivityCategory()`
