# Research: archive-data-layer

- **Query**: Activity/Tag archive (`isArchived`) data layer — DAO queries, repository methods, unique name on restore, entity fields, tests
- **Scope**: internal
- **Date**: 2026-08-31

## Findings

### Files Found

| File Path | Description |
|---|---|
| `core/data/src/main/java/com/nltimer/core/data/database/dao/ActivityDao.kt` | Activity archive queries and `setArchived` |
| `core/data/src/main/java/com/nltimer/core/data/database/dao/TagDao.kt` | Tag archive queries and `setArchived` |
| `core/data/src/main/java/com/nltimer/core/data/database/dao/ActivityGroupDao.kt` | Group CRUD; no archive queries |
| `core/data/src/main/java/com/nltimer/core/data/database/dao/TagGroupDao.kt` | Group CRUD; no archive queries |
| `core/data/src/main/java/com/nltimer/core/data/database/entity/ActivityEntity.kt` | `isArchived`, `archivedAt`; unique `name` index |
| `core/data/src/main/java/com/nltimer/core/data/database/entity/TagEntity.kt` | `isArchived`, `archivedAt`; unique `name` index |
| `core/data/src/main/java/com/nltimer/core/data/database/entity/ActivityGroupEntity.kt` | `isArchived`, `archivedAt`; unique `name` index |
| `core/data/src/main/java/com/nltimer/core/data/database/entity/TagGroupEntity.kt` | `isArchived`, `archivedAt`; unique `name` index |
| `core/data/src/main/java/com/nltimer/core/data/model/Activity.kt` | Domain model with `isArchived` / `archivedAt` |
| `core/data/src/main/java/com/nltimer/core/data/model/Tag.kt` | Domain model with `isArchived` / `archivedAt` |
| `core/data/src/main/java/com/nltimer/core/data/model/ActivityGroup.kt` | Domain model with `isArchived` / `archivedAt` |
| `core/data/src/main/java/com/nltimer/core/data/model/TagGroup.kt` | Domain model with `isArchived` / `archivedAt` |
| `core/data/src/main/java/com/nltimer/core/data/repository/ActivityRepository.kt` | Exposes `getAllActive`, `getAll`, `setArchived` |
| `core/data/src/main/java/com/nltimer/core/data/repository/TagRepository.kt` | Exposes `getAllActive`, `getAll`, `setArchived` |
| `core/data/src/main/java/com/nltimer/core/data/repository/ActivityManagementRepository.kt` | No `setArchived`; lists via `getAllActive` |
| `core/data/src/main/java/com/nltimer/core/data/repository/impl/ActivityRepositoryImpl.kt` | Delegates archive methods to `ActivityDao` |
| `core/data/src/main/java/com/nltimer/core/data/repository/impl/TagRepositoryImpl.kt` | Delegates archive methods to `TagDao` |
| `core/data/src/main/java/com/nltimer/core/data/repository/impl/ActivityManagementRepositoryImpl.kt` | `getAllActivities` → `getAllActive`; hard-delete |
| `core/data/src/test/java/com/nltimer/core/data/repository/ActivityRepositoryImplTest.kt` | `setArchived` / `getAllActive` / `getAll` tests |
| `core/data/src/test/java/com/nltimer/core/data/repository/TagRepositoryImplTest.kt` | `setArchived` / `getAllActive` / `getAll` tests |
| `feature/tag_management/src/test/java/com/nltimer/feature/tag_management/viewmodel/TagManagementViewModelTest.kt` | `deleteTag` → `setArchived(id, true)` |
| `core/data/src/test/java/com/nltimer/core/data/repository/ActivityManagementRepositoryImplTest.kt` | `getAllActivities` filters archived; no `setArchived` test |
| `docs/agent/05-data-model.md` | Entity field inventory including archive columns |

### 1. ActivityDao / TagDao queries

**`getArchived()` does not exist** on `ActivityDao`, `TagDao`, `ActivityGroupDao`, or `TagGroupDao`. Repo-wide search for `getArchived` returned no matches.

#### ActivityDao (`core/data/src/main/java/com/nltimer/core/data/database/dao/ActivityDao.kt`)

| Method | Signature | SQL / behavior |
|---|---|---|
| `getAllActive` | `fun getAllActive(): Flow<List<ActivityEntity>>` | `SELECT * FROM activities WHERE isArchived = 0 ORDER BY name` (L31–32) |
| `getAll` | `fun getAll(): Flow<List<ActivityEntity>>` | `SELECT * FROM activities ORDER BY name` — includes archived (L34–35) |
| `getAllActiveSync` | `suspend fun getAllActiveSync(): List<ActivityEntity>` | same filter as `getAllActive` (L89–90) |
| `setArchived` | `suspend fun setArchived(id: Long, archived: Boolean)` | `UPDATE activities SET isArchived = :archived WHERE id = :id` (L46–47) |
| `getByName` | `suspend fun getByName(name: String): ActivityEntity?` | `SELECT * FROM activities WHERE name = :name LIMIT 1` — **no `isArchived` filter** (L43–44) |
| `search` | `fun search(query: String): Flow<List<ActivityEntity>>` | `name LIKE … AND isArchived = 0` (L49–50) |
| `getUncategorized` | `fun getUncategorized(): Flow<List<ActivityEntity>>` | `groupId IS NULL AND isArchived = 0` (L53–54) |
| `getByGroup` | `fun getByGroup(groupId: Long): Flow<List<ActivityEntity>>` | `groupId = :groupId AND isArchived = 0` (L57–58) |
| `getAllPresets` / `getAllPresetsSync` | Flow / suspend | `isPreset = 1 AND isArchived = 0` (L61–65) |
| `insert` | `suspend fun insert(activity: ActivityEntity): Long` | `@Insert(onConflict = OnConflictStrategy.IGNORE)` (L19–20) |

`setArchived` updates **only** `isArchived`. It does **not** write `archivedAt`.

There is no `getAllSync` on `ActivityDao` that includes archived rows. Archived rows are reachable via `getAll()` (Flow), `getById`, `getByIds`, or `getByName`.

#### TagDao (`core/data/src/main/java/com/nltimer/core/data/database/dao/TagDao.kt`)

| Method | Signature | SQL / behavior |
|---|---|---|
| `getAllActive` | `fun getAllActive(): Flow<List<TagEntity>>` | `SELECT * FROM tags WHERE isArchived = 0 ORDER BY priority DESC, name` (L28–29) |
| `getAll` | `fun getAll(): Flow<List<TagEntity>>` | `SELECT * FROM tags ORDER BY name` — includes archived (L31–32) |
| `getAllDistinctSync` | `suspend fun getAllDistinctSync(): List<TagEntity>` | `SELECT * FROM tags ORDER BY name` — **includes archived** (L77–78) |
| `setArchived` | `suspend fun setArchived(id: Long, archived: Boolean)` | `UPDATE tags SET isArchived = :archived WHERE id = :id` (L43–44) |
| `getByName` | `suspend fun getByName(name: String): TagEntity?` | `SELECT * FROM tags WHERE name = :name LIMIT 1` — **no `isArchived` filter** (L37–38) |
| `getByCategory` | `fun getByCategory(category: String): Flow<List<TagEntity>>` | `category = :category AND isArchived = 0` (L40–41) |
| `search` | `fun search(query: String): Flow<List<TagEntity>>` | `name LIKE … AND isArchived = 0` (L46–47) |
| `getByActivityId` | `fun getByActivityId(activityId: Long): Flow<List<TagEntity>>` | join + `t.isArchived = 0` (L50–58) |
| `insert` | `suspend fun insert(tag: TagEntity): Long` | `@Insert(onConflict = OnConflictStrategy.IGNORE)` (L19–20) |

TagDao has **no** `getAllActiveSync`. Sync listing of all tags (including archived) is `getAllDistinctSync`.

`setArchived` likewise does **not** write `archivedAt`.

#### Group DAOs

`ActivityGroupDao` and `TagGroupDao` have `getAll()` / `getAllSync()` with **no** `isArchived` filter, and **no** `setArchived` / `getAllActive` / `getArchived`.

```18:22:core/data/src/main/java/com/nltimer/core/data/database/dao/ActivityGroupDao.kt
    @Query("SELECT * FROM activity_groups ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<ActivityGroupEntity>>

    @Query("SELECT * FROM activity_groups ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSync(): List<ActivityGroupEntity>
```

```14:18:core/data/src/main/java/com/nltimer/core/data/database/dao/TagGroupDao.kt
    @Query("SELECT * FROM tag_groups ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<TagGroupEntity>>

    @Query("SELECT * FROM tag_groups ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSync(): List<TagGroupEntity>
```

### 2. Repository archive methods

#### ActivityRepository / ActivityRepositoryImpl

Interface (`ActivityRepository.kt` L11–20):

```kotlin
fun getAllActive(): Flow<List<Activity>>
fun getAll(): Flow<List<Activity>>
fun getAllGroups(): Flow<List<ActivityGroup>>
fun search(query: String): Flow<List<Activity>>
suspend fun getById(id: Long): Activity?
suspend fun getByName(name: String): Activity?
suspend fun insert(activity: Activity): Long
suspend fun update(activity: Activity)
suspend fun setArchived(id: Long, archived: Boolean)
```

Impl (`ActivityRepositoryImpl.kt`):

- `getAllActive()` → `activityDao.getAllActive()` (L26–27)
- `getAll()` → `activityDao.getAll()` (L29–30)
- `setArchived(id, archived)` → `activityDao.setArchived(id, archived)` (L50–51)
- `getByName(name)` → `activityDao.getByName(name)` (L41–42)

**Missing on ActivityRepository:** `getArchived()`, `getAllActiveSync()`, any method that sets `archivedAt`.

#### TagRepository / TagRepositoryImpl

Interface (`TagRepository.kt` L10–20):

```kotlin
fun getAllActive(): Flow<List<Tag>>
fun getAll(): Flow<List<Tag>>
fun getByCategory(category: String): Flow<List<Tag>>
fun search(query: String): Flow<List<Tag>>
fun getByActivityId(activityId: Long): Flow<List<Tag>>
suspend fun getById(id: Long): Tag?
suspend fun getByName(name: String): Tag?
suspend fun insert(tag: Tag): Long
suspend fun update(tag: Tag)
suspend fun setArchived(id: Long, archived: Boolean)
```

Impl (`TagRepositoryImpl.kt`):

- `getAllActive()` → `tagDao.getAllActive()` (L19–20)
- `getAll()` → `tagDao.getAll()` (L22–23)
- `setArchived(id, archived)` → `tagDao.setArchived(id, archived)` (L46–47)
- `getByName(name)` → `tagDao.getByName(name)` (L37–38)

**Missing on TagRepository:** `getArchived()`, sync list of archived-only tags.

Call site that archives: `TagManagementViewModel.deleteTag` calls `tagRepository.setArchived(tag.id, true)` (`feature/tag_management/.../TagManagementViewModel.kt` L181–185). No production call of `setArchived(..., false)` exists.

#### ActivityManagementRepository / Impl

Interface (`ActivityManagementRepository.kt`) has **no** `setArchived`, **no** `getAll` (including archived), **no** `getArchived`.

Exposed listing:

- `getAllActivities(): Flow<List<Activity>>` — documented “获取所有未归档的活动” (L13–14)
- `getUncategorizedActivities()` / `getActivitiesByGroup(groupId)` — both DAO-filtered `isArchived = 0`
- `getAllActivitiesSync(): List<Activity>` — `activityDao.getAllActiveSync()` (impl L132–133)
- `deleteActivity(id: Long)` — hard delete: tag cross-refs, behaviors, bindings, then `activityDao.deleteById` (impl L69–76)

Impl `getAllActivities()`:

```42:43:core/data/src/main/java/com/nltimer/core/data/repository/impl/ActivityManagementRepositoryImpl.kt
    override fun getAllActivities(): Flow<List<Activity>> =
        activityDao.getAllActive().mapList { Activity.fromEntity(it) }
```

UI `ActivityManagementViewModel.deleteActivity` calls `repository.deleteActivity(id)` (hard delete), not `setArchived`.

There is **no** `TagManagementRepository`. Tag archive goes through `TagRepository`. There is **no** `TagGroup` repository.

### 3. Unique name constraints and restore conflict detection

#### Schema

All four entities declare a unique index on `name` (not partial; not scoped by `isArchived`):

```11:11:core/data/src/main/java/com/nltimer/core/data/database/entity/ActivityEntity.kt
@Entity(tableName = "activities", indices = [Index(value = ["name"], unique = true), Index("isArchived"), Index("groupId")])
```

```7:14:core/data/src/main/java/com/nltimer/core/data/database/entity/TagEntity.kt
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true),
        Index("isArchived"),
        Index("category"),
        Index("groupId"),
    ],
)
```

```7:7:core/data/src/main/java/com/nltimer/core/data/database/entity/ActivityGroupEntity.kt
@Entity(tableName = "activity_groups", indices = [Index(value = ["name"], unique = true)])
```

```7:7:core/data/src/main/java/com/nltimer/core/data/database/entity/TagGroupEntity.kt
@Entity(tableName = "tag_groups", indices = [Index(value = ["name"], unique = true)])
```

Migrations that create the unique indexes:

- `Migration5To6.kt` L33 / L75: `CREATE UNIQUE INDEX IF NOT EXISTS index_activities_name ON activities(name)` and `index_tags_name ON tags(name)`
- `Migration8To9.kt` L37 / L67: same unique indexes recreated after table rebuild
- `Migration13To14.kt` L21: `CREATE UNIQUE INDEX IF NOT EXISTS index_tag_groups_name ON tag_groups(name)`

Because uniqueness is on `name` alone, an **active** row and an **archived** row cannot share a name. Restoring an archived row (`setArchived(id, false)`) does **not** change `name`, so SQLite unique-index conflict cannot occur from restore itself.

Conflict arises when **creating a new row** whose `name` already exists on an archived (or active) row.

#### How name conflict is detected today

1. **Lookup by exact name (includes archived)**  
   `ActivityDao.getByName` / `TagDao.getByName` have no `isArchived` filter. Repository wrappers return the existing row regardless of archive state.

2. **Pre-check in AI tools** (not in AddActivity/AddTag use cases):
   - `CreateActivityTool.kt` L95–99: `if (activityRepository.getByName(name) != null)` → `ValidationError("活动已存在: $name")`
   - `CreateTagTool.kt` L85–89: `if (tagRepository.getByName(name) != null)` → `ValidationError("标签已存在: $name")`
   - `BatchCreateActivitiesTool.kt` L87–91 / `BatchCreateTagsTool.kt` L86: skip with reason `"活动已存在"` / equivalent

3. **Insert conflict strategy**  
   `ActivityDao.insert` and `TagDao.insert` use `OnConflictStrategy.IGNORE`. Duplicate `name` does not throw; Room returns `-1L` for the ignored insert. `AddActivityUseCase` (`core/data/.../AddActivityUseCase.kt` L31) and `AddTagUseCase` (`core/data/.../AddTagUseCase.kt` L37) call `insert` **without** a `getByName` pre-check.

4. **Import path**  
   `DataExportImportRepositoryImpl` uses `getByName` then merge-or-insert (`L160–167` tags, `L174–179` activities). Duplicate names update the existing row (including `isArchived` / `archivedAt` via `mergeFrom`).

5. **Restore path**  
   No production `setArchived(id, false)` call. If restore were `setArchived(id, false)` only, unique-index conflict would not fire. Conflict detection for “user created a new activity with the same name as an archived one” is: `getByName(name)` returning a non-null entity (possibly `isArchived == true`), or insert returning `-1L`.

`getByName` does not distinguish archived vs active; callers that need that distinction would inspect `result.isArchived` after lookup.

### 4. Entity fields: `isArchived`, `archivedAt`

| Entity | File | `isArchived` | `archivedAt` | Unique `name` | `isArchived` index |
|---|---|---|---|---|---|
| `ActivityEntity` | `.../entity/ActivityEntity.kt` L20–21 | `Boolean = false` | `Long? = null` | yes | yes (`Index("isArchived")`) |
| `TagEntity` | `.../entity/TagEntity.kt` L27–28 | `Boolean = false` | `Long? = null` | yes | yes |
| `ActivityGroupEntity` | `.../entity/ActivityGroupEntity.kt` L14–15 | `Boolean = false` | `Long? = null` | yes | **no** |
| `TagGroupEntity` | `.../entity/TagGroupEntity.kt` L14–15 | `Boolean = false` | `Long? = null` | yes | **no** |

Domain models mirror the same two fields: `Activity`, `Tag`, `ActivityGroup`, `TagGroup` (`toEntity` / `fromEntity` copy both).

`archivedAt` was added in `Migration8To9` (activities rebuilt with `archivedAt INTEGER`; `activity_groups` `ALTER TABLE … ADD COLUMN archivedAt`; tags rebuilt with `archivedAt`). `TagGroupEntity` columns created in `Migration13To14`.

`setArchived` SQL does not set `archivedAt`. After `setArchived(id, true)`, `isArchived` becomes true and `archivedAt` stays whatever it was (typically `null` unless written via `update` / import merge).

Group entities have the columns but no DAO/repository archive API.

### 5. Existing tests covering `setArchived`

#### Tests that exercise `setArchived` behavior

| Test | File | What it asserts |
|---|---|---|
| `` `setArchived updates archive status` `` | `core/data/src/test/java/com/nltimer/core/data/repository/ActivityRepositoryImplTest.kt` L252–259 | insert active → `repository.setArchived(1L, true)` → `getById` `isArchived == true` |
| `` `setArchived updates archive status` `` | `core/data/src/test/java/com/nltimer/core/data/repository/TagRepositoryImplTest.kt` L242–249 | same for tags |
| `` `deleteTag calls setArchived` `` | `feature/tag_management/src/test/java/com/nltimer/feature/tag_management/viewmodel/TagManagementViewModelTest.kt` L130–137 | `viewModel.deleteTag(tag)` records `archivedTagId = 1L`, `archivedValue = true` |

Related archive-filter tests (not `setArchived` itself):

- `ActivityRepositoryImplTest`: `` `getAllActive returns only non-archived activities` `` (L154–162), `` `getAll returns all activities including archived` `` (L165–172), `` `search filters by query and excludes archived` `` (L186–195)
- `TagRepositoryImplTest`: `` `getAllActive returns only non-archived tags` `` (L121–129), `` `getAll returns all tags including archived` `` (L132–139)
- `ActivityManagementRepositoryImplTest`: `` `getAllActivities returns only non-archived` `` (L63–71) — FakeActivityDao `setArchived` is a no-op (L310); this test does not call `setArchived`
- `ModelConversionTest`: maps `isArchived` / `archivedAt` on Activity and ActivityGroup (L15–172); does not call DAO `setArchived`
- Tool tests: `SelectActivitiesAndTagsToolTest` / `SearchActivitiesAndTagsToolTest` `` `includeArchived true uses getAll instead of getAllActive` ``

#### Fake DAO `setArchived` implementations (stubs, not coverage of production SQL)

Fake DAOs in repository tests copy `isArchived` only, not `archivedAt`:

```64:69:core/data/src/test/java/com/nltimer/core/data/repository/ActivityRepositoryImplTest.kt
        override suspend fun setArchived(id: Long, archived: Boolean) {
            activityEntities.replaceAll {
                if (it.id == id) it.copy(isArchived = archived) else it
            }
            activityFlow.value = activityEntities.toList()
        }
```

```69:74:core/data/src/test/java/com/nltimer/core/data/repository/TagRepositoryImplTest.kt
        override suspend fun setArchived(id: Long, archived: Boolean) {
            tagEntities.replaceAll {
                if (it.id == id) it.copy(isArchived = archived) else it
            }
            tagFlow.value = tagEntities.toList()
        }
```

No androidTest / Room in-memory DAO test for `ActivityDao.setArchived` or `TagDao.setArchived`. No test for `setArchived(id, false)` (restore). No test that `archivedAt` is written or cleared.

### Code Patterns

- Active lists: `WHERE isArchived = 0` on ActivityDao/TagDao query methods used by UI/management.
- Full lists: `getAll()` with no archive filter; tools use `includeArchived` to choose `getAll` vs `getAllActive`.
- Archive mutation: single boolean UPDATE by id; timestamp column exists but is unused by that UPDATE.
- Name uniqueness: table-wide unique index; lookup via `getByName` without archive filter; insert IGNORE.

### External References

- Not used. Room `@Insert(onConflict = OnConflictStrategy.IGNORE)` returns `-1L` when the unique index rejects the row (AndroidX Room Insert docs).

### Related Specs

- `.trellis/spec/` — no archive/`isArchived`/`setArchived` mentions found.
- `docs/agent/05-data-model.md` — documents `isArchived` / `archivedAt` on Activity, Tag, ActivityGroup, TagGroup.

## Caveats / Not Found

- `getArchived()` — not found anywhere in the repo.
- `setArchived(..., false)` — no production call site.
- `ActivityManagementRepository.setArchived` — not present; activity “delete” is hard delete.
- Group archive API (`ActivityGroupDao`/`TagGroupDao` `setArchived` / `getAllActive`) — not present; columns exist only on entities.
- `archivedAt` is not updated by `setArchived` SQL.
- No instrumented Room tests of the unique-index + IGNORE insert return value.
- `AddActivityUseCase` / `AddTagUseCase` do not call `getByName` before insert.
