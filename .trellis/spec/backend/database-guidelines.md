# Database Guidelines

> Database patterns and conventions for this project.

---

## Overview

<!--
Document your project's database conventions here.

Questions to answer:
- What ORM/query library do you use?
- How are migrations managed?
- What are the naming conventions for tables/columns?
- How do you handle transactions?
-->

(To be filled by the team)

---

## Query Patterns

### Archive (`isArchived` / `archivedAt`)

Activity and Tag DAOs:

```kotlin
@Query("SELECT * FROM … WHERE isArchived = 1 ORDER BY archivedAt DESC, name")
fun getArchived(): Flow<List<…Entity>>

@Query("UPDATE … SET isArchived = :archived, archivedAt = CASE WHEN :archived THEN :now ELSE NULL END WHERE id = :id")
suspend fun setArchived(id: Long, archived: Boolean, now: Long)
```

Repository keeps two-arg `setArchived(id, archived)` and passes `System.currentTimeMillis()`.

**Don't**: `UPDATE … SET isArchived = :archived` only — `archivedAt` stays null and archive-zone sort breaks.

**Don't**: Change DAO `setArchived` arity without updating every Fake DAO (`: ActivityDao` / `: TagDao`).

Archive confirm (`archiveActivity` / `archiveTag`) must set `isArchived`, `archivedAt`, and `archiveNote` together. Edit-form Save must not toggle those fields.

---

## Migrations

New-table migrations: inline FK in `CREATE TABLE` (see `feature/ai/.../Migrations.kt` style; core/data precedents `Migration6To7` composite-PK binding table, `Migration11To12` full-column-list write). No `PRAGMA foreign_keys` toggle unless rebuilding an existing table.

- **Registration is mandatory**: `DatabaseModule` uses `fallbackToDestructiveMigration(true)` — a missing entry in `ALL_MIGRATIONS` silently wipes user data on first launch after update.
- **Schema verification**: KSP exports `core/data/schemas/com.nltimer.core.data.database.NLtimerDatabase/{N}.json`. Compare migration SQL column order/type/not-null/default + index names (incl. `DESC` ordering) against it. core/data has no androidTest/room-testing — no instrumented migration tests possible.
- DESC index: `@Index(value=["activityId","timestamp"], orders=[ASC, DESC])` (Room 2.6+); SQL uses `CREATE INDEX ... (activityId, timestamp DESC)` and Room's generated index name.
- Do not write `DEFAULT` in CREATE TABLE unless the entity also has `@ColumnInfo(defaultValue)`.
- Room's schema validation is strict about index names: `index_<table>_<column>` for every FK column indexed or not.

## Naming Conventions

| Item | Rule | Example |
|------|------|---------|
| Index | `index_<table>_<col1>[_<col2>]` | `index_behavior_event_activityId_timestamp` |
| Enum column | TEXT storing `.key`, never `.name` | `status TEXT` `'active'` |
| Reserved words | Backtick in queries | `` MAX(`order`) `` |
| JSON in TEXT column | nullable `String?` + manual kotlinx encode/decode in domain layer | `optionsJson` |
| Composite reference table | `primaryKeys=["aId","bId"]` + one FK CASCADE per side + per-side index | `event_template_tag_binding`, `behavior_tag_cross_ref` |

## Testing

- Repository transactions go in Impl via `database.withTransaction {}` — DAO layer uses **zero `@Transaction` annotations**.
- **MockK cannot mock Room's inline `withTransaction` returning non-Unit**: `mockkStatic("androidx.room.RoomDatabaseKt")` + `coAnswers { (args[1] as suspend () -> Unit).invoke() }` pattern works for Unit-returning blocks only; tests hitting non-Unit transaction paths are `@Ignore`d (see `BehaviorRepositoryImplTest`). Design new transaction methods to return `Unit` for testability, or accept instrumented-only coverage.

---

## Common Mistakes

<!-- Database-related mistakes your team has made -->

(To be filled by the team)
