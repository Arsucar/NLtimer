# Research: 事件记录器（GitHub issue #15）

- **Query**: GitHub issue #15 — 事件记录器：持续计时中插入多条带时间戳的复盘事件
- **Scope**: mixed（issue 正文 + 仓库内部代码/schema）
- **Date**: 2026-09-05
- **Issue**: [Arsucar/NLtimer#15](https://github.com/Arsucar/NLtimer/issues/15)（state=OPEN，label=`enhancement`）
- **远程仓库**: `origin` = `https://github.com/Arsucar/NLtimer.git`（`huhage/NLtimer` 返回 404）

## Findings

### Issue #15 需求摘要（源：issue body）

标题：`feat: 增加事件记录器，支持持续计时中插入多条带时间戳的复盘事件`

现状描述（issue 原文）：`BehaviorEntity.note: String?` 是单字段备注；ACTIVE 卡片上的 note 只读展示，修改需打开整张编辑 Sheet。

目标：一次不中断的持续计时中，随时插入多条独立「事件/复盘」条目；每条带独立时间戳、可单独增删改；关联主 `behaviorId`，不中断计时。

issue 给出的数据模型：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` PK autoGenerate | 主键 |
| `behaviorId` | `Long` FK → `behaviors.id`，`onDelete = CASCADE` | 归属行为 |
| `content` | `String` | 事件/复盘内容 |
| `timestamp` | `Long` | epoch millis |
| `order` | `Int` | 排序，范式 `COALESCE(MAX(order), -1)+1` |
| 索引 | `Index("behaviorId")` | 按行为查询 |

issue 给出的版本变更：数据库 **16 → 17**，新增 `Migration16To17`（`CREATE TABLE behavior_event ...`）。

issue 给出的 UseCase 名：`AddBehaviorEvent` / `UpdateBehaviorEvent` / `DeleteBehaviorEvent` / `ObserveBehaviorEvents`。

issue 给出的 DAO 方法：`insert` / `update` / `delete` / `observeByBehavior(Flow)` / `nextOrder`。

验收 AC1–AC5 与实现 checklist 见 issue body（本文件不展开复述）。

---

### Files Found

| File Path | Description |
|---|---|
| `core/data/src/main/java/com/nltimer/core/data/database/NLtimerDatabase.kt` | 主库定义，`version = 16` |
| `core/data/src/main/java/com/nltimer/core/data/database/migration/Migration15To16.kt` | 最近一次主库迁移（ALTER COLUMN） |
| `core/data/src/main/java/com/nltimer/core/data/database/migration/Migration13To14.kt` | 最近一次「建新表」主库迁移（`tag_groups`） |
| `core/data/src/main/java/com/nltimer/core/data/database/migration/Migration12To13.kt` | 建新表迁移（`icon_search_miss`） |
| `core/data/src/main/java/com/nltimer/core/data/database/entity/BehaviorEntity.kt` | 行为实体，含单字段 `note` |
| `core/data/src/main/java/com/nltimer/core/data/database/dao/BehaviorDao.kt` | 行为 DAO，含 `setNote` |
| `core/data/src/main/java/com/nltimer/core/data/repository/BehaviorRepository.kt` | 行为仓库接口 |
| `core/data/src/main/java/com/nltimer/core/data/repository/impl/BehaviorRepositoryImpl.kt` | 行为仓库实现 |
| `core/data/src/main/java/com/nltimer/core/data/model/Behavior.kt` | 行为领域模型（含 `fromEntity`/`toEntity`） |
| `core/data/src/main/java/com/nltimer/core/data/model/BehaviorWithDetails.kt` | 行为+活动+标签聚合，不含事件列表 |
| `core/data/src/main/java/com/nltimer/core/data/di/DatabaseModule.kt` | Hilt Room + DAO `@Provides` |
| `core/data/src/main/java/com/nltimer/core/data/di/DataModule.kt` | Hilt Repository `@Binds` |
| `core/data/src/main/java/com/nltimer/core/data/usecase/AddBehaviorUseCase.kt` | 新增/编辑行为（含 `note`） |
| `feature/ai/src/main/java/com/nltimer/feature/ai/chat/data/ConversationMessageEntity.kt` | 子项表实体（CASCADE + `order`） |
| `feature/ai/src/main/java/com/nltimer/feature/ai/chat/data/ConversationMessageDao.kt` | `nextOrder` / `observeByConversation` |
| `feature/ai/src/main/java/com/nltimer/feature/ai/chat/data/Migrations.kt` | AI 库建 `conversation_message` 的 SQL |
| `feature/ai/src/androidTest/java/com/nltimer/feature/ai/chat/data/MigrationTest.kt` | `MigrationTestHelper` 示例 |
| `feature/ai/src/androidTest/java/com/nltimer/feature/ai/chat/data/ConversationDaoTest.kt` | CASCADE + `nextOrder` 仪器测试 |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/components/moment/ActiveCard.kt` | ACTIVE 卡片 UI |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/components/SlideActionPill.kt` | 滑动完成控件 |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/components/moment/TagNoteRow.kt` | 标签 + 只读 note |
| `feature/home/src/main/java/com/nltimer/feature/home/ui/components/BehaviorDetailDialog.kt` | 完成后详情对话框 |
| `core/data/schemas/com.nltimer.core.data.database.NLtimerDatabase/16.json` | 当前导出 schema |

仓库内 **不存在** `BehaviorEvent` / `behavior_event` / `事件记录` 相关源码（Grep 无匹配）。

---

### 当前 DB 版本

`NLtimerDatabase.kt:33-46`：

```kotlin
@Database(
    entities = [
        ActivityEntity::class,
        ActivityGroupEntity::class,
        TagEntity::class,
        TagGroupEntity::class,
        BehaviorEntity::class,
        ActivityTagBindingEntity::class,
        BehaviorTagCrossRefEntity::class,
        IconSearchMissEntity::class,
    ],
    version = 16,
    exportSchema = true,
)
```

- 版本确认为 **16**（与 issue 预期一致）。
- 8 个 entity，无 `BehaviorEventEntity`。
- DAO 访问器：`activityDao` / `activityGroupDao` / `tagDao` / `tagGroupDao` / `behaviorDao` / `iconSearchMissDao`（`NLtimerDatabase.kt:48-53`）。
- `ALL_MIGRATIONS`：`MIGRATION_3_4` … `MIGRATION_15_16`（`NLtimerDatabase.kt:56-70`）。
- Schema 导出：`core/data/schemas/com.nltimer.core.data.database.NLtimerDatabase/16.json`（`database.version = 16`）。
- KSP：`core/data/build.gradle.kts:11-13`，`room.schemaLocation = $projectDir/schemas`。

`docs/agent/01-project-overview.md` 仍写「Room … 版本 12, 6 表」；`docs/agent/05-data-model.md` 表清单与版本历史止于 16、无 `behavior_event`。

---

### 现有迁移模式

包：`com.nltimer.core.data.database.migration`

命名：文件 `Migration{From}To{To}.kt`，导出 `val MIGRATION_{FROM}_{TO} = object : Migration(from, to)`。

| 文件 | 对象 | 操作类型 |
|------|------|----------|
| `Migration15To16.kt:6-10` | `MIGRATION_15_16` | `ALTER TABLE activities/tags ADD COLUMN archiveNote` |
| `Migration14To15.kt:6-9` | `MIGRATION_14_15` | `ALTER TABLE activity_tag_binding ADD COLUMN source ...` |
| `Migration13To14.kt:6-25` | `MIGRATION_13_14` | `CREATE TABLE tag_groups` + `CREATE UNIQUE INDEX` + `ALTER TABLE` + `CREATE INDEX` |
| `Migration12To13.kt:6-18` | `MIGRATION_12_13` | `CREATE TABLE IF NOT EXISTS icon_search_miss (...)` |

建新表的最近模板是 `Migration12To13`（无外键）与 `Migration13To14`（新表 + 索引）。带 **FOREIGN KEY … ON DELETE CASCADE** 的建表 SQL 出现在 **AI 库** `feature/ai/.../chat/data/Migrations.kt:16-32`（`conversation_message`），不在主库迁移目录。

主库模块注册：`DatabaseModule.kt:35-36`

```kotlin
.fallbackToDestructiveMigration(true)
.addMigrations(*NLtimerDatabase.ALL_MIGRATIONS)
```

---

### BehaviorEntity / BehaviorDao / BehaviorRepository

#### Entity（`BehaviorEntity.kt:13-45`）

- 表名 `behaviors`。
- FK：`activityId` → `ActivityEntity.id`，`onDelete = ForeignKey.CASCADE`。
- 字段：`id, activityId, startTime, endTime, status, note, pomodoroCount, sequence, estimatedDuration, actualDuration, achievementLevel, wasPlanned`。
- `note: String? = null`（L38）。无事件子表字段。

schema `16.json:369` 与实体一致：`note TEXT` 可空。

#### DAO（`BehaviorDao.kt`）

与 note 相关：

- `setNote(id, note)` — L88-89：`UPDATE behaviors SET note = :note WHERE id = :id`
- `update(..., note)` — L176-194：批量更新含 `note`
- `insert(behavior)` — L66-67

无事件子表查询。`getCurrentBehavior()`（L105-106）：`status = 'active' AND endTime IS NULL LIMIT 1`。

删除：`delete(id)` L91-92；`deleteByActivityId` L224-225。Room 层 `behavior_tag_cross_ref` 对 behavior 为 CASCADE（`BehaviorTagCrossRefEntity.kt:16-21`）。

#### Repository

接口 `BehaviorRepository.kt:29`：`suspend fun setNote(id: Long, note: String?)`

实现 `BehaviorRepositoryImpl.kt:125-126`：直接转 `behaviorDao.setNote`。

`getBehaviorWithDetails`（Impl L62-75）组装 `Behavior + Activity + tags`，**不含**事件列表。`BehaviorWithDetails.kt:10-14` 三字段。

`delete(id)`（Impl L172）：`behaviorDao.delete(id)`。

---

### conversation_message 类比（issue 指定的 analog）

**所在库不是 NLtimerDatabase**，而是独立 `AiInterDatabase`（`feature/ai/.../data/AiInterDatabase.kt:10-22`，`version = 4`，`exportSchema = false`）。

#### Entity（`ConversationMessageEntity.kt:8-29`）

```kotlin
@Entity(
    tableName = "conversation_message",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("conversationId")],
)
data class ConversationMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val order: Int,
    val role: String,
    val content: String,
    val reasoning: String = "",
    val toolCallsJson: String = "",
    val createdAt: Long,
)
```

与 issue 中 `BehaviorEventEntity` 的对应关系：

| conversation_message | issue 中的 behavior_event |
|----------------------|---------------------------|
| `conversationId` + CASCADE | `behaviorId` + CASCADE |
| `Index("conversationId")` | `Index("behaviorId")` |
| `order: Int` | `order: Int` |
| `content` | `content` |
| `createdAt` | `timestamp` |
| PK `id: String`（非自增） | PK `id: Long` autoGenerate |
| 无独立 Repository，ViewModel 直调 DAO | issue checklist 要求 Repository + UseCase |

#### DAO（`ConversationMessageDao.kt:10-27`）

```kotlin
@Query("SELECT * FROM conversation_message WHERE conversationId = :id ORDER BY `order` ASC")
fun observeByConversation(id: String): Flow<List<ConversationMessageEntity>>

@Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM conversation_message WHERE conversationId = :id")
suspend fun nextOrder(id: String): Int

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(msg: ConversationMessageEntity)
```

另有 `delete` / `deleteFromOrder` / `deleteAllInConversation`。

`order` 是 SQL 保留字，列名在查询中写成 `` `order` ``。

#### 写入路径（无 Repository）

`AiAssistantChatViewModel.kt:249-260`：先 `nextOrder(conversationId)`，再 `insert(...)`，`createdAt = System.currentTimeMillis()`。`nextOrder` 与 `insert` **未**包在同一 `withTransaction` 内。

建表 SQL（`Migrations.kt:16-32`）：`FOREIGN KEY(conversationId) REFERENCES conversation(id) ON DELETE CASCADE` + `CREATE INDEX ... conversationId`。

#### 仪器测试（`ConversationDaoTest.kt`）

- L54-64 `delete_cascadesToMessages`：删父记录后 `observeByConversation` 为空。
- L67-75 `nextOrder_returnsZeroForEmpty_thenIncrements`：空表返回 0；插入 `order=0` 后返回 1；插入 `order=5` 后返回 6。
- 使用 `Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries`（L23-25）。

---

### ActiveCard 当前 UI

文件：`feature/home/.../ui/components/moment/ActiveCard.kt`（193 行）。issue body 写「L195-201」已过期；当前 `TagNoteRow` 在 **L173**。

入口：`MomentFocusCard.kt:24-31`，`onComplete = { activeCell.behaviorId?.let(onCompleteBehavior) }`。`ActiveCard` 参数：`cell, onComplete, _momentStyle, focusCardConfig, tagDisplayConfig`。无打点/事件回调。

`ActiveCardContent` 垂直结构（L137-191）：

1. 图标 + 活动名（L145-161）
2. `SlideActionPill`（L165-171）：`onActivate = onComplete`，文案「滑动完成」/「释放完成」，`Icons.Filled.Check`
3. `TagNoteRow(tags = cell.tags, note = cell.note, ...)`（L173）
4. 时长 + 「正在专注...」（L177-190）

`SlideActionPill.kt:46-54`：横向拖到 `maxOffset * 0.7f` 触发 `onActivate()`。无第二手势。

`TagNoteRow.kt:21-46`：`tags` 空且 `note` 空白则 return；否则 `FlowRow` 渲染 `TagChip` + 单行 `Text(note)`，`maxLines = 1`，`width(120.dp)`，`TextOverflow.Ellipsis`。无点击编辑。

完成链路：`HomeRoute.kt:66-68` → `HomeViewModel.completeBehavior`（L371-375）→ `behaviorRepository.completeCurrentAndStartNext`。另有 FAB「完成行为」（`HomeScreen.kt:247-256`）。完成路径不打开详情。

---

### 完成后的行为详情屏

完成动作本身不导航到详情。详情入口：

1. **长按单元格** → `HomeRoute.kt:45-47` `onCellLongClick` → `HomeViewModel.showEditSheet`（L292-319）→ `AddBehaviorSheet` / `AddCurrentBehaviorSheet` / `AddTargetBehaviorSheet`（`HomeSheetRouter.kt`）。
2. **操作表「详情」** → `HomeScreen.kt:283-301`：`BehaviorItemActionSheet` 的 `DETAIL` 设 `detailCell`，弹出 `BehaviorDetailDialog`。

`BehaviorDetailDialog.kt:60-123`：`AlertDialog`，只读展示 `GridCellUiState` 各字段（含 `note` L106），按钮「导出到剪贴板」「关闭」。无事件列表、无编辑。

`BehaviorDetailUiState.kt:13-28` 含 `note: String?` 与 `tags`，**无事件字段**。`HomeUiState.kt:27-28` 有 `isDetailSheetVisible` / `detailBehavior`，全仓库仅在该 data class 声明，**未被赋值或读取**。

---

### ACTIVE 卡片上 note 的当前工作方式

存储：单列 `behaviors.note`（可空 TEXT）。

写入：

| 路径 | 位置 |
|------|------|
| 添加/编辑 Sheet 确认 | `AddBehaviorUseCase` insert（L72）或 `updateBehavior(..., note)`（L207-213） |
| Sheet UI 输入 | `AddBehaviorState.note`（L105）；`NoteInputComponent`（`AddBehaviorSheetContent.kt:365-367`）；限额 5000 字（`ActivityNoteComponent.kt:48,70-71`） |
| 智能识别 | `HomeViewModel.processNote` / `matchNoteFromText`（L382-398） |
| AI 工具 | `UpdateBehaviorTool.kt:82` → `behaviorRepository.setNote` |
| 首页 VM | `HomeViewModel` **没有** `setNote` 方法；`addBehavior`（L341-369）把 note 交给 UseCase |

展示（只读）：

- ACTIVE / PENDING 卡片：`TagNoteRow`（`ActiveCard.kt:173`，`PendingCard.kt:139`）
- `GridCellUiState.note`（L32），由 `HomeUiStateBuilder.kt:246` 从 `behavior.note` 填入
- 日志/网格/时间线/文本列表：`BehaviorLogCard`、`GridCell`、`MomentBehaviorItem`、`TimelineReverseView`、`TextListView`

编辑 ACTIVE 的 note：长按 → `showEditSheet`（`editInitialNote = cell.note`，`HomeViewModel.kt:305`）→ `AddCurrentBehaviorSheet(initialNote = ...)`（`HomeSheetRouter.kt:108-116`）。打开整张 Sheet，会离开「专注卡片」交互。

`HomeViewModel` 当前注入（L66-79）：`BehaviorRepository`、`AddBehaviorUseCase` 等，无事件仓库。

---

### Hilt DB 模块

`DatabaseModule.kt`（`@Module @InstallIn(SingletonComponent::class)` object）：

- `provideDatabase` L29-38：`Room.databaseBuilder(..., "nltimer-database")` + `fallbackToDestructiveMigration(true)` + `addMigrations(*ALL_MIGRATIONS)` + 4 线程 query executor
- `provide*Dao`：Activity / ActivityGroup / Tag / TagGroup / Behavior / IconSearchMiss（L47-69）
- 无 `BehaviorEventDao` provider

`DataModule.kt` abstract `@Binds`：Activity / Tag / Behavior / Category / ActivityManagement / DataExportImport（L24-44）。无 `BehaviorEventRepository`。

`ServiceModule.kt`：`ClockService` → `SystemClockService`（`currentTimeMillis()`），以及 `TimeSnapService`。

UseCase 现有清单（`core/data/.../usecase/`）：`AddBehaviorUseCase`、`AddActivityUseCase`、`AddTagUseCase`、`ExportDataUseCase`、`ImportDataUseCase`、`StatsQueryUseCase`。均为 `@Singleton @Inject constructor`。issue 中的四个 BehaviorEvent UseCase **均不存在**。

---

### 迁移测试模式

| 位置 | 内容 |
|------|------|
| `feature/ai/src/androidTest/.../MigrationTest.kt` | `MigrationTestHelper(InstrumentationRegistry, AiInterDatabase::class.java)`；`createDatabase(name, 3)` → `runMigrationsAndValidate(name, 4, true, MIGRATION_3_4)` |
| `feature/ai/src/androidTest/.../ConversationDaoTest.kt` | in-memory Room + CASCADE / nextOrder |
| `core/data/src/test/.../migration/CategoryMigrationValidatorTest.kt` | **不是** Room schema 迁移；测 DataStore 分类 → `tag_groups` |
| `core/data/` | **无** `androidTest` 源集 |
| `core/data/build.gradle.kts` | test 仅 `junit` / `coroutines-test` / `mockk`；**无** `room-testing` |
| `app/build.gradle.kts:143` | `androidTestImplementation(libs.room.testing)` |
| `gradle/libs.versions.toml` | `room-testing = androidx.room:room-testing` |
| `nltimer.android.library.gradle.kts:11` | `testInstrumentationRunner = AndroidJUnitRunner` |

主库 **16→17 的 MigrationTestHelper 测试当前不存在**。仓库内唯一 `MigrationTestHelper` 用法在 AI 模块。

Repository 单测范式：`BehaviorRepositoryImplTest.kt` 用 Fake DAO + MockK `withTransaction`。UseCase 单测范式：`AddBehaviorUseCaseTest.kt` MockK `BehaviorRepository`。

---

### 删除主 behavior 时的级联现状

- `HomeViewModel.deleteBehavior`（L438-448）→ `behaviorRepository.delete(id)` → `BehaviorDao.delete`。
- `behavior_tag_cross_ref.behaviorId`：`ForeignKey.CASCADE`（`BehaviorTagCrossRefEntity.kt:16-21`）。
- `behaviors.activityId`：删 Activity 时 CASCADE 删 behaviors。
- 无 `behavior_event` 表，故当前无事件孤儿问题；issue AC4 依赖新表 FK CASCADE。

---

### 时间戳展示工具

`TimeFormatUtils.kt`：

- `hhmmFormatter` / `hhmmssFormatter` / `yyyyMMddHHmmFormatter`
- `formatTimestamp(timestamp: Long)` L100-101：`yyyy-MM-dd HH:mm`，`ZoneId.systemDefault()`
- `epochToLocalDateTime()` L48-49
- `ClockService.currentTimeMillis()` 为仓库内「现在」的注入点

导出 schema `BehaviorExportItem.note: String?`（`BehaviorExportSchema.kt:27`），无事件列表字段。issue 写「导出/统计可后续扩展」。

---

### Issue #15 文件清单对照（按现有分层约定）

issue checklist + `docs/agent/04-patterns.md`（Entity / DAO / Repository 接口+impl / UseCase / Hilt `di/`）对应如下路径。下列「待创建」在仓库中均不存在；「现有文件」为 checklist 会触及的已有文件。

#### Data — 待创建

| 路径 | 对应 checklist |
|------|----------------|
| `core/data/src/main/java/com/nltimer/core/data/database/entity/BehaviorEventEntity.kt` | Entity，表 `behavior_event`，FK CASCADE，`Index("behaviorId")` |
| `core/data/src/main/java/com/nltimer/core/data/database/dao/BehaviorEventDao.kt` | insert / update / delete / observeByBehavior / nextOrder |
| `core/data/src/main/java/com/nltimer/core/data/model/BehaviorEvent.kt` | 领域模型 + `fromEntity()` / `toEntity()`（本仓库无独立 Mapper 类） |
| `core/data/src/main/java/com/nltimer/core/data/repository/BehaviorEventRepository.kt` | 接口 |
| `core/data/src/main/java/com/nltimer/core/data/repository/impl/BehaviorEventRepositoryImpl.kt` | 实现；`nextOrder`+`insert` 与 `BehaviorRepositoryImpl` 一样可用 `database.withTransaction` |
| `core/data/src/main/java/com/nltimer/core/data/usecase/AddBehaviorEventUseCase.kt` | Add |
| `core/data/src/main/java/com/nltimer/core/data/usecase/UpdateBehaviorEventUseCase.kt` | Update |
| `core/data/src/main/java/com/nltimer/core/data/usecase/DeleteBehaviorEventUseCase.kt` | Delete |
| `core/data/src/main/java/com/nltimer/core/data/usecase/ObserveBehaviorEventsUseCase.kt` | Observe（Flow） |
| `core/data/src/main/java/com/nltimer/core/data/database/migration/Migration16To17.kt` | `MIGRATION_16_17`，`CREATE TABLE behavior_event` + FK + INDEX |

#### Data — 现有文件（checklist 会改）

| 路径 | 触及点 |
|------|--------|
| `core/data/src/main/java/com/nltimer/core/data/database/NLtimerDatabase.kt` | `version = 17`；`entities` 加 `BehaviorEventEntity`；`behaviorEventDao()`；`ALL_MIGRATIONS` 加 `MIGRATION_16_17` |
| `core/data/src/main/java/com/nltimer/core/data/di/DatabaseModule.kt` | `provideBehaviorEventDao` |
| `core/data/src/main/java/com/nltimer/core/data/di/DataModule.kt` | `@Binds BehaviorEventRepository` |
| `core/data/schemas/.../NLtimerDatabase/17.json` | KSP `exportSchema = true` 编译后生成，非手写 |

#### Test — 待创建

| 路径 | 对应 checklist / 现有范例 |
|------|---------------------------|
| `core/data/src/androidTest/java/com/nltimer/core/data/database/migration/Migration16To17Test.kt` | issue「Migration 测试（16→17）」；范例 `feature/ai/.../MigrationTest.kt` |
| `core/data/src/androidTest/java/com/nltimer/core/data/database/dao/BehaviorEventDaoTest.kt` | issue「DAO 测试（增删改查、CASCADE、order 自增）」；范例 `ConversationDaoTest.kt` |
| `core/data/src/test/java/com/nltimer/core/data/repository/BehaviorEventRepositoryImplTest.kt` | 范例 `BehaviorRepositoryImplTest.kt` |
| `core/data/src/test/java/com/nltimer/core/data/usecase/AddBehaviorEventUseCaseTest.kt`（及 Update/Delete/Observe） | 范例 `AddBehaviorUseCaseTest.kt` |

`core/data` 目前无 `androidTest` 依赖；`room-testing` 仅在 `app/build.gradle.kts`。仪器测试也可放在已声明 `androidTestImplementation(libs.room.testing)` 的 `app` 模块。

#### Presentation/UI — 现有文件（checklist 会改）

| 路径 | 触及点 |
|------|--------|
| `feature/home/.../ui/components/moment/ActiveCard.kt` | 「打点」入口 + 事件列表区 |
| `feature/home/.../ui/components/MomentFocusCard.kt` | 向 ActiveCard 传递打点/列表回调 |
| `feature/home/.../viewmodel/HomeViewModel.kt` | 订阅 `ObserveBehaviorEvents`、调用 Add/Update/Delete |
| `feature/home/.../model/HomeUiState.kt` / `GridCellUiState.kt` / `BehaviorDetailUiState.kt` | 事件列表状态 |
| `feature/home/.../ui/HomeRoute.kt` / `HomeScreen.kt` | 接线 |
| `feature/home/.../ui/components/BehaviorDetailDialog.kt` | AC3：完成后查看/编辑事件 |

#### Presentation/UI — 待创建（issue：「事件输入轻量弹窗」「事件项展示」）

现有 `moment/` 包内卡片拆分为独立文件（`ActiveCard.kt`、`PendingCard.kt`、`TagNoteRow.kt`、`EmptyCard`）。同类拆分下的候选路径：

| 路径 | 对应 checklist |
|------|----------------|
| `feature/home/.../ui/components/moment/BehaviorEventList.kt` | LazyColumn 事件列表 |
| `feature/home/.../ui/components/moment/BehaviorEventItem.kt` | 时间戳 + 内容 + 编辑/删除 |
| `feature/home/.../ui/components/moment/AddBehaviorEventDialog.kt`（或 Sheet） | 轻量输入，不离开计时页 |

`ConversationMessage*` 没有 Repository/UseCase；主库行为层走 Entity → DAO → Repository → UseCase → Hilt。issue checklist 与主库分层一致，而不是 AI 模块的 ViewModel-直调-DAO。

---

### 行号速查

| 主题 | 位置 |
|------|------|
| DB version 16 | `NLtimerDatabase.kt:44` |
| ALL_MIGRATIONS | `NLtimerDatabase.kt:56-70` |
| BehaviorEntity.note | `BehaviorEntity.kt:38` |
| BehaviorDao.setNote | `BehaviorDao.kt:88-89` |
| BehaviorRepository.setNote | `BehaviorRepository.kt:29`；Impl `125-126` |
| getCurrentBehavior | `BehaviorDao.kt:105-106` |
| completeCurrentAndStartNext | `BehaviorRepositoryImpl.kt:131-162` |
| ConversationMessage CASCADE | `ConversationMessageEntity.kt:11-16` |
| nextOrder SQL | `ConversationMessageDao.kt:14-15` |
| observeByConversation | `ConversationMessageDao.kt:11-12` |
| AI CREATE TABLE + FK | `feature/ai/.../Migrations.kt:16-32` |
| MigrationTestHelper | `feature/ai/.../MigrationTest.kt:14-18, 21-30` |
| nextOrder 测试 | `ConversationDaoTest.kt:67-75` |
| CASCADE 测试 | `ConversationDaoTest.kt:54-64` |
| SlideActionPill 调用 | `ActiveCard.kt:165-171` |
| TagNoteRow 调用 | `ActiveCard.kt:173` |
| TagNoteRow 只读 | `TagNoteRow.kt:35-43` |
| 完成计时 | `HomeViewModel.kt:371-375` |
| 编辑 note（Sheet） | `HomeViewModel.kt:292-319`；`HomeSheetRouter.kt:116` |
| 详情 Dialog | `HomeScreen.kt:297-301`；`BehaviorDetailDialog.kt:60-123` |
| DatabaseModule | `DatabaseModule.kt:24-69` |
| DataModule binds | `DataModule.kt:24-31` |
| schema 16 behaviors | `16.json:368-369` |

## Caveats / Not Found

- `BehaviorEvent*` / 表 `behavior_event` / 字符串「事件记录」：源码中不存在。
- 主库 Room `MigrationTestHelper` 测试：不存在；`core/data` 无 androidTest。
- `HomeUiState.detailBehavior` / `isDetailSheetVisible`：已声明未使用。
- issue 写 ActiveCard「L195-201」：当前文件 193 行，`TagNoteRow` 在 L173。
- `conversation_message` 在 `AiInterDatabase`（v4），不在 `NLtimerDatabase`（v16）；无 ConversationMessage Repository。
- GitHub MCP 对本会话返回 `invalid session`；issue 正文通过 `gh issue view 15 --repo Arsucar/NLtimer` 取得。
- `docs/agent/01-project-overview.md` 的 Room 版本/表数与代码（16 / 8 entity）不一致。
