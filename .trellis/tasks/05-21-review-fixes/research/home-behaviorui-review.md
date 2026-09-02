# home-behaviorui 审查报告（UI-A 首页行为链路）

> 生成时间：2026-08-01  
> 范围：feature/home（ui/components、viewmodel、model、match、di）+ core/behaviorui（sheet/*）  
> 约定：P0=崩溃/正确性 bug；P1=性能/并发/逻辑隐患；P2=整洁/死代码  
> 状态标记：【已修复】= 本次已直接改代码；【记录】= 仅记录待评估

---

## P0 — 崩溃 / 正确性 bug

### 1. 打开添加/编辑弹窗时 PENDING 目标导致崩溃 【已修复】

- 文件：`feature/home/.../ui/HomeSheetRouter.kt:50-56`
- 问题：`existingBehaviors` 从 `momentCells` 重建 `Behavior`，对 `startEpochMs` 使用
  `?: error("startEpochMs missing for behaviorId=...")`。但 `HomeUiStateBuilder.buildMomentBehaviors`
  （HomeUiStateBuilder.kt:244）对 **PENDING 目标**（以及历史遗留 `startTime <= 0` 的非 pending 数据）
  有意置 `startEpochMs = null`。用户只要有任意目标（PENDING）存在，点击空白格/添加按钮打开弹窗
  （COMPLETED/CURRENT/TARGET 任一模式）都会走到该 derivedStateOf 并抛 `IllegalStateException` 崩溃。
- 修复：改为 `cell.startEpochMs ?: 0L` —— 与 DB 约定「PENDING 的 startTime 存 0L」
  （AddBehaviorUseCase.kt:69）一致，`hasTimeConflict` 对 `startTime <= 0L` 也安全返回 false。
- 附带说明：`requireNotNull(cell.behaviorId)` / `requireNotNull(cell.status)` 前已有 filter 保证非空，保留。

### 2. StaggeredHorizontalGrid 竖排列数为 0 时数组越界 【已修复】

- 文件：`core/behaviorui/.../sheet/AddBehaviorSheetContent.kt:209-222`、`StaggeredHorizontalGrid.kt:50-56`
- 问题：竖向布局把 `dialogConfig.activityColumnLines / tagColumnLines` 作为
  `StaggeredHorizontalGrid(maxLines = ...)`，内部 `IntArray(maxLines)` 后访问 `rowWidths[0]`。
  设置页（feature/settings DialogConfigScreen.kt:92/160）竖排时 min=1，但 SettingsPrefsImpl 加载
  prefs 时**无校验**（`prefs[actColumnLinesKey] ?: 2`），历史脏数据/手动写入 0 会直接越界崩溃；
  横向行数 0 已有 `Int.MAX_VALUE` 防护，竖列 0 却没有。
- 修复：在消费处 `coerceAtLeast(1)` 钳制（活动、标签各一处），行为不变。

---

## P1 — 性能 / 并发 / 逻辑隐患

### 1. CURRENT 模式的 existingBehaviors 包含 PENDING 目标 【已修复】

- 文件：`feature/home/.../ui/HomeSheetRouter.kt`
- 问题：CURRENT 模式用 `nonActiveBehaviors`（排除了 ACTIVE，但保留 PENDING）做冲突检测输入。
  PENDING 不参与冲突（TimeConflictUtils.kt:33 早返回），语义上空转；且 PENDING 的 `endEpochMs=null`
  让 `Behavior.endTime=null` 落入 `hasTimeConflict` 的 COMPLETED 分支时是「未结束」语义。
- 修复：构建 `existingBehaviors` 时直接 `filter { it.status != PENDING }`（CURRENT/COMPLETED/TARGET
  全部更干净）。PENDING 新值也不做冲突检测。

### 2. showEditSheet 异步回填活动导致 AddBehaviorState 重建 【记录】

- 文件：`feature/home/.../viewmodel/HomeViewModel.kt:285-311`
- 问题：`editInitialActivityId` 先置 `null`，再异步 `getBehaviorWithDetails` 回填。
  `rememberAddBehaviorState`（AddBehaviorState.kt:37-47）以 `initialActivityId` 为 remember key，
  回填瞬间重建整个 AddBehaviorState —— 异步窗口内用户已调整的标签/时间选择会丢失（note 因
  `editInitialNote` 不变而保留）；且窗口内确认按钮 disabled，体验抖动。
- 建议：给 `GridCellUiState` 增加 `activityId` 字段（模型变更，需评估跨模块影响），
  `showEditSheet` 同步携带，消除异步窗口。

### 3. 编辑 PENDING 行为无法回填预估时长 【已修复】

- 文件：`AddBehaviorSheet.kt` / `AddBehaviorSheetContent.kt` / `HomeUiState` / `HomeViewModel` / `HomeSheetRouter`
- 问题：`initialEstimatedDurationMs` 参数链完整但三个 Sheet 包装函数均未透传；HomeSheetRouter
  TARGET 分支也没传 `cell.estimatedDuration`。编辑目标时预估时长显示「未设置」。
- 修复：
  1. `AddTargetBehaviorSheet` + `BehaviorSheetWrapper` + `AddBehaviorSheetContent` 透传
     `initialEstimatedDurationMs`（默认 null，兼容既有调用方）
  2. `HomeUiState` 增加 `editInitialEstimatedDurationMs`
  3. `showEditSheet` 写入 `cell.estimatedDuration`；`hideAddSheet` / combine 保留字段清空或保留
  4. TARGET 分支传入 `uiState.editInitialEstimatedDurationMs`

### 4. ACTIVE 模式缺少「开始时间不能晚于现在」UI 校验 【已修复】

- 文件：`core/behaviorui/.../sheet/AddBehaviorSheetContent.kt`（ConfirmButtonRow）
- 问题：仅 COMPLETED 校验 `start < end`；ACTIVE 模式若用户通过时间调整把开始时间调到未来，
  `AddBehaviorUseCase.validateTimeConstraints` 返回 ValidationError「开始时间不能大于当前时间」
  → snackbar 提示，但无本地按钮态/提示，体验割裂。
- 修复：ConfirmButtonRow 增加 ACTIVE 的 `startTime.isAfter(now)` 本地 Toast（与 COMPLETED 同风格），
  文案对齐 UseCase：「开始时间不能大于当前时间」。

### 5. MomentView 加载更多触发方向疑似错误 【已修复】

- 文件：`feature/home/.../ui/components/MomentView.kt`
- 问题：Moment 列表**最新在上**（`buildMomentDisplayItems` 按日期倒序 + `TIME_DESC` 排序，
  PENDING 在最顶），更早的数据追加在**底部**；但 loadMore 触发条件是 `firstVisibleItemIndex <= 5`
  （接近顶部），且加载指示器在 `loading-top`。用户在底部看旧数据时永远不触发加载；
  只有滚回顶部才触发 —— 与 TimelineReverseView/BehaviorLogView/TextListView/TimeAxisGrid 全部
  使用 `last >= total - 5`（底部触发）不一致，疑似复制粘贴遗留。
- 修复：改为底部触发 `lastVisible >= total - 5`；loading 指示器移至列表底部
  （`LoadingMoreIndicator`，key=`loading-bottom`）。

### 6. 手动智能识别后 linkage 与 directive 结果不一致 【已修复】

- 文件：`core/behaviorui/.../sheet/AddBehaviorSheetContent.kt`（手动识别按钮）
- 问题：手动按钮流程先 `applyDirectiveOutcome`（directive 的 lastActivityId **覆盖**选中），
  再 `applyNoteScan` + `applyLinkage(state, scanResult.activityId, ...)` —— linkage 只对
  **scanResult** 命中的活动做「绑定标签追加」。directive 新建/命中的活动（@工作 → 创建/复用）
  不会触发其绑定标签联动，出现「活动已选但标签未带出」的不一致。
- 修复：linkage 活动源改为 directive/scan 合并后的 `state.selectedActivityId` 最终值。

### 7. HomeScreen 嵌套 Scaffold（文档点名的反模式） 【记录】

- 文件：`feature/home/.../ui/HomeScreen.kt:174-224`
- 问题：符合 `docs/agent/06-common-bug.md` #1 的模式。当前内层 Scaffold 的 content `padding`
  被完全忽略（Column 未应用），所以**尚无可见双倍顶部空白**；但 Scaffold 只为 SnackbarHost 存在，
  属于维护地雷，后续改动（如给内层加 topBar）极易触发已知 bug。
- 建议：后续移除内层 Scaffold，SnackbarHost 直接平铺在 Box 内；改动涉及 snackbar 布局，需验证。

---

## P2 — 整洁 / 死代码

### 1. AddBehaviorState.hasTimeConflict 从未被 UI 消费 【已修复】

- 文件：`core/behaviorui/.../sheet/AddBehaviorState.kt`
- 问题：derivedStateOf 懒求值，全项目无 `.hasTimeConflict` 读取；真正的冲突检测在
  `AddBehaviorUseCase`（返回 Conflict → errorMessage）。属冗余计算。
- 修复：删除 `hasTimeConflict` 派生属性及相关 import；`existingBehaviors` 参数从 state 构造移除
  （公共 Sheet API 仍保留 `existingBehaviors` 参数以保持签名稳定）。

### 2. tagsForSelectedActivity / _selectedActivityId / onActivitySelected 生产死代码 【记录】

- 文件：`feature/home/.../viewmodel/HomeViewModel.kt:102-109, 328-330`
- 问题：`tagsForSelectedActivity` 仅被 `HomeViewModelTest.kt:149-155` 引用；生产中
  `onActivitySelected`（showEditSheet 内调用）不驱动任何 UI。整条「选中活动→标签流」链路闲置。
- 建议：保留（测试覆盖），或标注为未来标签预载入口；勿删以免破坏测试。

### 3. GridCell 背景色 isPlatinum 分支与 else 完全相同 【已修复】

- 文件：`feature/home/.../ui/components/GridCell.kt:61-65`
- 问题：`isPlatinum -> surfaceContainerLow` 与 `else -> surfaceContainerLow` 值相同，死分支
  （白金效果实际由边框表达）。已清理为二分支。

### 4. TimeSideBar / GridContent 中 derivedStateOf 包在 remember 里，失去意义 【记录】

- 文件：`feature/home/.../ui/components/TimeSideBar.kt:66-68`、`ui/HomeScreen.kt:494-500`
- 问题：`remember(bubbleY) { derivedStateOf { ... } }` 与 `remember(gridSections) { derivedStateOf {...} }`
  的 remember key 已包含输入，derivedStateOf 的惰性求值无收益（每次重组直接重算）。
- 建议：直接去掉外层 remember 或去掉 derivedStateOf，二者取一。

### 5. WheelPicker 用 index 作 LazyColumn key 【记录】

- 文件：`core/behaviorui/.../sheet/WheelPicker.kt:103-105`
- 问题：`key = { index -> index }`。小时/分钟列表静态无碍；日期列表（±3 天动态扩窗）增长时
  index key 可能复用错位的动画/滚动状态。
- 建议：DateWheelPicker 改用 `DateItem.date` 作稳定 key（需保留两端 null padding 的特殊 key）。

### 6. CategoryGroupCard FlowRow items 缺 key 【记录】

- 文件：`core/behaviorui/.../sheet/CategoryGroupCard.kt:205-226`
- 问题：`items.forEach` 未加 `key(item.itemId)`；排序（频率/字母/最近）切换时 Compose 可能复用
  错位 item 的选中/展开状态。
- 建议：`items.forEach { key(it.itemId) { ItemChip(...) } }`。

### 7. TimeAdjustmentOverlay 硬编码 90dp 偏移 【记录】

- 文件：`core/behaviorui/.../sheet/TimeAdjustmentSection.kt:75,103`
- 问题：覆盖层定位依赖 `+ 90.dp.toPx()` 魔法数，换 UI 高度/字体缩放时易错位。
- 建议：基于内层 Box 测量结果计算偏移（已有 innerBoxPositionInWindow，可减去固定 header 高度）。

### 8. HomeUiStateBuilder rowId 可能跨 section 重复 【记录】

- 文件：`feature/home/.../viewmodel/HomeUiStateBuilder.kt:282`
- 问题：`"row-%d-%s"` 的 rowIndex 按 section 内重置；当多个 section 首 cell 同为 add 占位时
  （当前唯一 add cell 只在今天 section，实际不冲突）key 可重复。脆弱。
- 建议：rowId 前缀加入 `section.date`。

### 9. 专注卡片无限 pulse 动画常驻 【记录】

- 文件：`feature/home/.../ui/components/moment/ActiveCard.kt:63-72`
- 问题：ActiveCard 的 `rememberInfiniteTransition` pulse 在卡片常驻时持续运行（每帧动画）。
  首页 header 常驻，电量/性能敏感。
- 建议：仅在可见/专注态启用动画，或降低帧率。

### 10. behaviorui 疑似未使用公共组件 【记录】

- 文件：`NoteInput.kt`、`TimePickerCompact.kt`、`ActivityPicker.kt`、`BehaviorNatureSelector.kt`
- 问题：生产环境无引用（debug 预览仅用 TagPicker/TimeAdjustmentComponent）。公共 API，
  删除需确认无外部/未来使用。
- 建议：与架构 owner 确认保留或归档。

### 11. AiQuickInputViewModel 每轮重建 toolsJson 【记录】

- 文件：`feature/home/.../viewmodel/AiQuickInputViewModel.kt:70-72`
- 问题：`toolRegistry.getAllTools().map { it.toOpenAiFunctionJson() }` 每次 send 全量重建。
- 建议：缓存到字段（工具注册表启动后基本不变）。

---

## 已修复清单

| 文件 | 修复 |
|------|------|
| `feature/home/.../ui/HomeSheetRouter.kt` | P0：`startEpochMs ?: error(...)` → `?: 0L`；P1：排除 PENDING；TARGET 传预估时长 |
| `core/behaviorui/.../sheet/AddBehaviorSheetContent.kt` | P0：columnLines `coerceAtLeast(1)`；P1：linkage 用最终 activityId；ACTIVE 未来时间校验；透传 estimatedDuration |
| `core/behaviorui/.../sheet/AddBehaviorSheet.kt` | P1：`AddTargetBehaviorSheet` / Wrapper 透传 `initialEstimatedDurationMs` |
| `core/behaviorui/.../sheet/AddBehaviorState.kt` | P2：删除未消费的 `hasTimeConflict`；remember key 含 estimatedDuration |
| `feature/home/.../ui/components/MomentView.kt` | P1：底部 loadMore + loading 指示器到底部 |
| `feature/home/.../model/HomeUiState.kt` | P1：`editInitialEstimatedDurationMs` |
| `feature/home/.../viewmodel/HomeViewModel.kt` | P1：编辑/关闭/combine 同步预估时长字段 |
| `feature/home/.../ui/components/GridCell.kt` | P2：清理 isPlatinum 死分支 |
