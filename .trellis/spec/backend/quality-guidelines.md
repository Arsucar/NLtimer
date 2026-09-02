# Quality Guidelines

> Code quality standards for backend development.

---

## Overview

<!--
Document your project's quality standards here.

Questions to answer:
- What patterns are forbidden?
- What linting rules do you enforce?
- What are your testing requirements?
- What code review standards apply?
-->

Documented from `extract-ai-inter-module` task (2026-05-21).

---

## Forbidden Patterns

### Don't: Keep experimental code in app/experimental/ long-term

**Problem**: `app/experimental/` is a staging area, not a permanent home. Code here creates tight coupling with the app module.

**Why it's bad**: Other modules can't reuse the code; app module grows unbounded; navigation constants pollute `NLtimerRoutes`.

**Instead**: Extract into `feature:<name>` module once the code is stable.

---

### Don't: Duplicate network/config code in feature modules

**Problem**: Feature module creates its own `network/` files when `core:ai` already has them.

**Why it's bad**: Two copies diverge; bugs fixed in one copy persist in the other; dependency graph becomes unclear.

**Instead**: Delete feature-level network files; import from `core:ai.network` directly.

---

### Don't: Reference feature routes from NLtimerRoutes constants

**Problem**: `NLtimerRoutes` contains AI-specific constants like `AI_INTER`, `AI_ASSISTANT_CHAT`.

**Why it's bad**: `NLtimerRoutes` becomes a god object; app module has compile-time dependency on feature internals.

**Instead**: Feature defines its own `<Name>Routes` object; `NLtimerRoutes` references `AiRoutes.AI_INTER` etc.

---

## Required Patterns

### Pattern: Feature Module Structure

Every feature module must follow this structure:

```
feature/<name>/
├── build.gradle.kts          # nltimer.android.library + nltimer.android.hilt
├── src/main/
│   ├── AndroidManifest.xml   # Empty manifest
│   └── java/com/nltimer/feature/<name>/
│       ├── navigation/       # <Name>Routes.kt + <Name>NavGraph.kt
│       ├── di/               # Hilt Module
│       ├── data/             # Repository, Database, DAO
│       ├── viewmodel/        # ViewModel
│       └── <Name>Screen.kt   # UI
```

**build.gradle.kts** must use:
```kotlin
plugins {
    id("nltimer.android.library")
    id("nltimer.android.hilt")
}
```

**navigation/** must contain:
- `<Name>Routes.kt` — route constants as `object`
- `<Name>NavGraph.kt` — `NavGraphBuilder.<name>NavGraph()` extension (if module has navigation)

### Pattern: Navigation Extraction

When extracting routes from `NLtimerRoutes`:

1. Create `<Name>Routes.kt` in feature module with all route constants
2. Create `<Name>NavGraph.kt` with `NavGraphBuilder` extension function
3. In `NLtimerRoutes`, replace constant definitions with imports from `<Name>Routes`
4. In `NLtimerNavHost`, replace inline `composable()` calls with single `include` call
5. Update `PRIMARY_ROUTES` and `SETTINGS_FULLSCREEN_ROUTES` to reference `<Name>Routes.CONSTANT`

### Pattern: Gradle Build Verification

After any module extraction, always verify:

```bash
./gradlew :feature:<name>:compileDebugKotlin --no-daemon
./gradlew :app:compileDebugKotlin --no-daemon
```

Both must pass before committing.

---

### Pattern: Home item short-click action layer

**Problem**: Five home layouts used to each host local `detailCell` + `BehaviorDetailDialog` on short-click, so multi-action UX (详情 / 删除 / future) would be copy-pasted five times.

**Solution**:
1. Layouts only call `onCellClick(cell)` (long-click still `onCellLongClick` → edit sheet).
2. `HomeScreen` hosts a local state machine:
   - `actionTargetCell` → `BehaviorItemActionSheet`
   - `detailCell` → `BehaviorDetailDialog`
   - `deleteTargetCell` → `ConfirmDialog` → `deleteBehavior(id)`
3. Extend menus via `BehaviorItemActions.Default` + `when (action.id)` in `HomeScreen` only — do not re-wire layouts.

**Don't**: Put `detailCell` / action sheet state back inside Grid/Timeline/Log/Moment/TextList views.

---

### Pattern: Activity / Tag archive zone

**Problem**: Users could archive items, but there was no list to view or restore them, and the edit-form Switch mixed archive with Save.

**Solution**:
1. Gear menu on `MANAGEMENT_ACTIVITIES` / `TAG_MANAGEMENT` adds `"归档区"`.
2. Navigate with `navController.navigate(archiveRoute)` — **not** `navigateToRoute` (that `popUpTo` start destination).
3. Routes `activity_archive` / `tag_archive` live in `SETTINGS_FULLSCREEN_ROUTES` (outer TopAppBar + back).
4. Archive screens: `Box` + `SnackbarHost` + `navigationBarsPadding()`. **No inner Scaffold.**
5. Restore = `setArchived(id, false)`; list is Flow-driven.
6. Edit form: no Switch / `archiveNote` fields. Trailing row is `归档` + delete. `归档` opens `ArchiveConfirmDialog` (`minLines = 6`, 取消 / 确认归档). Confirm writes `isArchived` + `archivedAt` + `archiveNote` and dismisses the sheet. Save does not toggle archive.

**Don't**: Merge activity/tag archives into one Tab page. Don't hard-delete from the archive zone. Don't put the archive Switch back on edit forms.

---

## Testing Requirements

(To be filled by the team)

---

## Code Review Checklist

### Module Extraction Review

- [ ] No `experimental/` references remain in app module
- [ ] Feature module compiles independently
- [ ] App module compiles with feature dependency
- [ ] No duplicate network/config code (deleted, not just moved)
- [ ] Route constants extracted to feature's own `<Name>Routes`
- [ ] NavHost uses feature's `<name>NavGraph()` extension
- [ ] DI module covers all injected dependencies
- [ ] `settings.gradle.kts` includes new module
- [ ] `app/build.gradle.kts` includes new module dependency
