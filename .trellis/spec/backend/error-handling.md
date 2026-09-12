# Error Handling

> How errors are handled in this project.

---

## Overview

Android / Kotlin multi-module app (Compose + Hilt + Room + Tools for AI).
Error handling differs by layer:

| Layer | Pattern |
|-------|---------|
| DAO / Room | Let exceptions bubble; no silent catch |
| Repository / UseCase | Convert to domain result or rethrow; log with `Log.e` |
| Tool (`core:tools`) | Return `ToolResult.Error` with typed code + message; never raw domain objects |
| ViewModel | Catch business errors into UiState; **rethrow `CancellationException`** |
| AI stream | `runCatching` around persistence that may race with session delete |

---

## Error Types

### Tool layer

- `ToolResult.Success(name, data: String)` — **data must be JSON string** (see `docs/agent/06-common-bug.md` #2)
- `ToolResult.Error(code, message)` — validation / internal / not-found style codes
- Prefer specific validation errors over `INTERNAL_ERROR` for bad user/AI input (e.g. invalid date)

### Domain / import-export

- `ImportResult.Error` for export/import failures
- Do not use bare `throw Exception("...")` for expected failures — prefer typed results

---

## Error Handling Patterns

### Required: rethrow CancellationException

```kotlin
catch (e: Exception) {
    if (e is CancellationException) throw e
    Log.e(TAG, "query failed", e)
    // map to UiState error
}
```

### Required: Tool execute returns JSON string

```kotlin
// Wrong
ToolResult.Success(name, listOfTags) // toString() → "Tag@abc"

// Correct
ToolResult.Success(name, JSONArray(list).toString())
```

### Required: BehaviorNature status strings

```kotlin
// Wrong — uppercase, invisible to DAO filters
dao.setStatus(id, BehaviorNature.ACTIVE.name)

// Correct
dao.setStatus(id, BehaviorNature.ACTIVE.key)
```

### Required: log swallowed exceptions

Empty `catch (_: Exception) {}` is forbidden for I/O and DB. At minimum:

```kotlin
catch (e: Exception) {
    if (e is CancellationException) throw e
    Log.e(TAG, "op failed", e)
}
```

### AI streaming / persistence race

When streaming may outlive session clear/delete, wrap final DB writes:

```kotlin
runCatching { dao.insert(...) }.onFailure { Log.e(TAG, "persist after cancel", it) }
```

---

## API Error Responses

N/A for classic HTTP API. AI tools surface errors via `ToolResult.Error` JSON consumed by `feature:ai` toolcall path.

---

## Common Mistakes

1. **Catch Exception without rethrowing CancellationException** — breaks `flatMapLatest` / rapid range switch
2. **Tool returns Map/List instead of JSON string** — AI sees unusable toString
3. **`enum.name` written to Room status column** — case mismatch with SQL filters
4. **Import ActivityTagBinding without `source="activity"`** — tags invisible on activity side
5. **SSE line-by-line JSON only** — multi-line pretty JSON silently dropped; prefer whole-chunk parse then line fallback
6. **Home `errorMessage` not cleared after Snackbar** — `LaunchedEffect` will not re-show the same string; call `clearErrorMessage()` after `showSnackbar` (mirror `eventFeedback`)
