# Research: home-item-click-detail-flow

- **Query**: Research current home page list-item click → detail dialog flow; document components, handlers, navigation, action patterns, multi-action sheets
- **Scope**: mixed (internal code + existing UI patterns)
- **Date**: 2026-08-12

## Findings

### Current click → detail flow (summary)

Across all five home layouts, **short click on a behavior cell/item** sets a **local Compose state** `detailCell: GridCellUiState?` and immediately shows **`BehaviorDetailDialog`** (`AlertDialog`). This is **not** Navigation Compose, **not** a `ModalBottomSheet`, and **not** driven by `HomeViewModel` / `HomeUiState.detailBehavior`.

**Long click** on the same items calls `onCellLongClick(cell)` → `HomeRoute` → `HomeViewModel.showEditSheet(cell)` → `HomeSheetRouter` opens the appropriate **Add*BehaviorSheet** (`ModalBottomSheet`) in edit mode.

There is **no intermediate action menu** on home item click today.

---

### 1. Home screen list item components and click handlers

| Layout | View composable | Item component | Click wiring |
|---|---|---|---|
| GRID | `TimeAxisGrid` → `GridRow` | `GridCell` | `combinedClickable(onClick={ detailCell=cell }, onLongClick={ onCellLongClick(cell) })` |
| TIMELINE_REVERSE | `TimelineReverseView` | `TimelineBehaviorItem` | `onClick={ detailCell=item.cell }`, `onLongClick={ onCellLongClick(item.cell) }` |
| LOG | `BehaviorLogView` | `BehaviorLogCard` | same pattern via card params |
| MOMENT | `MomentView` | `MomentBehaviorItem` | same |
| TEXT_LIST | `TextListView` | `TextListFlowRow` / `TextListTableRow` | same |

Data model for list-style layouts:

- `HomeListItem` sealed: `CellItem(cell: GridCellUiState)` | `DayDivider`
- Path: `feature/home/.../model/HomeListItem.kt`
- Cell payload: `GridCellUiState` (`feature/home/.../model/GridCellUiState.kt`)

Callback chain for **long click only** (edit):

```
HomeRoute.onCellLongClick → HomeViewModel.showEditSheet(cell)
  → HomeScreen.onCellLongClick
  → HomeLayoutContent → *Content → *View
```

Short click **never** leaves the layout view; each view owns its own `var detailCell by remember { mutableStateOf(...) }`.

Key call sites (onClick → detail):

| File | Lines (approx) |
|---|---|
| `feature/home/.../ui/components/GridRow.kt` | 38, 75–77, 93–97 |
| `feature/home/.../ui/components/TimelineReverseView.kt` | 89, 170–171, 186–188 |
| `feature/home/.../ui/components/BehaviorLogView.kt` | 53, 147–148, 159–161 |
| `feature/home/.../ui/components/MomentView.kt` | 137, 210–211, 253–254 |
| `feature/home/.../ui/components/TextListView.kt` | 80, 189–196, 206–208 |

Item UI wrappers:

| File | Role |
|---|---|
| `.../BehaviorLogCard.kt` | LOG card; `combinedClickable` |
| `.../MomentBehaviorItem.kt` | MOMENT row; `combinedClickable` |
| `.../GridCell.kt` | GRID filled cell (click on outer `GridRow`) |
| `TimelineReverseView.kt` (private `TimelineBehaviorItem`) | timeline row |

---

### 2. How detail dialog/sheet is currently shown

**Implementation: Material3 `AlertDialog`**, composable `BehaviorDetailDialog`.

- Path: `feature/home/src/main/java/com/nltimer/feature/home/ui/components/BehaviorDetailDialog.kt`
- API: `BehaviorDetailDialog(cell: GridCellUiState, onDismiss: () -> Unit)`
- Content: debug-style field dump (behaviorId, activity, status, tags, times, durations, note, etc.)
- Actions: **「导出到剪贴板」** (dismissButton) + **「关闭」** (confirmButton)
- **No edit / delete / more actions** on this dialog

**Not used for this flow:**

- `ModalBottomSheet` for detail
- Navigation route with behavior id
- `HomeUiState.isDetailSheetVisible` / `detailBehavior` (fields exist but **no writers/readers** elsewhere in codebase)

Unused / planned-looking state (declared only):

```kotlin
// HomeUiState.kt
val isDetailSheetVisible: Boolean = false,
val detailBehavior: BehaviorDetailUiState? = null,
```

`BehaviorDetailUiState` (`feature/home/.../model/BehaviorDetailUiState.kt`) documents “行为详情底部弹出页的 UI 状态” but is **not wired** to any composable or ViewModel method today.

---

### 3. Related navigation routes

`NLtimerRoutes` / `NLtimerNavHost`:

| Route | Destination | Relation to item detail |
|---|---|---|
| `home` | `HomeRoute` | Start destination; **all detail UI is in-screen local state** |
| `behavior_management` | `BehaviorManagementRoute` | Separate full screen; click item → **edit** via `AddBehaviorSheet`, not home detail dialog |
| Others (settings, tags, activities, …) | — | No home-item detail deep link |

There is **no** route like `behavior/{id}` or detail composable in the nav graph.

Home sheet surface is **local overlay** via `HomeSheetRouter` when `uiState.addSheetMode != null` (add/edit sheets only).

---

### 4. Existing action patterns (delete / edit / more) on behavior/list items

#### Home — dual gesture split

| Gesture | Effect |
|---|---|
| Click | `BehaviorDetailDialog` (local, read-only dump + clipboard) |
| Long click | Edit via `showEditSheet` → `AddBehaviorSheet` / `AddCurrentBehaviorSheet` / `AddTargetBehaviorSheet` based on `BehaviorNature` |

`HomeViewModel.showEditSheet` (approx lines 286–313):

- Maps `COMPLETED` → `AddSheetMode.COMPLETED`, `ACTIVE` → `CURRENT`, `PENDING` → `TARGET`
- Sets `editBehaviorId`, tag ids, note, estimated duration, idle start/end from cell
- Async loads `editInitialActivityId` via `behaviorRepository.getBehaviorWithDetails`

#### Home — delete API exists, UI not wired

- `HomeViewModel.deleteBehavior(id)` → `behaviorRepository.delete(id)` (line ~432)
- Covered by unit test `deleteBehavior calls repository`
- **Not** passed into `HomeScreen` / list views; no home UI button calls it

#### Home — other actions (not on list-item click)

- FAB / drag options: complete, add COMPLETED/CURRENT/TARGET, AI quick input (`HomeScreen` BottomBarDragFab)
- Empty cell click → `showAddSheet` (CURRENT or COMPLETED with idle range)
- Focus card: complete / start behavior

#### Behavior management screen

- Click item → `startEditBehavior` → **`AddBehaviorSheet` edit** (no intermediate action sheet)
- Long click → multi-select toggle
- Path: `feature/behavior_management/.../BehaviorManagementScreen.kt`

#### Activity management (closest “detail + multi actions” pattern)

- `ActivityDetailSheet`: `ModalBottomSheet` with header actions **Edit** + **Delete** (IconButtons)
- Delete routes through `ConfirmDialog` via `DialogState.DeleteActivity`
- Router: `ActivityManagementSheetRouter.kt`
- Sheet: `feature/management_activities/.../ActivityDetailSheet.kt`

#### Tag management

- Edit form sheet can call `onDelete` → `ConfirmDialog`
- Category cards use `DropdownMenu` for rename/delete

#### Shared confirm pattern

- `core/designsystem/.../ConfirmDialog.kt` — AlertDialog wrapper for destructive confirm (used by activities/tags/categories)

---

### 5. File paths of key classes

#### Home feature

| Path | Role |
|---|---|
| `feature/home/.../ui/HomeRoute.kt` | Binds VM; maps long-click → `showEditSheet` |
| `feature/home/.../ui/HomeScreen.kt` | Layout switch, FAB, hosts `HomeSheetRouter` |
| `feature/home/.../ui/HomeSheetRouter.kt` | Routes `AddSheetMode` → Add*BehaviorSheet |
| `feature/home/.../viewmodel/HomeViewModel.kt` | `showEditSheet`, `hideAddSheet`, `deleteBehavior`, add/complete |
| `feature/home/.../model/HomeUiState.kt` | Sheet + unused detail fields |
| `feature/home/.../model/HomeListItem.kt` | List item sealed type |
| `feature/home/.../model/GridCellUiState.kt` | Cell/behavior UI model |
| `feature/home/.../model/BehaviorDetailUiState.kt` | Unused detail sheet model |
| `feature/home/.../ui/components/BehaviorDetailDialog.kt` | Current detail UI (`AlertDialog`) |
| `feature/home/.../ui/components/GridRow.kt` | GRID click/long-click + dialog |
| `feature/home/.../ui/components/TimelineReverseView.kt` | Timeline click + dialog |
| `feature/home/.../ui/components/BehaviorLogView.kt` | LOG click + dialog |
| `feature/home/.../ui/components/MomentView.kt` | MOMENT click + dialog |
| `feature/home/.../ui/components/TextListView.kt` | TEXT_LIST click + dialog |
| `feature/home/.../ui/components/BehaviorLogCard.kt` | LOG item |
| `feature/home/.../ui/components/MomentBehaviorItem.kt` | MOMENT item |
| `feature/home/.../ui/components/SlideActionPill.kt` | Slide-to-activate pill (focus/complete UX; **not** list-item menu) |
| `feature/home/.../ui/components/AiQuickInputSheet.kt` | AI `ModalBottomSheet` on home |

#### Navigation

| Path | Role |
|---|---|
| `app/.../navigation/NLtimerNavHost.kt` | Registers `HomeRoute` at `home` |
| `app/.../navigation/NLtimerRoutes.kt` | Route constants |

#### Shared UI / sheets

| Path | Role |
|---|---|
| `core/behaviorui/.../sheet/AddBehaviorSheet.kt` | COMPLETED add/edit `ModalBottomSheet` |
| `core/behaviorui/.../sheet/AddCurrentBehaviorSheet` (same package) | ACTIVE sheet |
| `core/behaviorui/.../sheet/AddTargetBehaviorSheet` (same package) | PENDING sheet |
| `core/designsystem/.../component/AppBottomSheet.kt` | Generic titled `ModalBottomSheet` wrapper (**defined; no other call sites found**) |
| `core/designsystem/.../component/ConfirmDialog.kt` | Delete confirm pattern |
| `core/designsystem/.../form/GenericFormSheet.kt` | Form `ModalBottomSheet` |
| `feature/management_activities/.../ActivityDetailSheet.kt` | Detail sheet with edit/delete actions |
| `feature/management_activities/.../ActivityManagementSheetRouter.kt` | DialogState → sheets/dialogs |

---

### 6. Intermediate action menus / context menus on home items

**None for list-item click.**

Existing menu-like UIs elsewhere (not home item click):

| Pattern | Where |
|---|---|
| `DropdownMenu` / `DropdownMenuItem` | App top bar layout menu, tag category cards, filter bars, chat |
| Multi-select + bulk delete | Behavior management (long-press) |
| FAB drag radial options | Home FAB (“完成/目标/当前/AI/…”) — not per-item |
| `SlideActionPill` | Slide confirm control — not context menu for list rows |

---

### Multi-action / sheet UI patterns in the app

1. **`AddBehaviorSheet` family** (`core/behaviorui`)  
   - `ModalBottomSheet` + `rememberModalBottomSheetState(skipPartiallyExpanded = true)`  
   - Full form for create/edit behavior  
   - Used by home (`HomeSheetRouter`) and behavior management  

2. **`ActivityDetailSheet`**  
   - Detail content + toolbar **Edit / Delete** on the sheet itself  
   - Delete still goes through `ConfirmDialog`  

3. **`AppBottomSheet`**  
   - Thin wrapper: title + content column inside `ModalBottomSheet`  
   - Ready primitive for a simple action list sheet; currently unused outside its definition file  

4. **`ConfirmDialog`**  
   - Standard destructive confirmation  

5. **`AiQuickInputSheet` / ExportSheet / debug pickers**  
   - Additional `ModalBottomSheet` examples (AI, debug)  

6. **`DropdownMenu`**  
   - Compact overflow menus (top bar, category more)  

---

### Flow diagram (current)

```
[Home layout item with behaviorId]
        │
        ├─ onClick ──► local detailCell = cell
        │                 └─► BehaviorDetailDialog (AlertDialog)
        │                       ├─ 导出到剪贴板
        │                       └─ 关闭 → detailCell = null
        │
        └─ onLongClick ──► onCellLongClick(cell)
                              └─► HomeViewModel.showEditSheet
                                    └─► addSheetMode + edit* fields
                                          └─► HomeSheetRouter
                                                └─► Add*BehaviorSheet (ModalBottomSheet edit)
```

---

## Caveats / Not Found

- **`HomeUiState.detailBehavior` / `isDetailSheetVisible`**: declared, never updated or rendered; `BehaviorDetailUiState` is orphan model for a “bottom sheet detail” that does not exist in UI.
- **Home delete**: repository path exists on ViewModel; **no UI entry** on home list/detail for delete.
- **No shared home “action sheet” component** for item click; five layouts each duplicate `detailCell` + `BehaviorDetailDialog` hosting.
- **`AppBottomSheet`**: no production consumers found beyond definition.
- **Navigation**: no per-behavior detail route; changing click target does not require nav graph changes for in-screen overlays.
- Task PRD goal (click → intermediate action layer with 详情 + 删除) is **not implemented**; current click goes **straight** to `BehaviorDetailDialog`.
