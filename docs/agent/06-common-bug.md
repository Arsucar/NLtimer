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
