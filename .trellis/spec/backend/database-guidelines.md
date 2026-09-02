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

<!-- How to create and run migrations -->

(To be filled by the team)

---

## Naming Conventions

<!-- Table names, column names, index names -->

(To be filled by the team)

---

## Common Mistakes

<!-- Database-related mistakes your team has made -->

(To be filled by the team)
