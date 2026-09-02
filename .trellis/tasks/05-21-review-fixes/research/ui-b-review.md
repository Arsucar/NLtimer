# UI-B 模块审查报告（第 4 路）

> 审查范围：feature/stats、feature/settings、feature/management_activities、feature/categories、feature/tag_management、feature/behavior_management
> 生成时间：2026-08-01
> 原则：确定性高、风险低 → 直接修复；复杂重构/公共 API 变更 → 只记录不改

---

## feature/settings

### P1（已修复）

- `ui/IconMissLogScreen.kt:79-104` - **双重 TopAppBar（嵌套 Scaffold）**：`ICON_MISS_LOG` 在 `NLtimerRoutes.SETTINGS_FULLSCREEN_ROUTES` 中，外层 `NLtimerScaffold` 已为 secondary page 渲染带返回箭头的 TopAppBar；该路由又自带 `Scaffold + TopAppBar("图标库")`，导致两个顶栏叠加。符合 `docs/agent/06-common-bug.md` #1 模式。
  - **修复**：移除内层 Scaffold/TopAppBar/SnackbarHost（snackbarHostState 从未被使用），将"复制日志/清除日志"按钮移入内容头部 Row；参数 `onNavigateBack` 改名 `_onNavigateBack` 并同步更新 `app/.../NLtimerNavHost.kt` 调用处。
- `app/.../NLtimerScaffold.kt:103-118` - **secondary page 顶栏标题缺失**：DATA_MANAGEMENT / HOME_LAYOUT_CONFIG / COLOR_PALETTE / ICON_MISS_LOG / ADVANCED_SETTINGS / LOG_LIST 落入 `else -> visibleDateLabelState.value ?: "NLtimer"`，若用户先访问过首页会显示残留的日期标签。
  - **修复**：为这些路由补充固定标题（数据管理 / 主页布局配置 / 色板 / 图标库 / 高级 / 日志记录）。
- `ui/DataManagementViewModel.kt:60,77,91,141,158` - **SwallowedException（detekt）**：多处 `catch (e: Exception)` 吞异常无日志，问题定位困难。
  - **修复**：在各 catch 中补充 `Log.e("DataManagement", ...)`。

### P2（记录不改）

- `ui/DataManagementScreen.kt:40,113-114` - 参数 `_onNavigateBack` / `_isExporting` / `_isImporting` 未使用（公共 Compose API，不改签名）。
- `ui/DataManagementScreen.kt:92` - 内层 Scaffold 无 topBar，contentPadding top=0，无双顶栏；仅用于 SnackbarHost，可接受。
- `ui/ThemeSettingsViewModel.kt:45-49` - `updateTheme` 用 `getThemeFlow().first()` 读取再写入，快速连续切换时可能丢失中间状态（DataStore 写入在途、第二次读取拿到旧值）。读取 `.value` 同样有 WhileSubscribed 过期问题。低风险方案需重构，仅记录。
- `ui/ThemeSettingsScreen.kt:159,232` - `ThemeSettingsScreen`/`ThemeSettingsContent` LongParameterList(25) + LongMethod(257)（detekt），属公共 API，重构成本高，仅记录。
- `ui/ThemeSettingsScreen.kt:884,893,902` - `String.format` 缺 Locale（detekt ImplicitDefaultLocale）。
- `ui/AdvancedSettingsScreen.kt:30` / `ui/LogListScreen.kt:31` - `_onNavigateBack` 未使用（约定下划线前缀，保留）。

---

## feature/stats

### P1（已修复）

- `viewmodel/StatsViewModel.kt:60-62` - `catch (_: Exception)` 会吞掉 `CancellationException`：快速切换时间范围时，flatMapLatest 取消旧 flow，被取消的 query 抛 CancellationException 被当成查询失败，污染状态并可能短暂误置 isLoading=false。
  - **修复**：单独 `catch (e: CancellationException) { throw e }`，再 `catch (_: Exception) { null }`。
- `ui/component/TrendCard.kt:108-118` - `android.graphics.Paint()` 在 Canvas 绘制循环内逐标签创建，每次 draw 都分配对象，动画期间（Animatable 800ms 每帧）反复 GC。
  - **修复**：将 Paint 提升到循环外复用。
- `ui/component/BarChartCard.kt:130-140` - 同上，Paint 在循环内创建。
  - **修复**：提升到循环外；alpha 改用 `labelPaint.alpha` 更新。

### P2（记录不改）

- `viewmodel/StatsViewModel.kt:60-62` - 查询失败被吞掉且 UiState 无 error 字段，UI 会显示误导性的"暂无数据"。建议后续加 `isError` 状态（涉及 UiState/UI 变更，仅记录）。
- `viewmodel/StatsViewModel.kt` - `statsResult` 用 `WhileSubscribed(5000)` 但 init 里常驻 collect，后台也会执行查询；数据量小时影响小。
- `ui/StatsScreen.kt:47,118` / `ui/component/MetricCard.kt:47,141` / `ComparisonCard.kt:41,47` / `CategoryShareCard.kt:88,104` / `ActivityRankSection.kt:118` - `String.format` 缺 Locale。
- `ui/component/StatsGridContainer.kt:116` - 拖拽阈值硬编码 `itemHeight = 180f`，与真实面板高度（如单指标 120dp）不一致，长面板拖拽手感偏差（仅记录）。

---

## feature/behavior_management

### P1（已修复）

- `viewmodel/BehaviorManagementViewModel.kt:356` - `BehaviorNature.entries.first { it.key == item.status }`：导入文件 status 字段未知值（如手改/损坏 JSON）会抛 NoSuchElementException，导入中断且无反馈。
  - **修复**：改用 `firstOrNull() ?: BehaviorNature.COMPLETED` 兜底。
- `viewmodel/BehaviorManagementViewModel.kt:102-104` - observeBehaviors 的 `.catch` 静默清空列表，无日志。
  - **修复**：补充 `Log.e`。

### P2（记录不改）

- `viewmodel/BehaviorManagementViewModel.kt:255,271` - `catch (Exception)` 属 IO/解析边界，宽泛捕获可接受；但 `importFromJson`/`exportToJson` 失败仅 Log，用户无反馈（需新增 UiState 字段，仅记录）。
- `viewmodel/BehaviorManagementViewModel.kt:162` - MagicNumber `3`/`7` 等（detekt，低优先）。
- `ui/BehaviorManagementScreen.kt:50` - LongMethod(216)（detekt）。
- `ui/FilterBar.kt:52` - LongParameterList(13)（detekt）。
- `ui/TimeRangeSelector.kt:47` - LongMethod(139)（detekt）。
- `ui/BehaviorManagementRoute.kt:15` - `_onNavigateBack` 未使用（保留）。

---

## feature/management_activities

### 已检查无问题

- `ui/ActivityManagementScreen.kt:294` - `ManagementActivityItem.iconKey = activity.iconKey` **正确**，未硬编码 null（对照 06-common-bug.md #3）。
- `ui/components/ActivityDetailSheet.kt` - 无嵌套 Scaffold（ModalBottomSheet），`_allGroups` 未使用但为公共 API 保留。

### P2（记录不改）

- `viewmodel/ActivityManagementViewModel.kt:101-105` - `expandedGroupIds` 只增不减：分组删除后 ID 残留集合；新分组自动展开是预期行为，但旧 ID 永远不清。影响极小（Room AUTOINCREMENT 一般不复用 ID）。
- `ui/ActivityManagementScreen.kt:89` - LongMethod(182)（detekt）。
- `ui/components/ActivityDetailSheet.kt:51` - 参数 `_allGroups` 未使用（detekt UnusedParameter）。
- `ui/components/dialogs/ActivityFormSheets.kt:140` - `EditActivityFormSheet` LongMethod(124)（detekt）。

---

## feature/categories

### 已检查无问题

- `ui/CategoriesScreen.kt:167-178` - `CategoryCategorizableItem.iconKey = null`：分类本身无图标源且 `showItemIcon=false`，**非 bug**（对照 06-common-bug.md #3 检查清单：源对象无 iconKey 时允许 null）。
- `viewmodel/CategoriesViewModel.kt` - 分类合并/冲突检测逻辑正确；`_renameConflict` 一次性状态使用 SharedFlow 语义的 MutableStateFlow 手动清空，可接受。

### P2（记录不改）

- `ui/CategoriesScreen.kt:53,58` - LongParameterList(13) + `_onDeleteCategory` 未使用（公共 API 保留）。
- `viewmodel/CategoriesViewModel.kt:51` - `(tagCats + addedTag).distinct().sorted()` 每次 combine 全量重算，数据量小可接受。

---

## feature/tag_management

### 已检查无问题

- `ui/TagManagementScreen.kt:290` - `ManagementTagItem.iconKey = tag.iconKey` **正确**，未硬编码 null。
- `viewmodel/TagManagementViewModel.kt:65` - `addedCategories.filter { it in dbCategorySet || it in addedSet }` 看似冗余，但 `it in addedSet` 用于保留"已创建但尚无标签使用"的空分类，**不能删**（曾尝试简化，发现会丢失空分类，已还原）。

### P2（记录不改）

- `viewmodel/TagManagementViewModel.kt:74` - 仅在 `categories.isEmpty()` 首载时展开全部，之后新增分类不自动展开（行为设计，记录）。
- `ui/components/dialogs/EditTagFormSheet.kt:38` - CyclomaticComplexMethod(27) + LongMethod(185)（detekt）。
- `ui/components/dialogs/AddTagFormSheet.kt:22` - LongMethod(129)（detekt）。
- `ui/TagManagementScreen.kt:87` - LongMethod(178)（detekt）。
- `ui/TagManagementRoute.kt:17` - `_onNavigateBack` 未使用（保留）。

---

## 导航/壳（跨模块发现，记录）

- `app/.../NLtimerRoutes.kt:24-30` - `TAG_MANAGEMENT` 不在 `SETTINGS_FULLSCREEN_ROUTES` 中，也未在 `PRIMARY_ROUTES`。其通过抽屉/设置弹窗进入，当前 `isHomePage=true`（顶栏可能显示首页残留日期标题）、且会显示底栏。若产品意图为"抽屉直达的全屏子页"，应加入 SETTINGS_FULLSCREEN_ROUTES（需确认产品意图，仅记录）。
- `app/.../NLtimerScaffold.kt:276-287` - secondary page 的外层返回 TopAppBar 标题统一由 `topBarTitle` 决定，本次已补齐缺失标题。

---

## 修复清单汇总（本轮已落代码）

| 文件 | 修复 |
|------|------|
| feature/settings/ui/IconMissLogScreen.kt | 移除嵌套 Scaffold/TopAppBar 双重顶栏；复制/清除按钮移入内容头部 |
| app/navigation/NLtimerNavHost.kt | IconMissLogRoute 参数改名同步 |
| app/NLtimerScaffold.kt | 补齐 6 个二级页顶栏标题 |
| feature/settings/ui/DataManagementViewModel.kt | 5 处 catch 补充 Log |
| feature/stats/viewmodel/StatsViewModel.kt | CancellationException 重新抛出 |
| feature/stats/ui/component/TrendCard.kt | Paint 提升出绘制循环 |
| feature/stats/ui/component/BarChartCard.kt | Paint 提升出绘制循环 |
| feature/behavior_management/viewmodel/BehaviorManagementViewModel.kt | 导入状态未知时安全兜底 + 观察链 catch 打日志 |

> 未运行任何构建命令（按任务约束）；以上改动均为低风险局部修改，未改公共 Compose API 签名，未改 UI 文本/资源，未破坏 JsonImporter/Exporter 格式契约。
