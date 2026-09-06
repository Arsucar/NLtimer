# Research: prev-end-button

- **Query**: GitHub issue #14 — 记录时间「重置」按钮改为「上尾」
- **Scope**: mixed (internal code + GitHub issue body)
- **Date**: 2026-09-05

## Findings

### Issue #14

- **URL**: https://github.com/Arsucar/NLtimer/issues/14
- **Title**: feat: 记录时间「重置」按钮改为「上尾」，一键定位到上一条记录的结束时刻
- **Label**: enhancement
- **Stated current behavior**: `TimeAdjustmentComponent` row1 first button `"重置"` and row2 first button `"现在"` both write `LocalDateTime.now().withSecond(0).withNano(0)` into the currently edited field.
- **Stated target behavior**: rename `"重置"` → `"上尾"`; click sets the edited field to the previous completed record's endTime (`HomeUiState.lastBehaviorEndTime`); if null, fall back to now (same as original `"重置"`). `"现在"` stays now.
- **Stated data source**: reuse in-memory `HomeUiState.lastBehaviorEndTime` (already computed). No new Entity/DAO in MVP.
- **Stated save path**: still `AddBehaviorUseCase` → `TimeSnapService.snapAndCheckConflict`; button is input-stage jump only.
- **Stated localization**: 中「上尾」/ 英 `"Prev End"`; contentDescription 说明「定位到上一条记录的结束时间」.

### Files Found

| File Path | Description |
|---|---|
| `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/TimeAdjustmentComponent.kt` | Shortcut button grid; `"重置"` / `"现在"` both set now |
| `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/TimeAdjustmentSection.kt` | `TimeAdjustmentCard` + `TimeAdjustmentOverlay` wrappers |
| `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/AddBehaviorSheetContent.kt` | Wires overlay + `markUserAdjustedTime()` |
| `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/AddBehaviorState.kt` | Sheet state; `markUserAdjustedTime` / `endTimeAutoTracking` |
| `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/AddBehaviorSheet.kt` | Public sheets + `BehaviorSheetWrapper` param pass-through |
| `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/DualTimePickerComponent.kt` | Wheels; `LaunchedEffect` syncs when start/end change |
| `core/behaviorui/src/main/res/values/strings.xml` | behaviorui module zh strings; no `"重置"`/`"现在"` keys |
| `feature/home/src/main/java/com/nltimer/feature/home/model/HomeUiState.kt` | `lastBehaviorEndTime: LocalDateTime?` |
| `feature/home/src/main/java/com/nltimer/feature/home/viewmodel/HomeUiStateBuilder.kt` | `calculateLastBehaviorEndTime` |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/HomeSheetRouter.kt` | Passes `lastBehaviorEndTime` only as COMPLETED `initialStartTime` fallback |
| `feature/home/src/main/java/com/nltimer/feature/home/viewmodel/HomeViewModel.kt` | Rebuilds UI state; idle times overlay; does not extra-copy `lastBehaviorEndTime` |
| `feature/behavior_management/src/main/java/com/nltimer/feature/behavior_management/ui/BehaviorManagementScreen.kt` | Second `AddBehaviorSheet` caller (edit); no `lastBehaviorEndTime` |
| `feature/debug/src/main/java/com/nltimer/feature/debug/ui/preview/TimeAdjustmentPreview.kt` | Direct `TimeAdjustmentComponent` preview |
| `core/data/src/main/java/com/nltimer/core/data/util/TimeSnapService.kt` | Save-time snap/conflict; COMPLETED path does not clamp to prev end |
| `docs/ai generate/timepicker.kt` | Prototype DualTimePicker with title `"上尾"` (left section header, not a button) |
| `docs/ai generate/signTimepluser.kt` | Prototype `TimeAdjustmentComponent` with only `"现在"` |

### Code Patterns

#### 1. `TimeAdjustmentComponent` — `"重置"` and `"现在"` are identical

`TimeAdjustmentComponent.kt:24-60`:

```kotlin
fun TimeAdjustmentComponent(
    currentTime: LocalDateTime,
    onTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    maxTime: LocalDateTime? = null,
    onUserAdjusted: () -> Unit = {},
)
```

Current params: `currentTime`, `onTimeChanged`, `modifier`, `maxTime`, `onUserAdjusted`. There is **no** `lastBehaviorEndTime` / prev-end param.

Button grid (`TimeAdjustmentComponent.kt:37-60`):

| Row | Label | Action |
|---|---|---|
| row1[0] | `"重置"` L38 | `onUserAdjusted(); onTimeChanged(LocalDateTime.now().withSecond(0).withNano(0))` |
| row1[1..3] | `"-1"` `"-5"` `"-15"` | `currentTime.plusMinutes(-N)` |
| row2[0] | `"现在"` L44 | same as `"重置"`: now truncated to minute |
| row2[1..3] | `"+1"` `"+5"` `"+15"` | plus minutes, clamped to `maxTime` if set |

Labels are **hardcoded Chinese string literals**, not `stringResource`.

`TimeButton` (`TimeAdjustmentComponent.kt:80-104`): `Box` + `clickable` + `Text`. No `contentDescription`, no semantics. Height 26.dp, `labelSmall` 10.sp Bold.

Plus-buttons clamp to `maxTime`; `"重置"`/`"现在"` do **not** clamp.

#### 2. `TimeAdjustmentCard` / `TimeAdjustmentOverlay`

`TimeAdjustmentSection.kt:22-50` `TimeAdjustmentCard` — public Surface wrapper; forwards `currentTime`, `onTimeChanged`, `maxTime`, `onUserAdjusted` into `TimeAdjustmentComponent`. No extra params.

`TimeAdjustmentSection.kt:52-119` `TimeAdjustmentOverlay` — internal; signature:

```kotlin
internal fun TimeAdjustmentOverlay(
    mode: BehaviorNature,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    innerBoxPositionInWindow: Offset,
    boxPositionInWindow: Offset,
    onStartTimeChanged: (LocalDateTime) -> Unit,
    onEndTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    onUserAdjusted: () -> Unit = {},
)
```

Mode branches:

- `PENDING` (`L64`): return immediately (no overlay).
- `COMPLETED` (`L67-93`): two `TimeAdjustmentCard`s side by side.
  - Left: `currentTime = startTime`, `onTimeChanged = onStartTimeChanged`, **no** `maxTime`.
  - Right: `currentTime = endTime`, `onTimeChanged = onEndTimeChanged`, `maxTime = LocalDateTime.now()`.
  - Both share the same `onUserAdjusted`.
- `ACTIVE` (`L95-116`): one card, `currentTime = startTime`, `maxTime = LocalDateTime.now()`.

COMPLETED therefore has **two independent** `TimeAdjustmentComponent` instances (start field vs end field). Both currently receive the same `"重置"=now` action. Issue body says both start and end fields jump to last endTime when `"上尾"` is clicked.

#### 3. `AddBehaviorSheetContent` wiring

`AddBehaviorSheetContent.kt:72-112` signature currently has `initialStartTime` / `initialEndTime` but **no** `lastBehaviorEndTime`.

Overlay (`AddBehaviorSheetContent.kt:155-165`):

```kotlin
if (state.showTimeAdjustments && mode != BehaviorNature.PENDING) {
    TimeAdjustmentOverlay(
        mode = mode,
        startTime = state.startTime,
        endTime = state.endTime,
        innerBoxPositionInWindow = state.innerBoxPositionInWindow,
        boxPositionInWindow = state.boxPositionInWindow,
        onStartTimeChanged = { state.startTime = it },
        onEndTimeChanged = { state.endTime = it },
        onUserAdjusted = { state.markUserAdjustedTime() },
    )
}
```

`onStartTimeChanged` / `onEndTimeChanged` only assign state times. `onUserAdjusted` is what calls `markUserAdjustedTime()`. Overlay is shown when `state.showTimeAdjustments` is true (toggled by Dual/Single picker center click, `AddBehaviorSheetContent.kt:454-463`).

Wheel path also marks adjusted (`AddBehaviorSheetContent.kt:446-464`): DualTimePicker `onTimesChanged` and SingleTimePicker `onTimeChanged` call `state.markUserAdjustedTime()` when values change.

#### 4. `AddBehaviorState` — `onUserAdjusted` / `markUserAdjustedTime`

`AddBehaviorState.kt:76-98`:

- `sheetOpenTime` / `now` captured at construction (`LocalDateTime.now()`).
- `startTime = initialStartTime ?: now` (L80).
- `endTime = initialEndTime ?: now` (L81).
- `userAdjustedTime` starts false (L78).
- `endTimeAutoTracking` true only when `mode == COMPLETED && initialEndTime == null && editBehaviorId == null` (L90-92).

`markUserAdjustedTime()` (`AddBehaviorState.kt:95-98`):

```kotlin
fun markUserAdjustedTime() {
    userAdjustedTime = true
    endTimeAutoTracking = false
}
```

Effects:

- Stops `EndTimeAutoTickEffect` (`AddBehaviorSheetContent.kt:595-602`) which otherwise ticks `endTime = LocalDateTime.now()` every 1s.
- `resolveStartTime` (`AddBehaviorState.kt:131-145`): if `userAdjustedTime`, returns `startTime.withSecond(0).withNano(0)`; COMPLETED unadjusted keeps `initialStartTime` (millis-preserving for idle-gap fill).
- `resolveEndTime` (`AddBehaviorState.kt:147-150`): COMPLETED only; if adjusted, `endTime.withSecond(0).withNano(0)`, else `initialEndTime ?: endTime`.

State class currently has **no** field for last prev-end time. `rememberAddBehaviorState` (`AddBehaviorState.kt:22-60`) remember keys do not include a prev-end value.

#### 5. `HomeUiState.lastBehaviorEndTime` and builder

`HomeUiState.kt:31`:

```kotlin
val lastBehaviorEndTime: LocalDateTime? = null,
```

Computed in `HomeUiStateBuilder.buildUiState` (`HomeUiStateBuilder.kt:88,97`) and assigned on the returned `HomeUiState`.

`calculateLastBehaviorEndTime` (`HomeUiStateBuilder.kt:327-337`):

```kotlin
private fun calculateLastBehaviorEndTime(behaviors: List<Behavior>, zoneId: ZoneId): LocalDateTime? {
    return behaviors
        .filter { it.endTime != null }
        .maxByOrNull { it.endTime ?: 0 }
        ?.endTime
        ?.let { endTime ->
            Instant.ofEpochMilli(endTime)
                .atZone(zoneId)
                .toLocalDateTime()
        }
}
```

Facts:

- Filter is `endTime != null` only; **no** `status == COMPLETED` filter. ACTIVE with a non-null endTime would be included if such records exist. PENDING typically has null endTime and is excluded.
- Result is `LocalDateTime` in `ZoneId.systemDefault()`; seconds/nanos are **not** zeroed (unlike the `"现在"` button).
- Empty list path `buildEmptyState` (`HomeUiStateBuilder.kt:149-180`) does **not** set `lastBehaviorEndTime`; it stays default `null`.
- `HomeUiStateBuilderTest` has **no** assertions on `lastBehaviorEndTime`.

`HomeViewModel` collect (`HomeViewModel.kt:227-242`): `buildUiState(...)` produces a fresh `HomeUiState` that already contains `lastBehaviorEndTime`; then `copy(...)` overlays idle/edit/sheet fields. `lastBehaviorEndTime` is **not** in the copy overlay, so it always comes from the latest builder output.

#### 6. `HomeSheetRouter` — how `lastBehaviorEndTime` is passed today

`HomeSheetRouter.kt:80-106` COMPLETED:

```kotlin
AddBehaviorSheet(
    ...
    initialStartTime = uiState.idleStartTime ?: uiState.lastBehaviorEndTime,
    initialEndTime = uiState.idleEndTime,
    ...
)
```

`HomeSheetRouter.kt:108-132` CURRENT (`AddCurrentBehaviorSheet`):

```kotlin
initialStartTime = uiState.idleStartTime ?: LocalDateTime.now(),
```

Does **not** use `lastBehaviorEndTime`. Overlay still appears in ACTIVE mode (`TimeAdjustmentOverlay` ACTIVE branch), so CURRENT sheet also shows the `"重置"` button.

TARGET (`AddTargetBehaviorSheet`) has no time-adjustment overlay (`PENDING` early return).

`lastBehaviorEndTime` is currently used **only** as COMPLETED default start, not as a live button target. After sheet open, `AddBehaviorState.startTime` is a copy of that initial value; the `"重置"` button does not re-read it — it always writes `now()`.

`HomeViewModel.showAddSheet` (`HomeViewModel.kt:288-289`) sets `idleStartTime`/`idleEndTime`. `showEditSheet` (`HomeViewModel.kt:292-310`) sets them from the cell's start/end. `hideAddSheet` clears them.

#### 7. Public sheet API / other callers

Pass-through chain today:

```
AddBehaviorSheet / AddCurrentBehaviorSheet
  → BehaviorSheetWrapper (AddBehaviorSheet.kt:212-282)
    → AddBehaviorSheetContent
      → TimeAdjustmentOverlay
        → TimeAdjustmentCard
          → TimeAdjustmentComponent
```

`AddBehaviorSheet.kt:39-65` public signature: `initialStartTime`, `initialEndTime`, no `lastBehaviorEndTime`.

`AddCurrentBehaviorSheet.kt:98-123`: `initialStartTime` only (no end).

`BehaviorSheetWrapper` (`AddBehaviorSheet.kt:219-220`) has `initialStartTime`/`initialEndTime` and forwards them into `AddBehaviorSheetContent` (`L258-259`).

Second production caller: `BehaviorManagementScreen.kt:248-270` `AddBehaviorSheet(...)` for edit. Passes `initialStartTime`/`initialEndTime` from the edited behavior; **does not** pass any prev-end. That screen has `uiState.behaviors` (`L239-241`) which could compute a last end locally, but currently does not.

Preview: `AddBehaviorSheet.kt:305-313` `AddBehaviorSheetContent` with no times.

Debug: `TimeAdjustmentPreview.kt:48-51` calls `TimeAdjustmentComponent` with only `currentTime` + `onTimeChanged` (defaults for `maxTime`/`onUserAdjusted`).

#### 8. Wheel sync after `onTimeChanged`

`DualTimePicker` (`DualTimePickerComponent.kt:81-94`) `LaunchedEffect(startProperty, endProperty)` writes wheel selected date/hour/minute when parent `startTime`/`endTime` change (after minute truncation via `withSecond(0).withNano(0)` at L54-55).

`SingleTimePicker` (`DualTimePickerComponent.kt:228-234`) same pattern for start only.

So assigning `state.startTime` / `state.endTime` from the overlay already drives wheel refresh. No extra wiring exists specifically for `"重置"`.

Production `TimePickerSection` in DualTimePicker (`DualTimePickerComponent.kt:271-315`) has **no title** `"上尾"`. That title exists only in `docs/ai generate/timepicker.kt:70` (prototype left-column header).

#### 9. strings.xml localization pattern

Existing resources:

- `core/behaviorui/src/main/res/values/strings.xml` — zh only; keys like `behavior_nature_pending`/`add_behavior_confirm`. **No** keys for 重置/现在/上尾.
- `app/src/main/res/values/strings.xml` — `app_name` only.
- `feature/home/src/main/res/values/strings.xml` — home action strings.
- `core/designsystem/src/main/res/values/strings.xml` — shared UI strings.

Repo has **no** `values-en` (or any `values-*`) directories. English strings requested by the issue would be a new resource qualifier.

In-module usage pattern (`BehaviorNatureSelector.kt:11-14, 33-41`):

```kotlin
import androidx.compose.ui.res.stringResource
import com.nltimer.core.behaviorui.R
...
text = "${stringResource(labelRes)} $symbol",
```

`TimeAdjustmentComponent` currently does **not** follow this pattern (hardcoded `"重置"`/`"现在"`). `TimeButton` has no `contentDescription`.

Other nearby hardcoded time UI strings in the same module: `"用时：$durationText"` (`AddBehaviorSheetContent.kt:429`), Toast `"开始时间必须早于结束时间"` (L515), `"开始时间不能大于当前时间"` (L522).

#### 10. Save-time snap (issue-referenced, unchanged by UI button)

`TimeSnapService.kt:24-36`: when `newStatus == COMPLETED`, returns start/end as-is plus conflict flag — **no** prev-end +1 clamp.

`TimeSnapService.kt:38-51`: clamp `adjustedStart = prevEnd + 1` runs only when `newStatus != PENDING` **and** the COMPLETED early-return did not apply, i.e. ACTIVE (and theoretically other non-COMPLETED non-PENDING). Filter is `endTime != null && endTime >= adjustedStart`, then `maxByOrNull { endTime }`.

Issue body cites this as silent save-time snap for continuity; the COMPLETED branch in current code does not perform that clamp.

No DAO method `getLastCompletedBehavior()` / `getLastEndTime()` exists (grep empty).

### Minimal change surface (param thread)

Issue body names the thread: `lastBehaviorEndTime: LocalDateTime?` from `HomeUiState` into `TimeAdjustmentComponent`.

Current signatures **without** that param, in call order:

1. `HomeSheetRouter.kt:81-106` COMPLETED `AddBehaviorSheet(...)` — has `uiState.lastBehaviorEndTime` in scope; currently only used as `initialStartTime` fallback (L86). Not forwarded as a dedicated param.
2. `HomeSheetRouter.kt:108-132` CURRENT `AddCurrentBehaviorSheet(...)` — `uiState.lastBehaviorEndTime` is in `uiState` but unused.
3. `AddBehaviorSheet.kt:39-65` public `AddBehaviorSheet` — no prev-end param.
4. `AddBehaviorSheet.kt:98-123` public `AddCurrentBehaviorSheet` — no prev-end param.
5. `AddBehaviorSheet.kt:212-282` private `BehaviorSheetWrapper` — no prev-end param; forwards `initialStartTime`/`initialEndTime` only.
6. `AddBehaviorSheetContent.kt:72-100` — no prev-end param; overlay call L155-165 has no prev-end.
7. `TimeAdjustmentOverlay` (`TimeAdjustmentSection.kt:53-62`) — no prev-end; two/one `TimeAdjustmentCard` calls L80-114.
8. `TimeAdjustmentCard` (`TimeAdjustmentSection.kt:23-28`) — no prev-end; forwards to component L42-47.
9. `TimeAdjustmentComponent` (`TimeAdjustmentComponent.kt:25-30`) — no prev-end; `"重置"` action is hardcoded now at L38.

Optional/parallel storage:

- `AddBehaviorState` constructor / `rememberAddBehaviorState` — currently no prev-end field. Issue mentions source as `AddBehaviorState`/`HomeUiState`. State is not required if the overlay receives the value as a Composable param from `AddBehaviorSheetContent`.
- `BehaviorManagementScreen.kt:248` — second `AddBehaviorSheet` caller; no `HomeUiState.lastBehaviorEndTime`. Default would be null unless that screen computes it from `uiState.behaviors`.
- `TimeAdjustmentPreview.kt:48-51` — call site with default params.

`onUserAdjusted` already threads through Overlay → Card → Component and is wired to `state.markUserAdjustedTime()` at `AddBehaviorSheetContent.kt:164`. Changing the `"重置"`/`"上尾"` `onTimeChanged` target does not require a new adjustment-flag API.

`maxTime` already exists on the component (end/ACTIVE start cards pass `LocalDateTime.now()`). `"上尾"` target is not currently clamped by `maxTime`.

### Related Specs

- No `.trellis/spec/**` files present in this workspace snapshot.
- Issue text is the primary spec for this change.

## Caveats / Not Found

- GitHub MCP `issue_read` failed (`invalid session`); issue body retrieved via `gh issue view 14 --repo Arsucar/NLtimer`.
- No `values-en` resource directories exist anywhere in the repo.
- `calculateLastBehaviorEndTime` is private; no dedicated unit tests; empty-state leaves the field null.
- No DAO `getLastCompletedBehavior` / `getLastEndTime` (issue lists this as optional/long-term).
- `TimeAdjustmentComponent` is public; debug preview and (commented) `ActivityRecordCombinedPreview` call it without extra params.
- Prototype `docs/ai generate/timepicker.kt` uses `"上尾"` as DualTimePicker **section title**, not as the reset-button replacement described in issue #14.
- COMPLETED `TimeSnapService` path currently does not clamp start to previous end+1; ACTIVE path does. Issue body describes save-time clamp as existing continuity behavior.
)
</think>
Research persisted. Report for main agent: