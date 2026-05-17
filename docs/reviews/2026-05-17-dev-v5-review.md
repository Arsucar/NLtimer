# 2026-05-17 dev-v5 review

## Findings

1. `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/DurationPickerComponent.kt:56`

   `selectedHour` and `selectedMinute` are initialized from `durationMs` with `remember`, but the previous `LaunchedEffect(durationMs)` synchronization was removed. When the parent updates `durationMs` from outside this picker, for example opening an existing behavior, resetting a form, or applying a preset, the label can show the new value while both wheels keep the stale remembered selection. The next wheel interaction then emits a duration based on the stale wheel state and can overwrite the external update.

   Recommended fix: restore a `LaunchedEffect(durationMs)` or key the remembered wheel state by the normalized hour/minute values so the wheels always reflect the current source-of-truth duration.

## Notes

- The small width adjustment in `BehaviorDetailDialog.kt` looks low risk.
- This review did not run Gradle verification yet; it only records the blocking issue found in the current diff.
