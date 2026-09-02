# Research: archive-ui-nav

- **Query**: Activity/Tag management UI + navigation for Archive Zone
- **Scope**: internal
- **Date**: 2026-08-31

## Findings

### Screens

| Feature | Route entry | Screen | ViewModel |
|---|---|---|---|
| Activities | `ActivityManagementRoute()` — no nav callback | `feature/management_activities/.../ui/ActivityManagementScreen.kt` | `ActivityManagementViewModel` |
| Tags | `TagManagementRoute(_onNavigateBack)` | `feature/tag_management/.../ui/TagManagementScreen.kt` | `TagManagementViewModel` |

Both are **top-level** `composable()` in `app/.../NLtimerNavHost.kt` (L58–59). No feature-owned NavGraph.

Routes: `NLtimerRoutes.MANAGEMENT_ACTIVITIES = "management_activities"`, `TAG_MANAGEMENT = "tag_management"`.

`PRIMARY_ROUTES` includes `MANAGEMENT_ACTIVITIES`. `TAG_MANAGEMENT` is drawer / settings-popup only (`AppDrawer.kt`, `RouteSettingsPopup.kt`). Neither is in `SETTINGS_FULLSCREEN_ROUTES`.

### Delete / archive today

- Tag: `TagManagementViewModel.deleteTag` → `tagRepository.setArchived(tag.id, true)`.
- Activity: `ActivityManagementViewModel.deleteActivity` → `repository.deleteActivity(id)` **hard delete**. Confirm via `ActivityManagementSheetRouter` `ConfirmDialog`.

No `setArchived(..., false)` production call.

### List components

Management lists use `CategoryGroupCard` + `ActivityChip` / `TagChip` (designsystem). Archive list can reuse chips with a Restore action instead of delete.

### Toolbar / entry point

Pages do **not** own TopAppBar. Outer `NLtimerScaffold` provides it. Gear-menu extras (`NLtimerScaffold.kt` L194–206):

- Activity page: icon color mode (background / text / normal)
- Tag page: tag color mode + show icon

An 「归档区」 item can be added to these route-specific option lists, then `navController.navigate(...)`.

### States / i18n / Hilt

- Activity `loadData()` uses Flow `.catch { isLoading = false }` (no dedicated error message).
- Strings are mostly hardcoded Chinese in composables, not `strings.xml`.
- ViewModels: `@HiltViewModel` + `hiltViewModel()` in Route.

### Nested Scaffold

Do not wrap archive page in inner `Scaffold` (`docs/agent/06-common-bug.md` #1). Put archive routes in `SETTINGS_FULLSCREEN_ROUTES` if they should hide bottom nav.
