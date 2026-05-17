# AI 工具扩展实施计划

**日期**：2026-05-17
**分支**：feature/ai-inter
**目标**：扩展 ai-inter 测试对话的可用工具集，让 LLM 能完成 7 行用例表中的全部交互。

---

## 一、背景

用户在测试对话中提出 7 行典型用例（查看标签 / 补录 / 锚点补录 / 结束计时 / 复合多步 / 增加活动 / 增加标签）。
现有 ToolRegistry 仅注册 3 个工具（listActivities / queryCurrentBehavior / startBehavior），缺口较大。

---

## 二、关键架构事实（已核实）

| 事实 | 含义 |
|---|---|
| `BehaviorNature` 含 `PENDING / ACTIVE / COMPLETED` | Goal = PENDING 状态 Behavior，**不需要独立表** |
| `AddBehaviorUseCase` 已实现冲突检测 + 时间吸附 + sequence | **所有写入类工具必须走它**，不绕过 |
| `Tag.category: String?` 是纯字符串字段 | 没有独立 TagCategory 表，`createTagCategory` 只能"占位语义" |
| `Activity.groupId: Long?` 关联 `ActivityGroup` 表 | `createActivityCategory` 走 `CategoryRepository.addActivityCategory` |
| `IconKeyResolver` 用 `mi:style:name` 前缀 | emoji 字符串可直接存进 iconKey |
| `BehaviorRepository.getBehaviorsOverlappingRange` 已存在 | 冲突回报二次查询用它 |
| `ToolResult.Success.data?.toString()` 是当前给 LLM 的格式 | 保持一致，M5 再考虑统一 Json 化 |

---

## 三、最终工具清单（9 个）

| # | 工具名 | 文件 | 关键参数 | 落地 |
|---|---|---|---|---|
| 1 | `listTags` | `library/ListTagsTool.kt` | `includeArchived: Boolean=false` | `tagRepository.getAll()` 或 `getAllActive()` |
| 2 | `endBehavior` | `timing/EndBehaviorTool.kt` | — | `behaviorRepository.endCurrentBehavior(now)` |
| 3 | `recordBehavior` | `timing/RecordBehaviorTool.kt` | `activityId, startTime(ISO), endTime(ISO), tagIds[]?, note?` | `AddBehaviorUseCase(status=COMPLETED)`；冲突 → 二次查询 → JSON message |
| 4 | `createGoal` | `timing/CreateGoalTool.kt` | `activityId, tagIds[]?, estimatedDurationMinutes?, note?` | `AddBehaviorUseCase(status=PENDING)` |
| 5 | `deleteBehavior` | `timing/DeleteBehaviorTool.kt` | `id` | `behaviorRepository.delete(id)` |
| 6 | `createActivity` | `library/CreateActivityTool.kt` | `name, groupName?="预制菜", iconKey?, color?` | groupName 缺/不存在 → `ensurePresetActivityGroup`；color 缺 → `RandomMonetColor.next()` |
| 7 | `createTag` | `library/CreateTagTool.kt` | `name, category?="预制菜", iconKey?="#", color?` | category 直接写字符串字段；color 缺 → 同上 |
| 8 | `createActivityCategory` | `library/CreateActivityCategoryTool.kt` | `name` | `categoryRepository.addActivityCategory(name)` |
| 9 | `createTagCategory` | `library/CreateTagCategoryTool.kt` | `name` | `categoryRepository.addTagCategoryStub(name)`（**占位语义**：不写物理表，等首个 tag 写入后从 distinct 自然出现） |

**默认值约定**：
- "预制菜"在活动侧（ActivityGroup 表）和标签侧（Tag.category 字段）**同名共用**，物理上互不干扰。
- 颜色：工具内确定性生成莫奈中和色（HSL：S=0.55, L=0.62，H 随机）。
- 标签默认图标：字面量 `"#"`。

---

## 四、辅助文件

| 文件 | 内容 |
|---|---|
| `core/tools/library/RandomMonetColor.kt` | `fun next(): Long` — HSL→ARGB |
| `core/tools/library/PresetGroup.kt` | `suspend fun ensurePresetActivityGroup(repo, name="预制菜"): Long` |
| `core/tools/library/LibraryToolsModule.kt` | Hilt `@IntoSet` 绑定 5 个 library 工具 |
| `core/tools/timing/TimingToolsModule.kt`（**改**） | 新增 4 个 timing 工具的绑定 |
| `core/data/CategoryRepository.kt`（**改**） | 新增 `suspend fun addTagCategoryStub(name: String): Boolean` |

---

## 五、冲突回报格式（核心约定）

`RecordBehaviorTool` 内部流程：
1. 调 `AddBehaviorUseCase`
2. 若返回 `Result.Conflict`，**再查一次** `behaviorRepository.getBehaviorsOverlappingRange(start, end).first()`
3. 构造 JSON 写入 `ToolError.ValidationError.message`：
   ```json
   {
     "code": "CONFLICT",
     "conflicts": [
       {"id":42,"activityId":3,"activityName":"工作",
        "startTime":"2026-05-17T10:00:00+08:00",
        "endTime":"2026-05-17T13:30:00+08:00"}
     ]
   }
   ```

---

## 六、系统提示词补充（4 段）

写入 `AiInterViewModel.systemPrompt`：

1. **时间解析规则**
   - "10 点到 14 点" → 当天本地时区 10:00–14:00 ISO 字符串
   - "15 点午休 30 分钟" → 起 15:00，止 15:30
   - "现在结束" → `endBehavior()` 不传时间

2. **预制菜兜底**
   - 用户没指定分类时，工具默认值"预制菜"，不要主动追问

3. **多步序列分支**（食堂吃饭 + 散步 10 分钟）
   ```
   先 queryCurrentBehavior：
     有 ACTIVE → 全部走 createGoal（按序入队）
     无 ACTIVE → 第 1 个 startBehavior，剩下 createGoal
   ```

4. **冲突分支**
   ```
   recordBehavior 返回 ValidationError 且 message 含 "CONFLICT" →
     解析 JSON，复述冲突区间，给三个选项：
       覆盖 → 对每个冲突 id 调 deleteBehavior，再重发 recordBehavior
       取消 → 不动
       调整时间 → 让用户给新时间
   ```

---

## 七、推进里程碑

| 里程碑 | 范围 | 验证 |
|---|---|---|
| **M1** | `listTags` + Hilt 绑定 + 编译通过 | 用 LLM 问"查看所有标签"能调通 |
| **M2** | `RandomMonetColor` / `PresetGroup` / 4 个 create 工具 / `addTagCategoryStub` | LLM 能 createActivity + createTag + createActivityCategory + createTagCategory |
| **M3** | `endBehavior` | LLM 说"结束计时"能落地 |
| **M4** | `recordBehavior` + `createGoal` + `deleteBehavior` + 冲突 JSON | 跑 7 行用例第 2、3、5 行 |
| **M5** | 4 段系统提示词写入 + few-shot 示例 + 跑全 7 用例 | 全部用例通过 |

每个里程碑都可独立编译运行。

---

## 八、不引入的事

- 不改 `AddBehaviorUseCase`（保持现有调用方稳定）
- 不改 `BehaviorNature` 枚举
- 不引入新 `ToolError` 子类（冲突复用 `ValidationError`，靠 message 内 JSON 区分）
- 不引入新 `ToolCategory` 枚举值（library 类工具暂用 `ACTIVITIES`）
- 不动 Tag schema（保持 category: String?）
- 不引入工具结果统一 Json 化（保留给 M5 之后单独评估）
