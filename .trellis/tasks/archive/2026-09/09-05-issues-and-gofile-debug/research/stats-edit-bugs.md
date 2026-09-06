# Research: stats-edit-bugs (#8 / #9)

- **Query**: GitHub issues #8 (edit-mode panel drag jump/disorder) and #9 (edit-mode bottom buttons occluded by custom nav bar); current code vs expected fix after revert
- **Scope**: mixed (GitHub issue text + current `dev-v6` source + git blame/history)
- **Date**: 2026-09-05
- **HEAD**: `d8113c1f` on branch `dev-v6`

## Findings

### Issue status

| Issue | Title | State | URL |
|---|---|---|---|
| #8 | bug: 统计页面编辑模式下拖动排序面板会出现错乱/跳回 | OPEN | https://github.com/Arsucar/NLtimer/issues/8 |
| #9 | bug: 统计页面编辑模式底部按钮容器被自定义底部导航栏遮挡 | OPEN | https://github.com/Arsucar/NLtimer/issues/9 |

Both issues have the same two-comment history:

1. 2026-09-02 — owner comment “已修复…” (claimed compileDebugKotlin `--no-daemon` passed)
2. 2026-09-04 — owner comment “代码已回退，issue 重新打开。”

The claimed Sep 2 patches are **not present** on `dev-v6`, `origin/dev-v6`, or any local/remote ref (`git log -S "computeGridDropIndex" --all` empty; `git log --all --grep="#8|#9|取消编辑|computeGridDropIndex"` empty for those strings). Current stats-edit code matches the original May 2026 implementation (`git blame` → `6df1a2fe` 2026-05-21 for drag/movePanel; `2081e16e` 2026-05-26 for the bottom Surface).

### Files Found

| File Path | Description |
|---|---|
| `feature/stats/src/main/java/com/nltimer/feature/stats/viewmodel/StatsViewModel.kt` | `movePanel` / `removePanel` / `addPanel` / `toggleEditMode`; DataStore collect into `_uiState` |
| `feature/stats/src/main/java/com/nltimer/feature/stats/ui/component/StatsGridContainer.kt` | LazyVerticalGrid drag (`itemHeight`, `threshold`, `dragOffsetY`), `contentPadding.bottom`, `colSpan` span |
| `feature/stats/src/main/java/com/nltimer/feature/stats/ui/StatsScreen.kt` | Edit-mode bottom Surface (`BottomCenter` + `navigationBarsPadding`); two buttons only |
| `feature/stats/src/main/java/com/nltimer/feature/stats/ui/StatsRoute.kt` | Wires ViewModel callbacks into `StatsScreen` |
| `feature/stats/src/main/java/com/nltimer/feature/stats/model/StatsUiState.kt` | `dashboardConfig`, `isEditMode`; no local optimistic-order field |
| `core/data/src/main/java/com/nltimer/core/data/model/StatsDashboardConfig.kt` | `StatsPanelConfig.colSpan` default 4; default dashboard mixes colSpan=2 metrics + colSpan=4 charts |
| `core/data/src/main/java/com/nltimer/core/data/SettingsPrefsImpl.kt` | `getStatsDashboardConfigFlow` / `updateStatsDashboardConfig`; `BottomBarMode.FLOATING` coerced to `CENTER_FAB` |
| `app/src/main/java/com/nltimer/app/NLtimerScaffold.kt` | Bottom nav as `Box` overlay, not Scaffold `bottomBar`; `isAnyFloating` zeros content bottom padding |
| `app/src/main/java/com/nltimer/app/component/AppBottomNavigation.kt` | `AppBottomNavigation` / `AppFloatingBottomBar` / `AppCenterFabBottomBar`; writes `LocalNavBarWidth` |
| `core/designsystem/src/main/java/com/nltimer/core/designsystem/component/BottomBarDragFab.kt` | Defines `LocalNavBarWidth`; used by categories FAB, **not** by stats |
| `core/designsystem/src/main/java/com/nltimer/core/designsystem/theme/BottomBarMode.kt` | `STANDARD` / `FLOATING` / `CENTER_FAB` |
| `feature/categories/src/main/java/com/nltimer/feature/categories/ui/CategoriesScreen.kt` | Existing consumer of `BottomBarMode` + `LocalNavBarWidth` for overlay FAB inset |
| `feature/stats/src/main/java/com/nltimer/feature/stats/ui/component/MetricCard.kt` | `SingleMetricCard` 120.dp; `MetricCard` 132.dp |
| `feature/stats/src/main/java/com/nltimer/feature/stats/ui/component/BarChartCard.kt` | Chart canvas 200.dp |
| `feature/stats/src/main/java/com/nltimer/feature/stats/ui/component/TrendCard.kt` | Chart canvas 180.dp |
| `.trellis/tasks/05-21-review-fixes/research/ui-b-review.md` | Earlier note of hardcoded `itemHeight = 180f` |

No `feature/stats` unit tests exist.

---

## Issue #8 — drag reorder jump / disorder

### Current code state vs expected fix

**Current (`dev-v6`): still the original live-index drag + async DataStore write.** Expected Sep 2 fix (from issue comment): persist once on finger-up by panel id, optimistic UI so DataStore collect cannot overwrite, drop target from `visibleItemsInfo` + `computeGridDropIndex`, do not zero `dragOffsetY` mid-gesture. **None of those symbols/behaviors exist in current source.**

### 1. `StatsViewModel.movePanel` / `removePanel` / `addPanel` — read-modify-write race

`dashboardConfig` is a DataStore-backed `StateFlow` (`:48-49`) and is copied into `_uiState` by `init` collect (`:74-78`):

```74:78:feature/stats/src/main/java/com/nltimer/feature/stats/viewmodel/StatsViewModel.kt
        viewModelScope.launch {
            dashboardConfig.collect { config ->
                _uiState.update { it.copy(dashboardConfig = config) }
            }
        }
```

`movePanel` (`:100-111`) reads `_uiState.value.dashboardConfig.panels` **synchronously**, mutates a list copy, then launches a coroutine that writes DataStore. The `_uiState` snapshot used in the write is taken **inside** the coroutine (`:108`), not the same snapshot used to compute `mutable`. There is no `_uiState.update` of `panels` before persist. UI order therefore only changes after DataStore flow emits.

```100:111:feature/stats/src/main/java/com/nltimer/feature/stats/viewmodel/StatsViewModel.kt
    fun movePanel(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.dashboardConfig.panels
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val mutable = current.toMutableList()
        val panel = mutable.removeAt(fromIndex)
        mutable.add(toIndex, panel)
        viewModelScope.launch {
            settingsPrefs.updateStatsDashboardConfig(
                _uiState.value.dashboardConfig.copy(panels = mutable.toImmutableList()),
            )
        }
    }
```

`removePanel` (`:113-121`) and `addPanel` (`:123-135`) use the same pattern: compute from `_uiState.value`, persist in `viewModelScope.launch`, wait for collect to round-trip.

DataStore write:

```390:407:core/data/src/main/java/com/nltimer/core/data/SettingsPrefsImpl.kt
    override fun getStatsDashboardConfigFlow(): Flow<StatsDashboardConfig> = dataStore.data.map { prefs ->
        ...
    }
    override suspend fun updateStatsDashboardConfig(config: StatsDashboardConfig) {
        dataStore.edit { prefs ->
            prefs[statsDashboardConfigKey] = json.encodeToString(StatsDashboardConfig.serializer(), config)
        }
    }
```

Issue #8 expected persist path: **one persist on drag end keyed by panel id**, plus **optimistic `_uiState` update** so a later DataStore collect cannot clobber an in-flight reorder. Current code persists **on every adjacent swap during `onDrag`** (see grid below) and never updates `_uiState` locally.

`StatsUiState` (`StatsUiState.kt:10-17`) has only `dashboardConfig` / `isEditMode`; no separate optimistic panel list.

### 2. `StatsGridContainer` drag — `itemHeight`, `threshold`, `dragOffsetY`

```60:61:feature/stats/src/main/java/com/nltimer/feature/stats/ui/component/StatsGridContainer.kt
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
```

`detectDragGestures` (`:95-137`):

- `onDragStart` (`:97-100`): `dragIndex = index`; `dragOffsetY = 0f`
- `onDrag` (`:101-127`): accumulate `dragOffsetY += dragAmount.y`; auto-scroll via `visibleItemsInfo` first/last (`:104-114`); then:
  - `itemHeight = 180f` (`:116`)
  - `threshold = itemHeight * 0.6f` → **108f** (`:117`)
  - target is **only adjacent index** (`index-1` / `index+1`) (`:118-121`)
  - on hit: `onReorder(index, targetIndex)`; `dragIndex = targetIndex`; **`dragOffsetY = 0f`** (`:123-126`)
- `onDragEnd` / `onDragCancel` (`:129-136`): null index + zero offset. No “persist on finger up”.

Visual follow (`:139-148`): `translationY = dragOffsetY * 0.3f` so the dragged item only moves 30% of finger travel; after swap, offset snap-to-zero produces the jump described in #8.

`pointerInput(index)` (`:95`) is keyed on list index; after swap the same physical item’s index changes, so the gesture block is cancelled/restarted.

Issue #8 expected: drop index from `layoutInfo.visibleItemsInfo` + `computeGridDropIndex`; **do not zero offset mid-gesture**. Current: no `computeGridDropIndex` function; `visibleItemsInfo` is used only for edge auto-scroll (`:105-113`).

Hardcoded 180f vs actual panel inner heights:

| Component | File:line | Inner height |
|---|---|---|
| `SingleMetricCard` | `MetricCard.kt:76` | 120.dp |
| `MetricCard` (used inside `CoreMetricsGrid` / SUMMARY_CARD) | `MetricCard.kt:110` | 132.dp |
| `TrendCard` canvas | `TrendCard.kt:57` | 180.dp |
| `BarChartCard` canvas | `BarChartCard.kt:69` | 200.dp |
| `CategoryShareCard` donut | `CategoryShareCard.kt:75` | 160.dp size |
| `ActivityRankSection` | `ActivityRankSection.kt:32-63` | variable (one row per activity) |
| Edit chrome | `StatsGridContainer.kt:189-192` | extra title row (~48.dp) around content |

Edit-mode wrapper (`EditModeGridPanelWrapper` `:173-220`) adds a drag-handle row on top of those heights, so real item height is content + chrome, not 180f.

Same 180f note already recorded at `.trellis/tasks/05-21-review-fixes/research/ui-b-review.md:47`.

### 3. `StatsDashboardConfig.colSpan` vs 1D `removeAt`/`add`

```24:32:core/data/src/main/java/com/nltimer/core/data/model/StatsDashboardConfig.kt
data class StatsPanelConfig(
    ...
    val colSpan: Int = 4,
    val metricKind: StatsMetricKind? = null,
)
```

Default dashboard (`:99-108`): four `METRIC_CARD` with `colSpan = 2`, then `PIE_CHART` / `RANKING_LIST` with default `colSpan = 4`.

Grid uses 4 columns (`StatsGridContainer.kt:44`, `:64`) and `GridItemSpan(panels[index].colSpan.coerceIn(1, GridColumns))` (`:85-87`). `movePanel` is a 1D list `removeAt`/`add` (`StatsViewModel.kt:104-105`) with no colSpan / row occupancy math. Adjacent-index swap therefore does not match visual “drop between these two cells” when a colSpan=4 panel is dragged among colSpan=2 cards.

`addPanel` (`StatsViewModel.kt:123-128`) constructs `StatsPanelConfig(...)` without setting `colSpan`, so new panels are colSpan=4.

### 4. Previous fix that was reverted

Issue #8 comment (2026-09-02): “拖拽在松手后按 panel id 一次性 persist，乐观更新避免 DataStore 回灌覆盖；落点用 visibleItemsInfo + computeGridDropIndex，手势中不归零 offset.”

Issue #8 comment (2026-09-04): “代码已回退，issue 重新打开.”

Local history that is **not** that Sep patch:

| Commit | On `dev-v6`? | What it did |
|---|---|---|
| `6df1a2fe` (2026-05-21) | yes (ancestor) | Introduced current drag algorithm (`itemHeight=180f`, live `onReorder`, `dragOffsetY=0f` on swap) and current `movePanel` |
| `2081e16e` (2026-05-26) | yes | “启用拖拽排序（使用现有 StatsGridContainer 实现）”; added `headerContent` / padding; **did not change drag math** |
| `3378bad8` (2026-05-26) | **no** (`merge-base --is-ancestor` false) | `userScrollEnabled = !isEditMode` |
| `1f71a0da` (2026-05-26) | **no** | Replaced drag with up/down buttons |
| `6d2c528a` | **no** | Later stats chart removal on that side branch |

`git blame` on `StatsGridContainer.kt:115-127` and `StatsViewModel.kt:100-111` is still `6df1a2fe`. The Sep 2 implementation is not recoverable from this clone.

---

## Issue #9 — edit-mode bottom buttons occluded; missing 取消编辑

### Current code state vs expected fix

**Current: overlay `Surface` with only `navigationBarsPadding()`, two buttons, hardcoded grid bottom padding, no `BottomBarMode` / `LocalNavBarWidth` in stats.** Expected Sep 2 fix (from issue comment): inset edit bar using `BottomBarMode` + `LocalNavBarWidth` so it sits above the app overlay nav; add 「取消编辑」. **Those stats-side changes are absent after the 2026-09-04 revert.**

### 1. `StatsScreen.kt` Surface `align(BottomCenter)` + `navigationBarsPadding`

```194:226:feature/stats/src/main/java/com/nltimer/feature/stats/ui/StatsScreen.kt
        if (uiState.isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
            ) {
                Row(...) {
                    FilledTonalButton(...) { Text("添加面板") }
                    FilledTonalButton(...) { Text("恢复默认") }
                }
            }
        }
```

`git blame` on `:194-226` is `2081e16e` (layout) + `6df1a2fe` (row). Modifier stack is only system nav-bar inset. No `LocalTheme.bottomBarMode`, no `LocalNavBarWidth`, no extra `padding(bottom = …)` for the app bar.

### 2. `NLtimerScaffold` overlay vs Scaffold `bottomBar`

```293:300:app/src/main/java/com/nltimer/app/NLtimerScaffold.kt
            val isFloating = theme.bottomBarMode == BottomBarMode.FLOATING && !isSecondaryPage
            val isCenterFab = theme.bottomBarMode == BottomBarMode.CENTER_FAB && !isSecondaryPage
            val isAnyFloating = isFloating || isCenterFab

            Scaffold(
                modifier = Modifier.then(
                    if (!isSecondaryPage && !isAnyFloating) Modifier.padding(bottom = 80.dp) else Modifier
                )
```

```386:418:app/src/main/java/com/nltimer/app/NLtimerScaffold.kt
                            .padding(
                                top = if (isImmersive || isStats) 0.dp else ...,
                                bottom = if (isAnyFloating) 0.dp else if (!isSecondaryPage) padding.calculateBottomPadding() else 0.dp,
                            ),
...
            if (!isAnyFloating && !isSecondaryPage) {
                AppBottomNavigation(..., modifier = Modifier.align(Alignment.BottomCenter))
            }
            if (isFloating) {
                AppFloatingBottomBar(..., modifier = Modifier.align(Alignment.BottomCenter))
            }
            if (isCenterFab) {
                AppCenterFabBottomBar(..., modifier = Modifier.align(Alignment.BottomCenter))
            }
```

Scaffold has **no `bottomBar` slot**. All three bars are siblings in the outer `Box`, aligned `BottomCenter`, drawn after `NavHost`. When `isAnyFloating` is true, NavHost bottom padding is **0.dp**, so page content (including the stats edit Surface) extends under the overlay bar.

Stats is a primary tab (`isStats` at `:100` only affects top immersive padding `:376-387`, not bottom).

### 3. `BottomBarMode.CENTER_FAB` / `FLOATING`

Enum (`BottomBarMode.kt:3-7`): `STANDARD`, `FLOATING`, `CENTER_FAB`.

Theme default (`ThemeConfig.kt:22`): `BottomBarMode.CENTER_FAB`.

Settings read coerces FLOATING → CENTER_FAB (`SettingsPrefsImpl.kt:86-88`):

```86:88:core/data/src/main/java/com/nltimer/core/data/SettingsPrefsImpl.kt
            bottomBarMode = run {
                val mode = safeValueOf(prefs[bottomBarModeKey] ?: BottomBarMode.CENTER_FAB.name, BottomBarMode.CENTER_FAB)
                if (mode == BottomBarMode.FLOATING) BottomBarMode.CENTER_FAB else mode
            },
```

Theme settings UI only offers `STANDARD` and `CENTER_FAB` (`ThemeSettingsScreen.kt:470`). `ThemeSettingsViewModel.onBottomBarModeChange` also maps FLOATING → CENTER_FAB.

Default runtime path on stats: **CENTER_FAB** → `isAnyFloating = true` → overlay `AppCenterFabBottomBar` (`AppBottomNavigation.kt:242-331`) with its own `.navigationBarsPadding()` (`:260`) + `padding(bottom = 4.dp)` on the inner Row (`:268`). Stats edit Surface uses the same system inset and therefore shares the same bottom band as the floating toolbar.

`STANDARD` is the only mode where Scaffold gets `padding(bottom = 80.dp)` (`NLtimerScaffold.kt:300`) and NavHost gets `padding.calculateBottomPadding()` (`:388`).

### 4. `StatsGridContainer` `contentPadding.bottom` hardcoded

```67:72:feature/stats/src/main/java/com/nltimer/feature/stats/ui/component/StatsGridContainer.kt
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = if (isEditMode) 160.dp else 100.dp,
        ),
```

160.dp / 100.dp are constants. They do not read `BottomBarMode`, system insets, or `LocalNavBarWidth`. They only add LazyGrid trailing space; they do not move the overlay Surface.

### 5. Missing 「取消编辑」

Edit-mode bottom Row (`StatsScreen.kt:209-224`) has only 「添加面板」 and 「恢复默认」.

Exit edit is the header check IconButton (`StatsScreen.kt:142-153`) calling `onToggleEditMode` → `StatsViewModel.toggleEditMode` (`:96-98`), which flips `isEditMode` (`StatsUiState.kt:15`). No cancel that restores a pre-edit snapshot; toggle is the only exit.

The only other “取消” in the module is `AddPanelDialog` dismiss (`StatsScreen.kt:373`).

Grep of `feature/stats` for `取消编辑`: **no matches**.

### 6. Previous fix mentioned BottomBarMode + LocalNavBarWidth then reverted

Issue #9 comment (2026-09-02): “编辑底栏按 BottomBarMode + LocalNavBarWidth 给 App 叠加导航让位；增加「取消编辑」.”

Issue #9 comment (2026-09-04): “代码已回退，issue 重新打开.”

Current `LocalNavBarWidth` usage (stats does **not** appear):

| File:line | Role |
|---|---|
| `BottomBarDragFab.kt:22` | `compositionLocalOf { mutableStateOf(0.dp) }` — default 0.dp if never written |
| `AppBottomNavigation.kt:253, 269-270` | `AppCenterFabBottomBar` writes measured toolbar width |
| `BottomBarDragFab.kt:37-55` | FAB start padding `navBarWidth + 20.dp` when CENTER_FAB |
| `CategoriesScreen.kt:71-77, 143-144` | expand button start padding `navBarWidth + 92.dp` |

`NLtimerTheme` (`Theme.kt:50-53`) does **not** provide `LocalNavBarWidth`. There is no `CompositionLocalProvider(LocalNavBarWidth provides …)` anywhere; the local’s default `mutableStateOf(0.dp)` is used until `AppCenterFabBottomBar` writes it. Stats never reads that value.

---

## Related Specs

- `.trellis/tasks/05-21-review-fixes/research/ui-b-review.md:47` — records `StatsGridContainer.kt:116` hardcoded `itemHeight = 180f` vs real ~120.dp metric cards (P2, not fixed).
- `.trellis/tasks/archive/2026-05/05-20-stats-ai-dashboard/prd.md:189,245` — original dashboard PRD naming `StatsGridContainer` as drag-reorder grid.
- No `.trellis/spec/` document for stats edit-mode drag or bottom-bar clearance.

## Caveats / Not Found

- Sep 2 patch source is **not in this repository** (no commit, branch, stash, or leftover `computeGridDropIndex` / 「取消编辑」). Reconstruction of the reverted diff is not possible from git; only the issue comments describe it.
- `1f71a0da` (up/down buttons) and `3378bad8` (disable scroll in edit mode) exist on a side history that is **not** an ancestor of `dev-v6`. Current HEAD still uses the `6df1a2fe` drag path.
- GitHub MCP `search_*` tools were unavailable this session (`invalid session`); issue bodies/comments were loaded via `gh issue view 8/9`.
- `feature/stats` has no tests covering `movePanel` races or edit-bar insets.
- `LocalNavBarWidth` is only populated while `AppCenterFabBottomBar` is composed (CENTER_FAB). STANDARD / (dead) FLOATING paths do not write it.
- Issue #8 line citations (`StatsViewModel.kt:100-111`, `StatsGridContainer.kt:115-127`, `StatsDashboardConfig.kt:28`) still match current files. Issue #9 citations (`StatsScreen.kt:147-171`, `NLtimerScaffold.kt:280-402`, `StatsGridContainer.kt:81-82`) are **stale line numbers**; current equivalents are `StatsScreen.kt:194-226`, `NLtimerScaffold.kt:293-418`, `StatsGridContainer.kt:67-72`.
