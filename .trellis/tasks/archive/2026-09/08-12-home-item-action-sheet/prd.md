# 主页列表项点击 → 操作 BottomSheet

## Goal

主页五种布局的列表/格子项短按后，先弹出可扩展的操作 BottomSheet，再选择「详情」或「删除」；长按编辑不变。为后续更多操作预留列表扩展点。

## What I already know

* 用户诉求：短按加一层操作路由：详情（原）、删除，后续可加更多
* 现状（`research/home-item-click-detail-flow.md`）：
  * 五布局短按 → 本地 `detailCell` → `BehaviorDetailDialog`（AlertDialog）
  * 长按 → `showEditSheet` → Add*BehaviorSheet
  * `HomeViewModel.deleteBehavior` 已有，UI 未接
  * 参考：`ActivityDetailSheet`、`core/designsystem` `ConfirmDialog`

## Requirements

1. 五布局（GRID / TIMELINE_REVERSE / LOG / MOMENT / TEXT_LIST）短按统一先出操作 BottomSheet
2. 菜单项 MVP：**详情**、**删除**（列表式 action rows，便于后续追加）
3. **详情**：关闭操作 Sheet 后打开现有 `BehaviorDetailDialog`
4. **删除**：关闭操作 Sheet → `ConfirmDialog` 二次确认 → 调 `deleteBehavior(id)`
5. **长按**编辑路径保持不变
6. 不引入 Navigation 新 route

## Acceptance Criteria

* [ ] 任意布局短按行为项 → 出现操作 BottomSheet，不直接详情
* [ ] 点「详情」→ 原 `BehaviorDetailDialog`（导出剪贴板 / 关闭）
* [ ] 点「删除」→ ConfirmDialog → 确认后记录消失；取消则保留
* [ ] 长按仍打开对应 nature 的编辑 Sheet
* [ ] 新增菜单项只需扩展 action 定义，不必改五布局各自点击接线（或接线收敛到一处）

## Definition of Done

* `./gradlew :feature:home:compileDebugKotlin --no-daemon`（或等价）通过
* 主流程可装机验证
* 不破坏 FAB / 空格子添加 / 行为管理等其他入口

## Technical Approach

* 新增可复用 `BehaviorItemActionSheet`（ModalBottomSheet + action list）
* 统一短按状态：由各布局本地 `detailCell` 改为「action target cell」；详情 / 删除为后续状态机步骤
* 删除确认复用 `ConfirmDialog`；删除回调上提到 `HomeRoute` / `HomeScreen` 接 `viewModel.deleteBehavior`
* 可选收敛：在 `HomeLayoutContent` 层统一 host Sheet，避免五布局各拷一份

## Decision (ADR-lite)

| 议题 | 决定 |
|------|------|
| 操作层形态 | BottomSheet 操作菜单 |
| 删除 | ConfirmDialog 二次确认 |
| MVP 菜单 | 仅 详情 + 删除；编辑仍长按 |
| Nav route | 不做 |

## Out of Scope

* 菜单内「编辑」项
* 删除后 Snackbar 撤销
* 真正的 Navigation `behavior/{id}` 路由
* 行为管理页同样改造
* 重做 `BehaviorDetailDialog` 内容（保持现有 dump + 剪贴板）

## Research References

* [`research/home-item-click-detail-flow.md`](research/home-item-click-detail-flow.md)

## Technical Notes

* 关键接线文件：`GridRow` / `TimelineReverseView` / `BehaviorLogView` / `MomentView` / `TextListView`
* Dialog：`BehaviorDetailDialog.kt`
* VM：`HomeViewModel.deleteBehavior`
* 共享确认：`core/designsystem/.../ConfirmDialog.kt`
