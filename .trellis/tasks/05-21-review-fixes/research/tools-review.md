# core:tools 模块审查记录

> 路径：`core/tools/src/main/java/com/nltimer/core/tools/`
> 审查范围：ToolRegistry / ToolDefinition / timing/* / match/* / library/* / event/* / di/*
> 状态：确定性高、低风险项已直接修复；复杂/有歧义项仅记录
> 兼容性基线：工具输出经 `core/ai` 的 `AiChatToolHelper.executeToolCall` → `result.data?.toString()` 交给 LLM；UI 侧（HomeViewModel 等）不直接消费本模块工具 execute() 的返回值

---

## P0 —— bug（已修复）

1. `library/ListTagsTool.kt:53` - **listTags 返回 `List<Tag>` 领域对象而非 JSON 字符串**（违反 docs/agent/06-common-bug.md #2）。AI 拿到 `[Tag(id=1, name=专注, ...)]` Kotlin toString，与文档声明的 JSON 数组不符。
   修复：改为返回 `[{id,name,iconKey,color,category,isArchived}]` JSON 数组；`returnType` List::class → String::class。

2. `match/SearchActivitiesAndTagsTool.kt:145`、`match/SelectActivitiesAndTagsTool.kt:146` - **搜索/选择工具返回 Kotlin Map 而非 JSON 字符串**。AI toolcall 收到 `{query=天, activities=[{id=1, name=今天}]}` 这种非 JSON 文本。
   修复：改为 `JSONObject` 序列化输出（字段结构不变：query/useRegex/mode/activities/tags/matchedField）；`returnType` Map::class → String::class。
   测试同步：`src/test/.../match/MatchToolTestFixtures.kt` 的 `successData()` 改为把 JSON 字符串解析回 Map（保持既有断言不变）。

3. `match/ProcessNoteTool.kt:59` - **processNote 返回 Kotlin Map 而非 JSON 字符串**（同上）。
   修复：改为 `JSONObject` 输出（activityId/tagIds/cleanedNote/createdActivities/createdTags/matchedActivities/matchedTags）；`returnType` Map::class → String::class。

4. `timing/ActivateGoalTool.kt:64` - **`setStatus(id, BehaviorNature.ACTIVE.name)` 写入大写 "ACTIVE"，与 DAO 查询 `status = 'active'`（小写）不一致**。后果：激活的目标不会被 `getCurrentBehavior()` / `endCurrentBehavior()` 识别（查询用小写），激活后既无法被 queryCurrentBehavior 查到，也无法正常结束。
   修复：改用 `BehaviorNature.ACTIVE.key`（小写）。
   注：BehaviorRepositoryImpl / Behavior.toEntity 均用 `.key`，仅此处误用 `.name`。

5. `library/ExportDataTool.kt:33,38` - **参数类型声明 DATE_TIME，AI schema 映射为 "string"，但实现只接受 Number（epoch 毫秒）**。AI 按 schema 传字符串必然被 `(args["startDate"] as? Number)?.toLong()` 拒掉（"startDate 必填"），工具经 AI 实际不可用。
   修复：`startDate`/`endDate` 类型 DATE_TIME → NUMBER（与实现、文档示例 `startDate=1716163200000` 一致）。

---

## P1 —— 正确性/健壮性（已修复，低风险）

6. `timing/GetWeeklySummaryTool.kt:127`、`timing/GetTimeRangeSummaryTool.kt:113,127,134,143` - **String.format 无 Locale**（detekt ImplicitDefaultLocale；且部分区域小数点用逗号，JSON 数字串被污染如 "4,2"，破坏 LLM 解析）。
   修复：统一加 `Locale.US`。

7. `timing/ListBehaviorsTool.kt:77` - **未校验 endMs < startMs**：倒置区间时 `rangeDays` 为负、通过 `>31` 检查后继续查询。
   修复：先校验 `endMs <= startMs` → ValidationError。

8. `timing/GetDailySummaryTool.kt:62`、`timing/GetWeeklySummaryTool.kt:45` - **date 参数非法时 `LocalDate.parse` 抛 DateTimeParseException 被 runCatching 归一为 INTERNAL_ERROR**（错误码误导 LLM；用户输错日期应报 VALIDATION_ERROR）。
   修复：日期解析移出 runCatching，失败返回 ValidationError；顺带修复两处 `groupedStats[...]`/`activityTotals[...]` 赋值块缩进错乱。

9. `timing/UpdateBehaviorTool.kt:89-97` - **给 ACTIVE 行为设置 endTime 后不更新状态**：产生 `status=active + endTime` 不一致状态（getCurrentBehavior 查不到，但 GetDailySummary/GetTimeRangeSummary 等统计仍按"进行中"用 now-start 计算）。
   修复：设置 endTime（非空）且原状态为 ACTIVE 时，同步 `setStatus(id, BehaviorNature.COMPLETED.key)`。

---

## P2 —— 整洁/一致性（已修复）

10. `library/BulkUpdateActivitiesTool.kt:68`、`library/BulkUpdateTagsTool.kt:65` - **`JSONObject.put("id", item["id"])` 为 null 时 org.json 会移除该 key**，notFound 条目变成 `{"reason":"id 无效"}` 丢失 id，AI 无法知道哪个条目无效。
    修复：`put("id", item["id"] ?: JSONObject.NULL)`。

11. `library/SetBehaviorTagTool.kt:107-109` - **color/iconKey/category 为 null 时 put 会移除 key**，与文档示例 `"color": null` 不符（缺 key 而非 null）。
    修复：`.put("color", tag.color ?: JSONObject.NULL)` 等三处。

12. `library/BatchCreateActivitiesTool.kt:232`、`library/BatchCreateTagsTool.kt:220` - **未使用的 `MAX_BATCH_SIZE` 常量**（detekt UnusedPrivateProperty；实际走 ToolConfig.maxBatchSize）。
    修复：删除。

13. `timing/GetIconSearchMissesTool.kt:31` - **SimpleDateFormat 用 Locale.getDefault()**，部分区域（如阿拉伯语）数字本地化导致时间戳不可解析。
    修复：改 Locale.US。

14. `match/NoteMatcher.kt:47` - `val activityId = activities` 缩进错乱（不影响编译）。
    修复：调整缩进。

15. `match/SearchActivitiesAndTagsTool.kt:171`、`match/SelectActivitiesAndTagsTool.kt:172` - **正则构造 catch Exception 过宽**（detekt TooGenericExceptionCaught + SwallowedException）。`Regex(query, options)` 只可能抛 `PatternSyntaxException`（IllegalArgumentException 子类）。
    修复：精确 catch `IllegalArgumentException`（行为不变：仍包装为带消息的 IllegalArgumentException）。

---

## 记录但未改（复杂/有歧义/涉及 AI 可见行为变更，需评审后决定）

16. `timing/UpdateBehaviorTool.kt:94-96` - **endTime 语义与文档不符**：参数描述"传 null 清除"，实现却是"COMPLETED 时置为当前时间"，且 `setEndTime(id: Long)` 非空签名不支持真正清除。需要 Repository 增加 nullable endTime 支持或重定义文档；改动会改变 AI 可见行为 → 仅记录。

17. `timing/StartBehaviorTool.kt:128` - **标签关联多个活动时静默取 `activityIds.first()`**，可能不符合用户预期（用户说"开始小睡"但小睡关联多个活动时不知道选了哪个）。建议返回候选列表由 LLM 澄清 → 涉及 AI 行为，仅记录。

18. `library/SetBehaviorTagTool.kt:24,78`、`library/BatchDeleteBehaviorsTool.kt:24,69` - **工具直接注入 `BehaviorDao` 绕过 Repository 分层**（core:data 架构约定 Repository 接口→impl）。当前可用，建议在 BehaviorRepository 补齐方法后改为仅依赖 Repository → 涉及 core:data 修改，仅记录。

19. `library/BatchDeleteBehaviorsTool.kt:66` - **notFound 对无效 id 直接 `put(rawId)`（null 时输出 JSON null）**，与 `BatchDeleteActivitiesTool` 的 `{id, reason}` 结构不一致，LLM 解析体验不一致。改动影响 AI 输出结构 → 仅记录。

20. `library/CreateActivityTool.kt:139,157`、`library/BatchCreateActivitiesTool.kt:162,187`、`library/BulkUpdateActivitiesTool.kt:110` - **`ensureActivityGroupId` / `inferRelatedKeywords` 三处重复**，且 Create 版 `resolveIconKey` 不记录 icon_search_miss、Batch 版记录（行为不一致）。建议抽公共 helper（放 `library/` 或新 `internal object`），抽取属中等风险重构 → 仅记录。

21. `library/BatchCreateActivitiesTool.kt:79` - **批量创建活动未校验 name 长度**（BatchCreateTagsTool 有 50 字上限），且参数声明无约束。补校验属于行为变更 → 仅记录。

22. `timing/GetDailySummaryTool.kt:73`、`timing/GetWeeklySummaryTool.kt:62`、`timing/ListBehaviorsTool.kt:74` - **日结束边界用 23:59:59 而非下一日 00:00:00**，末秒内结束的行为可能漏统计。改动会改变统计口径 → 仅记录。

23. `tools/ToolConfig.kt:15` - **maxBatchSize 注释声称 1–200 但无写入校验**（setter 可被 app 层写成任意值）。补校验需明确 app 层写入点 → 仅记录。

24. `tools/ToolRegistry.kt:82` - **未注册工具的事件 category 硬编码 SEARCH**（语义不准确，ToolCategory 无 UNKNOWN）。枚举扩展属公共 API 变更 → 仅记录。

25. `timing/GetTimeRangeSummaryTool.kt:63`、`timing/ListBehaviorsTool.kt:78` - **`rangeDays` 用整数除法**：如 31 天 + 数小时区间取整为 31 通过上限检查（轻微超限漏检）。改为浮点或毫秒比较更严格 → 影响错误信息文案，仅记录。

---

## 兼容性影响标注（feature/ai）

- **变更 #1/#2/#3（ListTags/Search/Select/ProcessNote 输出 → JSON 字符串）**：`AiChatToolHelper.executeToolCall` 对 Success 统一 `data?.toString()`。修复前这些工具输出 Kotlin Map/List toString（非合法 JSON），修复后为合法 JSON 字符串 —— 属严格改进，AI 端解析更好；无 UI 调用方受影响（UI 走 Repository / HomeViewModel.processNote，不经过工具）；Debug ToolConsole 仅 `data.toString()` 展示，不受影响。
- **变更 #4（ActivateGoalTool 状态写入小写）**：修复前激活目标不被查询识别（隐性 bug），修复后符合设计语义。AI 流程 `listGoals → activateGoal → queryCurrentBehavior` 链路由不可用变为可用。
- **变更 #9（UpdateBehaviorTool 补 COMPLETED 状态）**：仅影响"给 ACTIVE 行为设置 endTime"路径，消除不一致状态，不改变正常参数（note/startTime/tagIds）行为。
