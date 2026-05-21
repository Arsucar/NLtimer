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
