# 主页与交互体验修复/优化

## Goal

修复 4 个 UI bug 和 2 个交互优化，提升首页专注卡片、行为弹窗、AI 对话界面的视觉一致性与交互合理性。

## Requirements

### Bug 修复

**B1. 专注卡片关闭卡片模式后文字颜色不可见**

- 文件: `FocusCardStyleExt.kt:34-45`
- 根因: `enableCardStyle=false` 时 Surface 背景变透明，但文字颜色仍用 `onPrimaryContainer`（或基于 themeColor 亮度计算的固定色），与默认背景色相近导致不可读
- 修复方向: 当 `enableCardStyle=false` 时，文字颜色应使用对默认背景有足够对比度的颜色（如 `onSurface`）

**B2. 行为弹窗时间滚轴激活行文字颜色可视性弱**

- 文件: `WheelPicker.kt:44-53`
- 根因: 选中项文字色用 `onPrimaryContainer`，背景为 `secondaryContainer`；两者搭配在某些动态配色下文字对比度不足（背景色本身没问题，是文字色需要调整）
- 修复方向: 将选中项文字色改为与 `secondaryContainer` 背景有更高对比度的颜色（如 `onSecondaryContainer` 或 `onSurface`）

**B3. 首次进入无数据时专注卡片整个区域消失（无 loading，直接空白）**

- 文件: `HomeUiStateBuilder.kt:149-181`, `HomeScreen.kt:173`, 各布局 Content 函数
- 已知信息:
  - `buildEmptyState()` 返回 `momentCells = persistentListOf()`，`isLoading = false`
  - `activeCell == null && nextPendingCell == null` → 理论上应走 `EmptyCard` 分支
  - `HomeUiState()` 默认 `isLoading = true`，但用户未看到 loading 页
  - 需排查: 数据流时序（Flow 是否及时发射）、各布局 Content 是否有条件跳过 header、或 `isLoading` 卡在 true
- 修复方向: 排查并确保空数据状态下 EmptyCard 正常渲染；可能需要调整 `isLoading` 初始值或在 Flow 首次发射前确保状态正确

**B4. AI 对话界面生成消息时输入框出现原生边框**

- 文件: `ChatInput.kt`
- 根因: TextField 的 containerColor 设为完全透明（`Color.Transparent`），生成消息时 hazeEffect 渲染可能异常，导致 Material3 TextField 原生边框/背景闪现
- 参考方案: rikkahub 使用 `surfaceContainerHigh.copy(alpha = 0.6f)` 作为 containerColor，确保 TextField 始终有可见背景
- 修复方向: 对标 rikkahub 的 TextField colors 配置

### 优化

**O1. 点击空专注卡片时执行当前弹窗而非完成弹窗**

- 文件: `HomeRoute.kt:32-36`
- 根因: 当前 `onEmptyCellClick` 调用 `viewModel.showAddSheet(AddSheetMode.COMPLETED, ...)`
- 修复方向: 改为 `AddSheetMode.CURRENT`（当前进行中行为弹窗）

**O2. 标签样式配置弹窗显示样式容器元素挤压**

- 文件: `TagDisplayConfigDialog.kt:70-86`
- 根因: `SingleChoiceSegmentedButtonRow` 强制 9 个按钮在同一行，导致元素严重挤压
- 参考方案: `DialogConfigScreen.kt` 中的 `ChipFlowSelector` 使用 `FlowRow` 自动换行
- 修复方向: 替换为 `FlowRow` + `SelectableOptionChip` 模式

## Acceptance Criteria

- [ ] B1: 关闭卡片模式后，所有专注卡片（Active/Pending/Empty）文字在任何主题下清晰可读
- [ ] B2: 时间滚轴激活行背景与未激活行有明显视觉区分
- [ ] B3: 首次安装/清除数据后，首页显示 EmptyCard（"添加行为"提示）
- [ ] B4: AI 对话生成消息期间，输入框不出现异常边框或背景闪烁
- [ ] O1: 点击空专注卡片打开「当前」行为弹窗，而非「完成」弹窗
- [ ] O2: 标签样式配置弹窗的 9 个显示样式选项可自动换行，不挤压

## Definition of Done

- 所有 6 个 AC 通过手动验证
- Detekt 静态分析无新增告警
- 编译通过 (`./gradlew :app:compileDebugKotlin --no-daemon`)

## Out of Scope

- 不涉及新功能开发
- 不修改数据模型/数据库 schema
- 不调整主题配色方案本身

## Technical Notes

### 关键文件

| 文件 | 问题 |
|------|------|
| `feature/home/.../moment/FocusCardStyleExt.kt` | B1 文字颜色 |
| `feature/home/.../moment/ActiveCard.kt` | B1 活跃卡片 |
| `feature/home/.../moment/PendingCard.kt` | B1 待开始卡片 |
| `feature/home/.../moment/EmptyCard.kt` | B1/B3 空卡片 |
| `core/behaviorui/.../DualTimePickerComponent.kt:318-326` | B2 激活行颜色 |
| `app/.../ai_inter/chat/components/ChatInput.kt` | B4 输入框 |
| `feature/home/.../ui/HomeRoute.kt:32-36` | O1 点击逻辑 |
| `feature/home/.../TagDisplayConfigDialog.kt:70-86` | O2 布局 |

### 参考实现

- rikkahub ChatInput: `docs/reference/rikkahub/.../ChatInput.kt:778-783`
- ChipFlowSelector: `feature/settings/.../DialogConfigScreen.kt:236-264`
