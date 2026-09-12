# 常见 Bug 记录

## 1. 嵌套 Scaffold 导致顶部空白间距

**出现时机**：页面被外层 `NLtimerScaffold` 包裹，内部又自行套了一层 `Scaffold`。

**典型场景**：
- 新增子页面时参考了独立页面模板（自带 Scaffold），但忘记外层已有 Scaffold 提供 topBar padding。
- 常见于 `AiInter` 子页面、Settings 子路由等二级页面。

**现象**：顶部栏与列表内容之间出现一段无法解释的空白间距，大小约等于一个 TopAppBar 高度。

**避免方式**：
- 子页面（通过 NavHost 嵌入 NLtimerScaffold 的）**不要再套 Scaffold**，直接用 `LazyColumn` / `Column` 等布局。
- 若确实需要内层 Scaffold（如需要内层 snackbarHost / bottomBar），则 `contentPadding` 的 `top` 不要再加 `padding.calculateTopPadding()`，因为外层已通过 `Modifier.padding(top = padding.calculateTopPadding())` 施加过。

**若已有则参考修复方式**：
1. 移除内层 `Scaffold`，将其 `content` lambda 内容直接提升一级。
2. `LazyColumn` 的 `contentPadding.top` 改为固定值（如 `12.dp`），不再叠加 `padding.calculateTopPadding()` 和 `LocalImmersiveTopPadding`。
3. 删除不再需要的 `Scaffold` 和 `LocalImmersiveTopPadding` import。

## 2. 工具返回值必须是 JSON 字符串

**出现时机**：新工具实现 `execute()` 时直接返回领域模型对象。

**现象**：AI 拿到的是 `Behavior@abc123` 或 `Activity@def456` 这样的 toString()，无法理解内容，导致回复无意义或反复追问。

**避免方式**：
- 所有工具的 `execute()` 返回值必须是 `String` 类型（`ToolResult.Success(name, jsonString)`）。
- 使用 `org.json.JSONObject` / `JSONArray` 构建结构化 JSON。
- 复杂查询结果应包含关联信息（活动名、标签列表、计算后的时长等），不要只返回 ID。

**已有案例**：
- `QueryCurrentBehaviorTool` 曾直接返回 `Behavior?` 对象，已重构为返回含 activityName/tags/durationMinutes 的 JSON。
- `ListActivitiesTool` 曾返回 `List<Activity>` 原始列表，已重构为含 groupName/tags 的 JSON 数组。

## 3. CategorizableItem.iconKey 硬编码为 null

**出现时机**：新增或修改 `CategoryPickerModels` 中的 `CategorizableItem` 实现类。

**现象**：在 CategoryPickerDialog 或 CategoryGroupCard 中选择项时，图标显示为 ❓ 或占位符。

**典型场景**：
- 新写 `data class XxxCategorizable : CategorizableItem` 时忘记传递 `iconKey`
- `TagCategorizable.iconKey` 曾硬编码为 `null` 而不是取 `tag.iconKey`

**避免方式**：
- 实现 `CategorizableItem` 时必须检查 `iconKey` 是否从源对象传递，而非硬编码 `null`
- 在 `ActivityGroupCategorizable` 等新类中确保 `iconKey = group.iconKey`

**检查清单**：
- [ ] `iconKey` 是否从领域模型传递而非硬编码
- [ ] 若源对象无 iconKey，`CategoryGroupCard.ItemChip` 会自动跳过图标（`showIcon && item.iconKey != null`）

## 4. 缺失sdk
**典型场景**
- 编译构建
**若已有报错则参考修复方式**：
- 请手动指定文件 `local.properties` 中添加 `sdk.dir=D\:\\por\\10_Library\\App_AndroidStudio_sdk`

## 5. BehaviorNature 写入 DB 必须用 `.key` 而非 `.name`

**出现时机**：工具或 Repository 调用 `setStatus(id, status)` / 写 status 字符串时。

**现象**：`BehaviorNature.ACTIVE.name` 写入 `"ACTIVE"`（大写），但 DAO 查询使用小写 `'active'`；激活后的目标对 `getCurrentBehavior` / `endBehavior` 不可见。

**避免方式**：
- 统一使用 `BehaviorNature.xxx.key`（或项目约定的小写 key）写入与比较
- 禁止 `enum.name` 直接落库

## 6. ActivityTagBinding 的 source 方向

**出现时机**：导入/创建活动侧标签绑定时。

**现象**：默认 `source="tag"` 时，活动侧查询（`getByActivityId` / `getTagIdsForActivitySync` 过滤 `source='activity'`）读不到绑定，导入后活动标签在 UI/工具中静默消失。

**避免方式**：
- 活动侧绑定必须显式 `source="activity"`
- 标签侧绑定使用 `source="tag"`
- 导入四条路径（SMART/REPLACE 等）需统一设置

## 7. Flow catch 不可吞掉 CancellationException

**出现时机**：ViewModel 中 `flow.catch { }` 或 `try/catch (Exception)` 包住 `flatMapLatest` / 可取消协程。

**现象**：快速切换时间范围等场景下，取消信号被吞掉，旧任务继续更新 UI 或异常状态。

**避免方式**：
```kotlin
catch (e: Exception) {
    if (e is CancellationException) throw e
    // 处理业务异常
}
```

## 8. 归档必须同时写 `archivedAt`

**出现时机**：`setArchived` 或编辑表单 `update()` 只改 `isArchived`。

**现象**：归档区按 `archivedAt DESC` 排序时全部挤在一起；无法区分归档先后。

**避免方式**：
- DAO：`archivedAt = CASE WHEN :archived THEN :now ELSE NULL END`
- 归档确认弹窗：`archiveActivity` / `archiveTag` 同时写 `isArchived=true`、`archivedAt=now`、`archiveNote`
- 编辑表单「保存」不得改 `isArchived` / `archivedAt` / `archiveNote`

## 9. 根据文档同步器解析 Entity 标题为精确匹配

**出现时机**：更新 `docs/agent/05-data-model.md` 新增 Entity 小节后运行 `python scripts/check-docs-sync.py`。

**现象**：字段表格明明写全，校验仍报「文档中无字段记录」。

**避免方式**：
- Entity 小节标题必须是 `### EntityName` 精确格式，禁止 `### EntityName（v17，说明…）` 之类后缀——附加说明写在标题下的 `>` 引用行
- 每个字段独占一行表格行；不要写 `createdAt / updatedAt` 合并行
- 更新后必须重新运行 `python scripts/check-docs-sync.py` 直至全绿

## 10. MockK 无法 mock Room inline `withTransaction` 非 Unit 返回

**出现时机**：RepositoryImpl 在 `database.withTransaction { return Long/Map/... }` 内做写库，然后 JVM 单测直接调用。

**现象**：`@Ignore`（14 处先例）或 `ClassCastException`；`coEvery { db.withTransaction(...) }` 只对 `suspend () -> Unit` 形态可通过 `(args[1] as suspend () -> Unit).invoke()` 放行。

**避免方式**：
- 新增 Repository 事务方法尽量声明返回 `Unit`
- 非 Unit 事务路径：拆出独立可测的普通 suspend 方法，或接受仅仪器测试覆盖
- 已有范式见 `BehaviorRepositoryImplTest`（`mockkStatic("androidx.room.RoomDatabaseKt")` + `coAnswers`）

## 11. DataStore 不可在 `data.collect` 内部 `edit`

**出现时机**：从 Preferences Flow 读到旧值后，想在同一 collect 回调里 `dataStore.edit { }` 写回（例如默认值迁移）。

**现象**：DataStore 单写者互斥锁未释放，`edit` 永久挂起；订阅方拿不到后续 emit，看起来像无限循环或卡死。

**避免方式**：
- 先在 `onStart { }` 里 `data.first()` 读一次，再 `edit` 写回；随后的 `data.map` 只负责映射
- 或用 `PreferenceDataStoreFactory` 的 `DataMigration`
- 禁止 `data.collect { dataStore.edit { ... } }` / `flow { data.collect { edit(); return@collect } }`

**已有案例**：`SettingsPrefsImpl.getFocusCardConfigFlow` 把旧默认 260 迁到 280 时，写回必须放在 `onStart`，不能放在 `data.collect` 内部。

## 12. Room `IN (:emptyList)` / `NOT IN (:emptyList)` 空列表不删

**出现时机**：差量删除用 `id NOT IN (:keepIds)`，调用方传入空列表（全部换成新字段，或清空字段）。

**现象**：Room 把空列表展开成 `NOT IN (NULL)`，条件恒假，旧行一行不删。Kotlin `id !in emptyList()` 会删全部，内存 Fake DAO 测不出这条 SQL 语义。

**避免方式**：
- `keepIds.isEmpty()` 时走 `DELETE … WHERE templateId = :id`（已有 `deleteByTemplate`）
- Fake DAO 的 `deleteExceptIds` 应对空列表 `return`，才能复现 Room 行为

## 13. 完成补记同分钟截断会误判时间冲突

**出现时机**：完成 FAB 打开补记弹窗，开始时间预填上一条 `endTime`（含秒），用户只改结束轮或 DualTimePicker 把开始回写成整分。

**现象**：上一条结束 `10:04:37`，新记录 UI 开始显示 `10:04`，确认后报「该时间段与已有行为记录冲突」。半开区间 `[start, end)` 本身允许边界相接；误拦来自开始被截成 `10:04:00` 后 `10:04:00 < 10:04:37`。

**避免方式**：
- `userAdjustedStart` / `userAdjustedEnd` 拆开；只改结束轮不得把开始截成整分
- DualTimePicker `onTimesChanged` 仅在该侧分钟变化时回写
- 「上尾」写入真实 `prevEndTime`，不要先 `withSecond(0)`
- `TimeSnapService` 对 COMPLETED：同时钟分钟且 `newStart < prevEnd` 时吸附 `adjustedStart = prevEnd`，再跑 `hasTimeConflict`

## 14. ModalBottomSheet skipPartiallyExpanded=false 吞点击

**出现时机**：`ModalBottomSheet` 使用 `rememberModalBottomSheetState(skipPartiallyExpanded = false)`，停在 PartiallyExpanded。

**典型场景**：事件记录器 `EventSheet` 曾为了半屏上滑展开而关掉 skip。

**现象**：Sheet 可见，但 chips、按钮、输入框点击无反应。

**原因**：PartiallyExpanded 下 Material3 嵌套滚动与 sheet 拖动手势抢指针，子组件收不到点击。

**避免方式**：
- 本项目一律 `rememberModalBottomSheetState(skipPartiallyExpanded = true)`（与 `AddBehaviorSheet` 等其它 Sheet 一致）
- 不要省略 `sheetState`：`ModalBottomSheet` 默认 `rememberModalBottomSheetState()` 的 skip 为 `false`，同样会停在 PartiallyExpanded
- 需要提示可拖时保留默认 `dragHandle` 即可，上滑关闭仍可用
- 不要为了半屏展示关掉 skip
