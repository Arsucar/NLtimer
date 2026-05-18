# AI 助手对话页 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在 NLtimer AI Inter 板块新增独立路由 `AI_ASSISTANT_CHAT`，对齐 rikkahub 风格的多会话 AI 对话界面（毛玻璃输入栏 + 抽屉式会话切换 + 圆角气泡 + Markdown/代码高亮/表格渲染 + 工具调用流式 + 对话导出）。

**架构：** 复用现有 `AiInterApiClient`/`ToolRegistry`/`AiInterRepository`；新增 `chat/` 子包内含 ViewModel + Composable 组件 + 移植 rikkahub markdown 子集（GFM+代码高亮+表格+图片）和 highlight 模块；数据层新增 `conversation` + `conversation_message` 两张 Room 表，`AiInterDatabase` v3→v4 + 显式 Migration_3_4。

**技术栈：** Kotlin / Jetpack Compose / Material3 / Hilt / Room / OkHttp-SSE / kotlinx.serialization / `dev.chrisbanes.haze` / `io.coil-kt.coil3` / `org.jetbrains:markdown` / `org.jsoup:jsoup` / `wang.harlon.quickjs:wrapper-android`

**参考文档：** [docs/superpowers/specs/2026-05-18-ai-assistant-chat-design.md](../specs/2026-05-18-ai-assistant-chat-design.md)

---

## 关键修正（相对 spec）

- DB 版本：spec 写"v2→v3"是误判，实际 `AiInterDatabase.kt` 当前 `version = 3`，正确升级是 **v3→v4** + Migration_3_4
- 保留 `fallbackToDestructiveMigration(true)` 兜底（旧 `ai_call_log` 已被允许丢失），同时显式追加 Migration_3_4 确保正常升级路径下数据不丢
- 测试位置：项目无 `app/src/test/`，所有 Room/Compose 测试走 `app/src/androidTest/`

---

## 阶段总览

| 阶段 | 范围 | Commit 数 | 主要交付 |
|---|---|---|---|
| 1 | 依赖 + 数据层 + Migration | 3 | libs + Entity/DAO + Migration_3_4，单元测试 |
| 2 | ConversationExporter（数据驱动） | 1 | Markdown / JSON 序列化 + snapshot tests |
| 3 | ViewModel + 工具循环 | 2 | AiAssistantChatViewModel + Fake 集成测试 |
| 4 | highlight 模块移植 + Markdown 核心 | 3 | Highlighter + MarkdownBlock（GFM+代码+图片） |
| 5 | Markdown 表格 | 1 | DataTable + HtmlTable 分支 |
| 6 | UI：ChatScreen 全套（无毛玻璃） | 4 | ChatTopBar/Input/List/Message/Drawer/ExportSheet |
| 7 | Haze 毛玻璃 + 跳转按钮 + 路由接通 | 2 | hazeEffect + MessageJumper + AI_ASSISTANT_CHAT 路由 |

每个 commit 都应可独立编译。

---

## 阶段 1：依赖与数据层（3 commits）

### 任务 1.1：引入新依赖

**文件：**
- 修改：`gradle/libs.versions.toml`
- 修改：`app/build.gradle.kts`

- [ ] **步骤 1：编辑 libs.versions.toml，在 `[versions]` 段追加版本号**

```toml
haze = "1.6.0"
coil3 = "3.0.4"
intellijMarkdown = "0.7.3"
jsoup = "1.18.3"
quickjs = "1.1.0"
```

- [ ] **步骤 2：在 `[libraries]` 段追加 library 定义**

```toml
haze                 = { module = "dev.chrisbanes.haze:haze",                version.ref = "haze" }
haze-materials       = { module = "dev.chrisbanes.haze:haze-materials",      version.ref = "haze" }
coil3-compose        = { module = "io.coil-kt.coil3:coil-compose",           version.ref = "coil3" }
coil3-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp",    version.ref = "coil3" }
intellij-markdown    = { module = "org.jetbrains:markdown",                  version.ref = "intellijMarkdown" }
jsoup                = { module = "org.jsoup:jsoup",                         version.ref = "jsoup" }
quickjs-android      = { module = "wang.harlon.quickjs:wrapper-android",     version.ref = "quickjs" }
```

- [ ] **步骤 3：编辑 app/build.gradle.kts，在 dependencies 块内 androidTestImplementation 之前追加**

```kotlin
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.intellij.markdown)
    implementation(libs.jsoup)
    implementation(libs.quickjs.android)
```

- [ ] **步骤 4：同步 Gradle，验证编译**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL（仅引入依赖，未使用）

如果 `wang.harlon.quickjs:wrapper-android:1.1.0` 拉不到，先确认 `settings.gradle.kts` 的 repositories 已包含 `mavenCentral()` 与 `google()`；如版本号不存在则查最新版替换。

- [ ] **步骤 5：Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build(deps): 引入 haze/coil3/intellij-markdown/jsoup/quickjs 依赖

为后续 AI 助手对话页移植 rikkahub 渲染方案做准备：
- haze: 输入栏/抽屉的毛玻璃模糊
- coil3: 图片加载（Markdown 内嵌图与未来扩展）
- intellij-markdown + jsoup: Markdown 解析 → HTML → Compose
- quickjs-android: highlight.js 代码高亮 JS 引擎"
```

---

### 任务 1.2：定义 Conversation 数据模型 + DAO + Migration

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/data/ConversationEntity.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/data/ConversationMessageEntity.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/data/ConversationDao.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/data/ConversationMessageDao.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/data/Migrations.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/data/AiInterDatabase.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/di/AiInterModule.kt`
- 测试：`app/src/androidTest/java/com/nltimer/app/experimental/ai_inter/chat/data/ConversationDaoTest.kt`

- [ ] **步骤 1：编写 ConversationDao 测试（先 fail）**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nltimer.app.experimental.ai_inter.data.AiInterDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConversationDaoTest {

    private lateinit var db: AiInterDatabase
    private lateinit var convDao: ConversationDao
    private lateinit var msgDao: ConversationMessageDao

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AiInterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        convDao = db.conversationDao()
        msgDao = db.conversationMessageDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun upsertAndObserveAll_returnsConversationOrderedByUpdatedAtDesc() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        convDao.upsert(ConversationEntity("b", "B", 2L, 10L))
        convDao.upsert(ConversationEntity("c", "C", 3L, 5L))

        val list = convDao.observeAll().first()
        assertEquals(listOf("b", "c", "a"), list.map { it.id })
    }

    @Test
    fun rename_updatesTitleAndTimestamp() = runBlocking {
        convDao.upsert(ConversationEntity("a", "old", 1L, 1L))
        convDao.rename("a", "new", 99L)

        val got = convDao.get("a")!!
        assertEquals("new", got.title)
        assertEquals(99L, got.updatedAt)
    }

    @Test
    fun delete_cascadesToMessages() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        msgDao.insert(ConversationMessageEntity("m1", "a", 0, "user", "hi", "", "", 1L))
        msgDao.insert(ConversationMessageEntity("m2", "a", 1, "assistant", "ok", "", "", 2L))

        convDao.delete("a")

        val msgs = msgDao.observeByConversation("a").first()
        assertEquals(emptyList<ConversationMessageEntity>(), msgs)
        assertNull(convDao.get("a"))
    }

    @Test
    fun nextOrder_returnsZeroForEmpty_thenIncrements() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        assertEquals(0, msgDao.nextOrder("a"))

        msgDao.insert(ConversationMessageEntity("m1", "a", 0, "user", "hi", "", "", 1L))
        assertEquals(1, msgDao.nextOrder("a"))

        msgDao.insert(ConversationMessageEntity("m2", "a", 5, "assistant", "ok", "", "", 2L))
        assertEquals(6, msgDao.nextOrder("a"))
    }

    @Test
    fun deleteAllInConversation_clearsMessagesKeepsConversation() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        msgDao.insert(ConversationMessageEntity("m1", "a", 0, "user", "hi", "", "", 1L))
        msgDao.insert(ConversationMessageEntity("m2", "a", 1, "assistant", "ok", "", "", 2L))

        msgDao.deleteAllInConversation("a")

        assertEquals(emptyList<ConversationMessageEntity>(), msgDao.observeByConversation("a").first())
        assertEquals("A", convDao.get("a")?.title)
    }
}
```

- [ ] **步骤 2：运行测试，确认编译失败（DAO/Entity 不存在）**

运行：`./gradlew :app:compileDebugAndroidTestKotlin`
预期：编译错误，找不到 `ConversationDao`/`ConversationMessageDao`/`ConversationEntity`/`ConversationMessageEntity`/`AiInterDatabase.conversationDao()`

- [ ] **步骤 3：创建 ConversationEntity**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **步骤 4：创建 ConversationMessageEntity**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

- [ ] **步骤 5：创建 ConversationDao**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conv: ConversationEntity)

    @Query("UPDATE conversation SET title = :title, updatedAt = :ts WHERE id = :id")
    suspend fun rename(id: String, title: String, ts: Long)

    @Query("UPDATE conversation SET updatedAt = :ts WHERE id = :id")
    suspend fun touch(id: String, ts: Long)

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun delete(id: String)
}
```

- [ ] **步骤 6：创建 ConversationMessageDao**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_message WHERE conversationId = :id ORDER BY `order` ASC")
    fun observeByConversation(id: String): Flow<List<ConversationMessageEntity>>

    @Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM conversation_message WHERE conversationId = :id")
    suspend fun nextOrder(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ConversationMessageEntity)

    @Query("DELETE FROM conversation_message WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversation_message WHERE conversationId = :id AND `order` >= :fromOrder")
    suspend fun deleteFromOrder(id: String, fromOrder: Int)

    @Query("DELETE FROM conversation_message WHERE conversationId = :id")
    suspend fun deleteAllInConversation(id: String)
}
```

- [ ] **步骤 7：创建 Migration_3_4**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conversation (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conversation_message (
                id TEXT NOT NULL PRIMARY KEY,
                conversationId TEXT NOT NULL,
                `order` INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                reasoning TEXT NOT NULL DEFAULT '',
                toolCallsJson TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(conversationId) REFERENCES conversation(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_conversation_message_conversationId
            ON conversation_message(conversationId)
        """.trimIndent())
    }
}
```

- [ ] **步骤 8：升级 AiInterDatabase 到 v4**

替换 `AiInterDatabase.kt` 全文为：

```kotlin
package com.nltimer.app.experimental.ai_inter.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity

@Database(
    entities = [
        AiCallLogEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AiInterDatabase : RoomDatabase() {
    abstract fun aiCallLogDao(): AiCallLogDao
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationMessageDao(): ConversationMessageDao
}
```

- [ ] **步骤 9：更新 AiInterModule 注入新 DAO + 注册 migration**

替换 `AiInterModule.kt` 全文为：

```kotlin
package com.nltimer.app.experimental.ai_inter.di

import android.content.Context
import androidx.room.Room
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageDao
import com.nltimer.app.experimental.ai_inter.chat.data.MIGRATION_3_4
import com.nltimer.app.experimental.ai_inter.data.AiCallLogDao
import com.nltimer.app.experimental.ai_inter.data.AiInterDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiInterModule {

    @Provides
    @Singleton
    fun provideAiInterDatabase(@ApplicationContext context: Context): AiInterDatabase {
        return Room.databaseBuilder(
            context,
            AiInterDatabase::class.java,
            "ai_inter_database",
        )
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideAiCallLogDao(database: AiInterDatabase): AiCallLogDao =
        database.aiCallLogDao()

    @Provides
    fun provideConversationDao(database: AiInterDatabase): ConversationDao =
        database.conversationDao()

    @Provides
    fun provideConversationMessageDao(database: AiInterDatabase): ConversationMessageDao =
        database.conversationMessageDao()
}
```

- [ ] **步骤 10：运行测试，确认全部通过**

需先启动 Android 模拟器或连接真机。

运行：`./gradlew :app:connectedDebugAndroidTest --tests com.nltimer.app.experimental.ai_inter.chat.data.ConversationDaoTest`
预期：5 tests PASS

- [ ] **步骤 11：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/data/ \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/data/AiInterDatabase.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/di/AiInterModule.kt \
        app/src/androidTest/java/com/nltimer/app/experimental/ai_inter/chat/data/ConversationDaoTest.kt
git commit -m "feat(ai_inter): 添加 Conversation Room 表与 DAO

- 新增 ConversationEntity / ConversationMessageEntity 表，外键级联删除
- 新增 ConversationDao / ConversationMessageDao，支持观察/upsert/重命名/touch/级联/单删/范围删/清空
- AiInterDatabase v3→v4，追加 MIGRATION_3_4 同时保留 destructive 兜底
- AiInterModule 注入两个新 DAO
- 配套 5 个 androidTest 验证 CRUD/级联/序号自增/清空"
```

---

### 任务 1.3：Migration_3_4 验证测试

**文件：**
- 修改：`gradle/libs.versions.toml`（追加 room-testing 库）
- 修改：`app/build.gradle.kts`（追加 androidTestImplementation）
- 测试：`app/src/androidTest/java/com/nltimer/app/experimental/ai_inter/chat/data/MigrationTest.kt`

- [ ] **步骤 1：在 libs.versions.toml `[libraries]` 段追加（room 版本应已有）**

```toml
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
```

若 libs 中 room 的 version.ref 名不叫 `room`（可能叫 `roomVersion`），保持一致。如不确定，运行 Grep 找 `androidx.room:room-runtime` 那行查 ref 名。

- [ ] **步骤 2：在 app/build.gradle.kts dependencies 内追加**

```kotlin
    androidTestImplementation(libs.room.testing)
```

- [ ] **步骤 3：编写 Migration 测试**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.nltimer.app.experimental.ai_inter.data.AiInterDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AiInterDatabase::class.java,
    )

    @Test
    fun migrate3to4_preservesAiCallLogs_andCreatesConversationTables() {
        // v3 schema 写一条 ai_call_log
        helper.createDatabase(testDbName, 3).apply {
            execSQL("""
                INSERT INTO ai_call_logs (timestamp, type, status, durationMs, model, tools, prompt, response, errorMessage, requestUrl, requestTokens, responseTokens, reasoning, toolCallsJson)
                VALUES (1000, 'Test', 'Success', 100, 'gpt', '', 'hi', 'hello', NULL, '', 0, 0, '', '')
            """.trimIndent())
            close()
        }

        helper.runMigrationsAndValidate(testDbName, 4, true, MIGRATION_3_4).use { db ->
            // 老表数据保留
            db.query("SELECT prompt FROM ai_call_logs WHERE timestamp = 1000").use {
                assertEquals(true, it.moveToFirst())
                assertEquals("hi", it.getString(0))
            }

            // 新表可写
            db.execSQL("INSERT INTO conversation VALUES ('c1', 'title', 100, 100)")
            db.execSQL("INSERT INTO conversation_message VALUES ('m1', 'c1', 0, 'user', 'msg', '', '', 100)")

            db.query("SELECT content FROM conversation_message WHERE id = 'm1'").use {
                assertEquals(true, it.moveToFirst())
                assertEquals("msg", it.getString(0))
            }
        }
    }
}
```

- [ ] **步骤 4：运行 migration 测试**

运行：`./gradlew :app:connectedDebugAndroidTest --tests com.nltimer.app.experimental.ai_inter.chat.data.MigrationTest`
预期：1 test PASS

- [ ] **步骤 5：Commit**

```bash
git add app/src/androidTest/java/com/nltimer/app/experimental/ai_inter/chat/data/MigrationTest.kt \
        gradle/libs.versions.toml app/build.gradle.kts
git commit -m "test(ai_inter): 验证 Migration_3_4 保留旧数据并建新表

引入 androidx.room:room-testing 依赖；构造 v3 schema 写入一条 ai_call_log，
跑 migration 后断言旧记录保留，且 conversation/conversation_message 表可正常写入。"
```

---

## 阶段 2：ConversationExporter（1 commit）

### 任务 2.1：ExportFormat + ConversationExporter + snapshot tests

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/export/ExportFormat.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/export/ConversationExporter.kt`
- 测试：`app/src/androidTest/java/com/nltimer/app/experimental/ai_inter/chat/export/ConversationExporterTest.kt`

- [ ] **步骤 1：创建 ExportFormat / ExportOptions**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.export

enum class ExportFormat {
    MARKDOWN,
    JSON,
}

data class ExportOptions(
    val includeTools: Boolean = true,
    val includeReasoning: Boolean = true,
)
```

- [ ] **步骤 2：编写 Exporter 测试（先 fail）**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.export

import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationExporterTest {

    private val exporter = ConversationExporter()

    private val conv = ConversationEntity(
        id = "c1",
        title = "测试对话",
        createdAt = 1763438215000L,
        updatedAt = 1763438218000L,
    )

    private val msgs = listOf(
        ConversationMessageEntity(
            id = "m1", conversationId = "c1", order = 0,
            role = "user", content = "请帮我记录看书 10-14 点",
            reasoning = "", toolCallsJson = "",
            createdAt = 1763438215000L,
        ),
        ConversationMessageEntity(
            id = "m2", conversationId = "c1", order = 1,
            role = "assistant", content = "已为你记录看书活动",
            reasoning = "用户指定了时间区间",
            toolCallsJson = """[{"id":"call_1","name":"recordBehavior","arguments":"{\"a\":1}","result":"{\"id\":42}","success":true,"durationMs":142}]""",
            createdAt = 1763438218000L,
        ),
    )

    @Test
    fun exportMarkdown_full_containsAllSections() {
        val md = exporter.exportMarkdown(conv, msgs, ExportOptions(includeTools = true, includeReasoning = true))

        assertTrue("title", md.contains("# 测试对话"))
        assertTrue("user msg", md.contains("请帮我记录看书 10-14 点"))
        assertTrue("assistant msg", md.contains("已为你记录看书活动"))
        assertTrue("reasoning", md.contains("用户指定了时间区间"))
        assertTrue("tool name", md.contains("recordBehavior"))
        assertTrue("tool args", md.contains("\"a\":1"))
        assertTrue("tool result", md.contains("\"id\":42"))
        assertTrue("duration", md.contains("142ms"))
    }

    @Test
    fun exportMarkdown_excludeTools_omitsToolBlock() {
        val md = exporter.exportMarkdown(conv, msgs, ExportOptions(includeTools = false, includeReasoning = true))
        assertTrue("user msg", md.contains("请帮我记录看书 10-14 点"))
        assertTrue("reasoning kept", md.contains("用户指定了时间区间"))
        assertTrue("no tool name", !md.contains("recordBehavior"))
    }

    @Test
    fun exportMarkdown_excludeReasoning_omitsReasoningBlock() {
        val md = exporter.exportMarkdown(conv, msgs, ExportOptions(includeTools = true, includeReasoning = false))
        assertTrue("no reasoning", !md.contains("用户指定了时间区间"))
        assertTrue("tools kept", md.contains("recordBehavior"))
    }

    @Test
    fun exportJson_full_isValidJsonContainingAllFields() {
        val json = exporter.exportJson(conv, msgs, ExportOptions(includeTools = true, includeReasoning = true))

        assertTrue("schemaVersion", json.contains("\"schemaVersion\":1"))
        assertTrue("conv id", json.contains("\"id\":\"c1\""))
        assertTrue("conv title", json.contains("\"title\":\"测试对话\""))
        assertTrue("msg order", json.contains("\"order\":0"))
        assertTrue("tool name", json.contains("\"name\":\"recordBehavior\""))
        assertTrue("durationMs", json.contains("\"durationMs\":142"))
    }

    @Test
    fun exportJson_excludeTools_omitsToolCallsArray() {
        val json = exporter.exportJson(conv, msgs, ExportOptions(includeTools = false, includeReasoning = true))
        assertTrue("no tool calls", !json.contains("\"toolCalls\""))
        assertTrue("reasoning kept", json.contains("\"reasoning\""))
    }

    @Test
    fun exportMarkdown_emptyConversation_hasHeaderOnly() {
        val md = exporter.exportMarkdown(conv, emptyList(), ExportOptions())
        assertTrue("title", md.contains("# 测试对话"))
        assertTrue("no role section", !md.contains("## 用户") && !md.contains("## 助手"))
    }
}
```

- [ ] **步骤 3：实现 ConversationExporter**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.export

import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ConversationExporter @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun exportMarkdown(
        conversation: ConversationEntity,
        messages: List<ConversationMessageEntity>,
        options: ExportOptions,
    ): String = buildString {
        appendLine("# ${conversation.title}")
        appendLine()
        appendLine("- 创建时间：${dateFmt.format(Date(conversation.createdAt))}")
        appendLine("- 导出时间：${dateFmt.format(Date(System.currentTimeMillis()))}")
        appendLine("- 消息数：${messages.size}")
        appendLine()
        appendLine("---")
        appendLine()

        messages.forEach { msg ->
            val roleLabel = if (msg.role == "user") "用户" else "助手"
            appendLine("## $roleLabel · ${timeFmt.format(Date(msg.createdAt))}")
            appendLine()

            if (msg.role == "assistant") {
                if (options.includeReasoning && msg.reasoning.isNotBlank()) {
                    appendLine("<details><summary>思考过程</summary>")
                    appendLine()
                    appendLine(msg.reasoning)
                    appendLine()
                    appendLine("</details>")
                    appendLine()
                }

                if (options.includeTools && msg.toolCallsJson.isNotBlank()) {
                    val calls = runCatching {
                        json.parseToJsonElement(msg.toolCallsJson).jsonArray
                    }.getOrNull()
                    if (calls != null && calls.isNotEmpty()) {
                        appendLine("<details><summary>工具调用 (${calls.size})</summary>")
                        appendLine()
                        calls.forEachIndexed { idx, callEl ->
                            val call = callEl.jsonObject
                            val name = call["name"]?.jsonPrimitive?.content.orEmpty()
                            val args = call["arguments"]?.jsonPrimitive?.content.orEmpty()
                            val result = call["result"]?.jsonPrimitive?.content.orEmpty()
                            val success = call["success"]?.jsonPrimitive?.content == "true"
                            val duration = call["durationMs"]?.jsonPrimitive?.content.orEmpty()

                            appendLine("### ${idx + 1}. $name · ${duration}ms · ${if (success) "成功" else "失败"}")
                            appendLine()
                            appendLine("**参数：**")
                            appendLine("```json")
                            appendLine(args)
                            appendLine("```")
                            appendLine()
                            appendLine("**结果：**")
                            appendLine("```json")
                            appendLine(result)
                            appendLine("```")
                            appendLine()
                        }
                        appendLine("</details>")
                        appendLine()
                    }
                }
            }

            appendLine(msg.content)
            appendLine()
            appendLine("---")
            appendLine()
        }
    }

    fun exportJson(
        conversation: ConversationEntity,
        messages: List<ConversationMessageEntity>,
        options: ExportOptions,
    ): String {
        val obj = buildJsonObject {
            put("conversation", buildJsonObject {
                put("id", conversation.id)
                put("title", conversation.title)
                put("createdAt", conversation.createdAt)
                put("updatedAt", conversation.updatedAt)
            })
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("id", msg.id)
                        put("order", msg.order)
                        put("role", msg.role)
                        put("content", msg.content)
                        put("createdAt", msg.createdAt)

                        if (options.includeReasoning && msg.reasoning.isNotBlank()) {
                            put("reasoning", msg.reasoning)
                        }
                        if (options.includeTools && msg.toolCallsJson.isNotBlank()) {
                            val calls = runCatching {
                                json.parseToJsonElement(msg.toolCallsJson).jsonArray
                            }.getOrNull()
                            if (calls != null) {
                                put("toolCalls", calls)
                            }
                        }
                    })
                }
            })
            put("exportedAt", System.currentTimeMillis())
            put("schemaVersion", 1)
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }
}
```

- [ ] **步骤 4：运行测试**

运行：`./gradlew :app:connectedDebugAndroidTest --tests com.nltimer.app.experimental.ai_inter.chat.export.ConversationExporterTest`
预期：6 tests PASS

注意：测试用例中 `toolCallsJson` 里 `"success":true` 是 boolean；`json.parseToJsonElement(...).jsonArray[i].jsonObject["success"]?.jsonPrimitive?.content` 会得到字符串 `"true"`，所以判断 `== "true"` 是正确的。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/export/ \
        app/src/androidTest/java/com/nltimer/app/experimental/ai_inter/chat/export/
git commit -m "feat(ai_inter): 实现 ConversationExporter（Markdown + JSON）

支持导出对话为人类可读 Markdown 或机器友好 JSON，可分别控制是否包含
reasoning 与 toolCalls；JSON 含 schemaVersion=1 便于未来工具多合一分析。
6 个 snapshot 测试覆盖：全量/无工具/无 reasoning/JSON 校验/空对话。"
```

---

## 阶段 3：ViewModel（2 commits）

### 任务 3.1：UI 状态聚合 + ViewModel 骨架（无 sendMessage）

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatUiState.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatViewModel.kt`

- [ ] **步骤 1：创建 UI 状态聚合**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat

import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord

data class StreamingState(
    val reasoning: String = "",
    val content: String = "",
    val toolCalls: List<ToolCallRecord> = emptyList(),
) {
    val isEmpty: Boolean get() = reasoning.isEmpty() && content.isEmpty() && toolCalls.isEmpty()
}
```

注：直接复用 `com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord`（现有 AiInterViewModel.kt 中已定义，6 字段：id/name/arguments/result/success/durationMs）。

- [ ] **步骤 2：创建 ViewModel 骨架**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import com.nltimer.app.experimental.ai_inter.chat.export.ConversationExporter
import com.nltimer.app.experimental.ai_inter.chat.export.ExportFormat
import com.nltimer.app.experimental.ai_inter.chat.export.ExportOptions
import com.nltimer.app.experimental.ai_inter.data.AiInterConfig
import com.nltimer.app.experimental.ai_inter.data.AiInterRepository
import com.nltimer.app.experimental.ai_inter.network.AiInterApiClient
import com.nltimer.core.tools.ToolRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AiAssistantChatViewModel @Inject constructor(
    private val repository: AiInterRepository,
    private val conversationDao: ConversationDao,
    private val messageDao: ConversationMessageDao,
    private val apiClient: AiInterApiClient,
    private val toolRegistry: ToolRegistry,
    private val exporter: ConversationExporter,
) : ViewModel() {

    val config: StateFlow<AiInterConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiInterConfig(),
    )

    val conversations: StateFlow<List<ConversationEntity>> = conversationDao.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    val currentMessages: StateFlow<List<ConversationMessageEntity>> = _currentConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else messageDao.observeByConversation(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _streamingState = MutableStateFlow(StreamingState())
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            val list = conversationDao.observeAll().first()
            if (list.isNotEmpty()) {
                _currentConversationId.value = list.first().id
            } else {
                newConversation()
            }
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
    }

    fun newConversation() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            conversationDao.upsert(ConversationEntity(id, "新对话", now, now))
            _currentConversationId.value = id
        }
    }

    fun renameConversation(id: String, title: String) {
        viewModelScope.launch {
            val trimmed = title.trim().ifBlank { "新对话" }
            conversationDao.rename(id, trimmed, System.currentTimeMillis())
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.delete(id)
            if (_currentConversationId.value == id) {
                val remaining = conversationDao.observeAll().first()
                _currentConversationId.value = remaining.firstOrNull()?.id
                if (_currentConversationId.value == null) {
                    newConversation()
                }
            }
        }
    }

    fun clearCurrent() {
        viewModelScope.launch {
            val id = _currentConversationId.value ?: return@launch
            messageDao.deleteAllInConversation(id)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch { messageDao.delete(id) }
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun stopStreaming() {
        streamJob?.cancel()
    }

    fun copyMessage(id: String): String =
        currentMessages.value.firstOrNull { it.id == id }?.content.orEmpty()

    fun exportConversation(
        id: String,
        format: ExportFormat,
        options: ExportOptions,
    ): String {
        val conv = conversations.value.firstOrNull { it.id == id } ?: return ""
        val msgs = currentMessages.value
        return when (format) {
            ExportFormat.MARKDOWN -> exporter.exportMarkdown(conv, msgs, options)
            ExportFormat.JSON -> exporter.exportJson(conv, msgs, options)
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            val cfg = config.value
            apiClient.fetchModels(cfg.apiAddress, cfg.apiKey).fold(
                onSuccess = { _availableModels.value = it },
                onFailure = { _availableModels.value = emptyList() },
            )
        }
    }

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            repository.updateConfig { it.copy(modelName = modelName) }
        }
    }

    fun sendMessage(text: String) {
        TODO("implemented in 任务 3.2")
    }

    fun regenerateLastAssistant() {
        TODO("implemented in 任务 3.2")
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
```

- [ ] **步骤 3：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL（`TODO()` 会有 warning）

- [ ] **步骤 4：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatUiState.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatViewModel.kt
git commit -m "feat(ai_inter): 添加 AiAssistantChatViewModel 骨架与 UI 状态

聚合 conversations / currentMessages / streamingState / config / availableModels 等 Flow，
包含会话管理（新建/切换/重命名/删除/清空）+ 消息删除/复制 + 导出 + 模型刷新等纯增删查改逻辑。
sendMessage/regenerateLastAssistant 暂留 TODO，下个 commit 接入工具循环。"
```

---

### 任务 3.2：实现 sendMessage 与 regenerateLastAssistant

**文件：**
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatViewModel.kt`

- [ ] **步骤 1：在 ViewModel 文件顶部追加 imports**

```kotlin
import com.nltimer.app.experimental.ai_inter.data.AiCallLogEntity
import com.nltimer.app.experimental.ai_inter.network.StreamEvent
import com.nltimer.app.experimental.ai_inter.network.toOpenAiFunctionJson
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord
import com.nltimer.core.tools.ToolError
import com.nltimer.core.tools.ToolResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
```

- [ ] **步骤 2：在 ViewModel 类末尾追加 companion object + 内部类**

```kotlin
    companion object {
        private const val MAX_TOOL_ROUNDS = 5
        private const val DEFAULT_TITLE = "新对话"
        private const val TITLE_MAX_LEN = 24

        private const val TOOLS_SYSTEM_PROMPT = """# 工具调用规则（强制）

## 1. 时间解析
- "10 点到 14 点" → 当天本地时区 10:00-14:00，ISO 8601 含时区偏移
- "15 点午休 30 分钟" → 起 15:00 止 15:30
- "现在结束计时" / "结束" → 直接调 endBehavior()，不要传时间参数

## 2. 默认值兜底（不要主动追问）
- 用户没指定活动分类时，createActivity 默认 groupName="预制菜"
- 用户没指定标签分类时，createTag 默认 category="预制菜"
- 颜色缺失时工具内自动生成莫奈中和色，无需问用户
- 标签图标缺失时默认 "#"

## 3. 多步序列分支
当用户说"先做 X，再做 Y"或一连串任务时：
1. 先 queryCurrentBehavior 看是否有 ACTIVE
2. 有 ACTIVE → 所有任务都走 createGoal（按顺序排队）
3. 无 ACTIVE → 第 1 个用 startBehavior 立即开始，剩下走 createGoal

## 4. 冲突处理
当 recordBehavior 返回 ValidationError 且 message 是 JSON 含 "code":"CONFLICT" 时：
1. 解析 message 里的 conflicts 数组（每项含 id / activityName / startTime / endTime）
2. 向用户复述冲突区间，给出三个选项：
   - 覆盖：对每个 conflict.id 调 deleteBehavior(id)，然后重发 recordBehavior
   - 取消：不做任何动作，告诉用户已取消
   - 调整时间：让用户给新时间，再发 recordBehavior

## 5. "查看所有标签"
- 默认 listTags() 返回未归档标签
- 用户明确说"包括归档"时才传 includeArchived=true
"""
    }

    private class ToolCallBuffer {
        var id: String? = null
        var name: String? = null
        val arguments: StringBuilder = StringBuilder()
    }
```

- [ ] **步骤 3：替换 sendMessage 实现**

将 `fun sendMessage(text: String) { TODO(...) }` 替换为：

```kotlin
    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return
        val conversationId = _currentConversationId.value ?: return
        _isSending.value = true
        _streamingState.value = StreamingState()
        _chatError.value = null

        streamJob = viewModelScope.launch {
            val cfg = config.value
            val now = System.currentTimeMillis()

            // 1. 写入 user 消息
            val userOrder = messageDao.nextOrder(conversationId)
            messageDao.insert(
                ConversationMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    order = userOrder,
                    role = "user",
                    content = text,
                    createdAt = now,
                )
            )
            conversationDao.touch(conversationId, now)

            // 首条用户消息自动填标题
            val conv = conversationDao.get(conversationId)
            if (conv != null && conv.title == DEFAULT_TITLE) {
                val title = text.lineSequence().firstOrNull()?.take(TITLE_MAX_LEN)?.trim().orEmpty()
                if (title.isNotBlank()) conversationDao.rename(conversationId, title, now)
            }

            // 2. 构建 wire history
            val history = messageDao.observeByConversation(conversationId).first()
            val workingHistory = buildWireHistory(cfg, history).toMutableList()

            val toolDefs = toolRegistry.getAllTools()
            val toolsJson: JsonArray? = if (toolDefs.isNotEmpty()) {
                buildJsonArray { toolDefs.forEach { add(it.toOpenAiFunctionJson()) } }
            } else null

            // 3. 流式 + 工具循环
            val finalReasoning = StringBuilder()
            val finalContent = StringBuilder()
            val allToolCalls = mutableListOf<ToolCallRecord>()
            var errorMsg: String? = null
            var wasCancelled = false
            val startTime = System.currentTimeMillis()
            val fullUrl = cfg.apiAddress.trimEnd('/') + cfg.apiPath

            try {
                var round = 0
                while (round < MAX_TOOL_ROUNDS) {
                    val roundReasoning = StringBuilder()
                    val roundContent = StringBuilder()
                    val toolBuffers = mutableMapOf<Int, ToolCallBuffer>()

                    apiClient.streamChat(
                        baseUrl = cfg.apiAddress,
                        path = cfg.apiPath,
                        apiKey = cfg.apiKey,
                        model = cfg.modelName,
                        messagesJson = JsonArray(workingHistory),
                        toolsJson = toolsJson,
                    ).collect { event ->
                        when (event) {
                            is StreamEvent.Content -> {
                                roundContent.append(event.text)
                                finalContent.append(event.text)
                                emitStreaming(finalReasoning, finalContent, allToolCalls)
                            }
                            is StreamEvent.Reasoning -> {
                                roundReasoning.append(event.text)
                                emitStreaming(StringBuilder(finalReasoning).append(roundReasoning), finalContent, allToolCalls)
                            }
                            is StreamEvent.ToolCallDelta -> {
                                val buf = toolBuffers.getOrPut(event.index) { ToolCallBuffer() }
                                event.id?.let { buf.id = it }
                                event.name?.let { buf.name = it }
                                buf.arguments.append(event.argumentsChunk)
                            }
                        }
                    }

                    if (roundReasoning.isNotEmpty()) {
                        if (finalReasoning.isNotEmpty()) finalReasoning.append("\n")
                        finalReasoning.append(roundReasoning)
                    }

                    if (toolBuffers.isEmpty()) break

                    // 写入 assistant 的 tool_calls 消息到 wire history
                    workingHistory += buildJsonObject {
                        put("role", "assistant")
                        put("content", roundContent.toString())
                        putJsonArray("tool_calls") {
                            toolBuffers.toSortedMap().forEach { (_, buf) ->
                                add(buildJsonObject {
                                    put("id", buf.id ?: "")
                                    put("type", "function")
                                    putJsonObject("function") {
                                        put("name", buf.name ?: "")
                                        put("arguments", buf.arguments.toString())
                                    }
                                })
                            }
                        }
                    }

                    // 执行每个工具，回传结果
                    toolBuffers.toSortedMap().forEach { (_, buf) ->
                        val record = executeToolCall(buf)
                        allToolCalls += record
                        emitStreaming(finalReasoning, finalContent, allToolCalls)
                        workingHistory += buildJsonObject {
                            put("role", "tool")
                            put("tool_call_id", record.id)
                            put("name", record.name)
                            put("content", record.result)
                        }
                    }

                    round++
                }
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            } catch (e: Exception) {
                errorMsg = e.message ?: e::class.simpleName ?: "Unknown error"
            } finally {
                val duration = System.currentTimeMillis() - startTime
                val finalContentStr = finalContent.toString()
                val finalReasoningStr = finalReasoning.toString()
                val finalToolCalls = allToolCalls.toList()

                val assistantContent = when {
                    wasCancelled && finalContentStr.isNotEmpty() -> "$finalContentStr\n\n(已中断)"
                    wasCancelled -> ""
                    errorMsg != null && finalContentStr.isNotEmpty() -> "$finalContentStr\n\n(中途出错：$errorMsg)"
                    errorMsg != null -> "(请求失败：$errorMsg)"
                    finalContentStr.isNotEmpty() -> finalContentStr
                    finalToolCalls.isNotEmpty() -> "(已完成 ${finalToolCalls.size} 次工具调用，但模型未给出文本回复)"
                    else -> "(空响应)"
                }

                if (assistantContent.isNotEmpty()) {
                    val assistantOrder = messageDao.nextOrder(conversationId)
                    messageDao.insert(
                        ConversationMessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            order = assistantOrder,
                            role = "assistant",
                            content = assistantContent,
                            reasoning = finalReasoningStr,
                            toolCallsJson = serializeToolCalls(finalToolCalls),
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                    conversationDao.touch(conversationId, System.currentTimeMillis())
                }

                _streamingState.value = StreamingState()
                _isSending.value = false
                if (errorMsg != null) _chatError.value = errorMsg

                // 双写调用日志
                repository.addLog(
                    AiCallLogEntity(
                        timestamp = startTime,
                        type = "Assistant Chat",
                        status = if (errorMsg == null && (finalContentStr.isNotEmpty() || finalToolCalls.isNotEmpty())) "Success" else "Failed",
                        durationMs = duration,
                        model = cfg.modelName,
                        tools = finalToolCalls.joinToString(",") { it.name },
                        prompt = text,
                        response = finalContentStr,
                        errorMessage = errorMsg,
                        requestUrl = fullUrl,
                        reasoning = finalReasoningStr,
                        toolCallsJson = serializeToolCalls(finalToolCalls),
                    )
                )
            }
        }
    }

    fun regenerateLastAssistant() {
        if (_isSending.value) return
        viewModelScope.launch {
            val id = _currentConversationId.value ?: return@launch
            val msgs = messageDao.observeByConversation(id).first()
            val lastAssistant = msgs.lastOrNull { it.role == "assistant" } ?: return@launch
            val precedingUser = msgs.lastOrNull { it.role == "user" && it.order < lastAssistant.order } ?: return@launch

            messageDao.delete(lastAssistant.id)
            // 直接调 sendMessage，会再 insert 一条 user —— 这是 "重发最后一句" 的等价行为
            sendMessage(precedingUser.content)
        }
    }
```

- [ ] **步骤 4：在 ViewModel 类末尾追加私有辅助函数**

```kotlin
    private fun emitStreaming(
        reasoning: CharSequence,
        content: CharSequence,
        toolCalls: List<ToolCallRecord>,
    ) {
        _streamingState.value = StreamingState(
            reasoning = reasoning.toString(),
            content = content.toString(),
            toolCalls = toolCalls.toList(),
        )
    }

    private fun buildWireHistory(
        cfg: AiInterConfig,
        messages: List<ConversationMessageEntity>,
    ): List<JsonObject> {
        val nowIso = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val systemContent = buildString {
            append("当前时间: ")
            append(nowIso)
            append("\n\n")
            append(TOOLS_SYSTEM_PROMPT)
            if (cfg.promptChat.isNotBlank()) {
                append("\n\n# 附加指引\n")
                append(cfg.promptChat)
            }
        }
        val list = mutableListOf<JsonObject>()
        list += buildJsonObject {
            put("role", "system")
            put("content", systemContent)
        }
        messages.forEach { msg ->
            list += buildJsonObject {
                put("role", msg.role)
                put("content", msg.content)
            }
        }
        return list
    }

    private suspend fun executeToolCall(buf: ToolCallBuffer): ToolCallRecord {
        val name = buf.name.orEmpty()
        val argsStr = buf.arguments.toString()
        val id = buf.id ?: "call_${System.nanoTime()}"
        val started = System.currentTimeMillis()
        val argsMap = parseToolArguments(argsStr)
        val result = if (name.isBlank()) {
            ToolResult.Error(
                name = name,
                error = ToolError.ValidationError("模型未提供工具名"),
            )
        } else {
            toolRegistry.executeTool(name, argsMap)
        }
        val durationMs = System.currentTimeMillis() - started
        val (success, resultStr) = when (result) {
            is ToolResult.Success -> true to (result.data?.toString() ?: "null")
            is ToolResult.Error -> false to "[${result.error::class.simpleName}] ${result.error.message}"
        }
        return ToolCallRecord(
            id = id,
            name = name,
            arguments = argsStr,
            result = resultStr,
            success = success,
            durationMs = durationMs,
        )
    }

    private fun parseToolArguments(argsStr: String): Map<String, Any?> {
        if (argsStr.isBlank()) return emptyMap()
        return try {
            val parser = Json { ignoreUnknownKeys = true }
            val element = parser.parseToJsonElement(argsStr)
            if (element !is JsonObject) return emptyMap()
            element.mapValues { (_, v) ->
                when (v) {
                    is JsonPrimitive -> when {
                        v.isString -> v.content
                        v.content == "true" -> true
                        v.content == "false" -> false
                        else -> v.content.toLongOrNull() ?: v.content.toDoubleOrNull() ?: v.content
                    }
                    else -> v.toString()
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeToolCalls(records: List<ToolCallRecord>): String {
        if (records.isEmpty()) return ""
        val writer = Json { ignoreUnknownKeys = true }
        val array = buildJsonArray {
            records.forEach { rec ->
                add(buildJsonObject {
                    put("id", rec.id)
                    put("name", rec.name)
                    put("arguments", rec.arguments)
                    put("result", rec.result)
                    put("success", rec.success)
                    put("durationMs", rec.durationMs)
                })
            }
        }
        return writer.encodeToString(JsonArray.serializer(), array)
    }
```

- [ ] **步骤 5：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL；如有 `toOpenAiFunctionJson` import 错误，确认其是扩展函数还是 top-level；当前 `AiNetModels.kt` 中以 `fun ToolDefinition.toOpenAiFunctionJson()` 定义，import 形如 `import com.nltimer.app.experimental.ai_inter.network.toOpenAiFunctionJson`

- [ ] **步骤 6：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatViewModel.kt
git commit -m "feat(ai_inter): sendMessage 接入 SSE 流式与工具调用循环

复用 AiInterApiClient.streamChat 与 ToolRegistry，最多 5 轮工具循环。
新对话首条 user 消息自动用首句前 24 字符填标题。
完成/中断/错误三种结束态都正确 insert assistant 消息并双写 ai_call_log。
regenerateLastAssistant 删最后一条 assistant 后重发对应 user。"
```

---

## 阶段 4：highlight 模块移植 + Markdown 核心（3 commits）

### 任务 4.1：移植 highlight 模块

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/highlight/Highlighter.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/highlight/HighlightText.kt`
- 创建：`app/src/main/assets/highlight.min.js`

- [ ] **步骤 1：复制 highlight.js bundle 到 assets**

```bash
mkdir -p app/src/main/assets
cp docs/reference/rikkahub/highlight/src/main/assets/highlight.min.js \
   app/src/main/assets/highlight.min.js
```

- [ ] **步骤 2：复制并改包名 Highlighter.kt**

读取 `docs/reference/rikkahub/highlight/src/main/java/me/rerere/highlight/Highlighter.kt` 全文，写入 `app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/highlight/Highlighter.kt`。
- 首行 `package me.rerere.highlight` → `package com.nltimer.app.experimental.ai_inter.chat.highlight`
- 如有 `me.rerere` 开头的内部 import，保留并在同包补全文件（同步骤复制对应文件）

- [ ] **步骤 3：复制并改包名 HighlightText.kt**

读取 `docs/reference/rikkahub/highlight/src/main/java/me/rerere/highlight/HighlightText.kt` 全文，写入对应路径，同样改 package。

- [ ] **步骤 4：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL；如缺 import 报错（如 `LocalHighlighter`、`HighlightTokenList` 等中间类型），按报错路径继续到 rikkahub highlight 模块复制对应文件到本地 highlight 包

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/highlight/ \
        app/src/main/assets/highlight.min.js
git commit -m "feat(ai_inter): 移植 rikkahub highlight 模块（QuickJS + highlight.js）

将 me.rerere:highlight 模块作为本地包内嵌到 chat/highlight/，避免新增子模块。
提供 Highlighter（异步调用 highlight.js）+ HighlightText（结果 → AnnotatedString）。
assets/highlight.min.js 复用 rikkahub 同名 bundle。"
```

---

### 任务 4.2：移植 Markdown 渲染器（GFM 核心 + 代码高亮）

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/MarkdownBlock.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HtmlBlockRenderer.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HtmlInlineRenderer.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/CssStyleParser.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HighlightCodeBlock.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/MarkdownPreprocess.kt`

- [ ] **步骤 1：创建 MarkdownPreprocess.kt**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.markdown

private val INLINE_LATEX_REGEX = Regex("\\\\\\((.+?)\\\\\\)")
private val BLOCK_LATEX_REGEX = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)
private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```|`[^`\n]*`", RegexOption.DOT_MATCHES_ALL)

internal fun preProcess(content: String): String {
    val codeBlocks = mutableListOf<IntRange>()
    CODE_BLOCK_REGEX.findAll(content).forEach { codeBlocks.add(it.range) }
    fun isInCodeBlock(pos: Int) = codeBlocks.any { pos in it }

    var result = INLINE_LATEX_REGEX.replace(content) { m ->
        if (isInCodeBlock(m.range.first)) m.value else "$" + m.groupValues[1] + "$"
    }
    result = BLOCK_LATEX_REGEX.replace(result) { m ->
        if (isInCodeBlock(m.range.first)) m.value else "$$" + m.groupValues[1] + "$$"
    }
    return result
}
```

- [ ] **步骤 2：基于 rikkahub MarkdownNew.kt 拆分 4 个文件**

读取 `docs/reference/rikkahub/app/src/main/java/me/rerere/rikkahub/ui/components/richtext/MarkdownNew.kt`，按下列规则拆分：

**MarkdownBlock.kt**（入口）：保留 `MarkdownBlock`（rikkahub 中叫 `MarkdownNew`，重命名）+ `generateMarkdownHtml`：

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup

private val flavour by lazy { GFMFlavourDescriptor(makeHttpsAutoLinks = true, useSafeLinks = true) }
private val parser by lazy { MarkdownParser(flavour) }

private fun generateMarkdownHtml(content: String): String {
    val preprocessed = preProcess(content)
    val tree = parser.buildMarkdownTreeFromString(preprocessed)
    return HtmlGenerator(preprocessed, tree, flavour).generateHtml()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onClickCitation: (String) -> Unit = {},
) {
    var html by remember { mutableStateOf(generateMarkdownHtml(content)) }
    val updated by rememberUpdatedState(content)
    LaunchedEffect(Unit) {
        snapshotFlow { updated }
            .distinctUntilChanged()
            .mapLatest { generateMarkdownHtml(it) }
            .catch { it.printStackTrace() }
            .flowOn(Dispatchers.Default)
            .collect { html = it }
    }

    val document = remember(html) {
        runCatching { Jsoup.parse(html) }.getOrElse { Jsoup.parse("") }
    }

    ProvideTextStyle(style) {
        Column(modifier = modifier.padding(start = 4.dp)) {
            document.body().childNodes().fastForEach { node ->
                HtmlBodyNode(node = node, onClickCitation = onClickCitation)
            }
        }
    }
}
```

**HtmlBlockRenderer.kt** + **HtmlInlineRenderer.kt** + **CssStyleParser.kt**：从 rikkahub `MarkdownNew.kt` 复制对应函数到三个文件，包名改为 `com.nltimer.app.experimental.ai_inter.chat.markdown`，并执行替换：
- 删除所有 `LocalSettings.current.displaySetting.enableLatexRendering` 判断分支：保留非 LaTeX 路径
- 删除 `<table>` / `HtmlTable` 分支（任务 5.1 再加）
- 删除 `<span class="math">` 块/行内 分支
- 删除 citation 内联占位（`text.startsWith("citation,")` 整段移除）
- `JetbrainsMono` → `FontFamily.Monospace`
- `me.rerere.hugeicons.HugeIcons.Tick01` → `androidx.compose.material.icons.Icons.Default.Check`
- `me.rerere.rikkahub.utils.toDp` 在文件内补一个 Composable extension：

```kotlin
@Composable
private fun androidx.compose.ui.unit.TextUnit.toDp(): androidx.compose.ui.unit.Dp =
    with(androidx.compose.ui.platform.LocalDensity.current) { this@toDp.toDp() }
```

具体函数分布：
- `HtmlBlockRenderer.kt`：`HtmlBodyNode` / `HtmlBlockElement` / `HtmlParagraph` / `HtmlParagraphContent` / `HtmlHeading` / `HtmlList` / `HtmlListItem` / `HtmlBlockquote` / `HtmlDetails` / `HtmlProgress` / `HtmlStyledElement` / `HeaderStyle` 常量
- `HtmlInlineRenderer.kt`：`HtmlInlineGroup` / `HtmlInlineAsComposable` / `appendHtmlInlineNode` / `appendHtmlInlineElement` / `buildFontTagStyle`
- `CssStyleParser.kt`：`parseInlineSpanStyle` / `parseBlockTextStyle` / `parseCssDeclarations` / `parseFontSize` / `parseSpacing` / `parseLineHeight` / `parseLegacyFontSize` / `parseFontFamily` / `parseColor` / `parseFontWeight` / `parseFontStyle` / `parseTextDecoration` / `parseTextAlign`

`<img>` 分支保留并调用本地 `ZoomableAsyncImage`（任务 4.3 创建）；`<pre>` 分支调用 `HighlightCodeBlock`（下一步创建）。

- [ ] **步骤 3：创建 HighlightCodeBlock.kt**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.highlight.Highlighter

@Composable
fun HighlightCodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    completeCodeBlock: Boolean = true,
) {
    val highlighter = remember { Highlighter() }
    val highlighted by produceState(initialValue = AnnotatedString(code), code, language) {
        value = try {
            highlighter.highlight(code, language)
        } catch (_: Throwable) {
            AnnotatedString(code)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}
```

注：`Highlighter.highlight(code, language): AnnotatedString` 是任务 4.1 移植版本的签名；若签名为 suspend，把 produceState body 改为 `value = highlighter.highlight(...)`。如签名差异较大，按移植版调整。

- [ ] **步骤 4：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL；如有 import 报错或 `ZoomableAsyncImage`/`DataTable` 未定义，先注释掉 `<img>` 和 `<table>` 分支（保留 else 默认渲染），等任务 4.3/5.1 再启用

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/
git commit -m "feat(ai_inter): 移植 rikkahub MarkdownNew 渲染器（GFM 子集）

包含 5 个文件分工：
- MarkdownBlock：composable 入口 + Markdown→HTML 转换
- HtmlBlockRenderer：块级元素渲染（p/h*/ul/ol/blockquote/pre/details/...)
- HtmlInlineRenderer：行内 AnnotatedString 构建（b/em/code/a/...)
- CssStyleParser：CSS style 属性 → Compose TextStyle
- MarkdownPreprocess：代码块识别、LaTeX 占位符预处理（保留扩展位）
- HighlightCodeBlock：接 highlight 模块 + 降级文本

剔除项：LaTeX/Mermaid/表格/citation/Favicon/JetbrainsMono/LocalSettings 依赖。
HugeIcons.Tick01 替换为 Material Icons.Default.Check。"
```

---

### 任务 4.3：ZoomableAsyncImage（图片）

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/ZoomableAsyncImage.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HtmlBlockRenderer.kt`（启用 <img> 分支）

- [ ] **步骤 1：实现 ZoomableAsyncImage**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.markdown

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun ZoomableAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offset = if (scale > 1f) offset + pan else Offset.Zero
                }
            },
    )
}
```

- [ ] **步骤 2：在 HtmlBlockRenderer 启用 <img> 分支**

确认 `HtmlBlockRenderer.kt` 的 `HtmlBlockElement` when 分支中：

```kotlin
        "img" -> {
            val src = element.attr("src")
            val alt = element.attr("alt")
            if (src.isNotEmpty()) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    ZoomableAsyncImage(
                        model = src,
                        contentDescription = alt.takeIf { it.isNotEmpty() },
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .widthIn(min = 120.dp)
                            .heightIn(min = 120.dp),
                    )
                }
            }
        }
```

- [ ] **步骤 3：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL

- [ ] **步骤 4：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/ZoomableAsyncImage.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HtmlBlockRenderer.kt
git commit -m "feat(ai_inter): 添加 ZoomableAsyncImage，支持 Markdown 内嵌图片

基于 Coil3 + detectTransformGestures 的轻量缩放/平移；不含全屏预览（MVP 范围外）。
HtmlBlockRenderer 的 <img> 分支接入此组件。"
```

---

## 阶段 5：Markdown 表格（1 commit）

### 任务 5.1：移植 DataTable + 启用 <table> 分支

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/DataTable.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HtmlBlockRenderer.kt`

- [ ] **步骤 1：移植 DataTable**

读取 `docs/reference/rikkahub/app/src/main/java/me/rerere/rikkahub/ui/components/table/DataTable.kt` 全文，写入 `app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/DataTable.kt`：
- package 改为 `com.nltimer.app.experimental.ai_inter.chat.markdown`
- 删除 rikkahub-only import（`LocalSettings`/`extendColors`/`shimmer` 等），改用 MaterialTheme 默认值
- 保持公共 API：

```kotlin
@Composable
fun DataTable(
    headers: List<@Composable () -> Unit>,
    rows: List<List<@Composable () -> Unit>>,
    modifier: Modifier = Modifier,
    columnMinWidths: List<androidx.compose.ui.unit.Dp> = emptyList(),
    columnMaxWidths: List<androidx.compose.ui.unit.Dp> = emptyList(),
)
```

- [ ] **步骤 2：在 HtmlBlockRenderer 启用 <table> 分支**

修改 `HtmlBlockRenderer.kt` 的 `HtmlBlockElement` when：

```kotlin
        "table" -> HtmlStyledElement(element = element) {
            HtmlTable(element = element, onClickCitation = onClickCitation)
        }
```

在文件末尾追加 `HtmlTable` 实现：

```kotlin
@Composable
private fun HtmlTable(element: org.jsoup.nodes.Element, onClickCitation: (String) -> Unit) {
    val headerElements = element.select("thead tr th")
    val columnCount = headerElements.size.takeIf { it > 0 }
        ?: element.select("tbody tr:first-child td").size
    if (columnCount == 0) return

    val headers = List(columnCount) { col ->
        @Composable {
            if (col < headerElements.size) {
                HtmlStyledElement(headerElements[col]) {
                    HtmlInlineGroup(headerElements[col].childNodes(), onClickCitation)
                }
            }
        }
    }

    val bodyRows = element.select("tbody tr")
    val rows = bodyRows.map { tr ->
        val cells = tr.select("td")
        List(columnCount) { col ->
            @Composable {
                if (col < cells.size) {
                    HtmlStyledElement(cells[col]) {
                        HtmlInlineGroup(cells[col].childNodes(), onClickCitation)
                    }
                }
            }
        }
    }

    DataTable(
        headers = headers,
        rows = rows,
        modifier = androidx.compose.ui.Modifier.padding(vertical = 8.dp),
        columnMinWidths = List(columnCount) { 80.dp },
        columnMaxWidths = List(columnCount) { 200.dp },
    )
}
```

注：`HtmlInlineGroup` 接受 `List<org.jsoup.nodes.Node>`，与 `element.childNodes()` 返回类型一致；如果声明的是 `List<Node>` 而 `childNodes()` 返回 `MutableList`，加 `.toList()`。

- [ ] **步骤 3：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL

- [ ] **步骤 4：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/DataTable.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/markdown/HtmlBlockRenderer.kt
git commit -m "feat(ai_inter): 支持 Markdown 表格渲染

移植 rikkahub DataTable + 在 HtmlBlockRenderer 的 <table> 分支接入。
配置 columnMinWidths=80dp / columnMaxWidths=200dp 适配长内容横向滚动。"
```

---

## 阶段 6：UI 组件全套（4 commits）

### 任务 6.1：ChatMessage / ReasoningBlock / ToolCallsBlock

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ReasoningBlock.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ToolCallsBlock.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatMessage.kt`

- [ ] **步骤 1：ReasoningBlock**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.markdown.MarkdownBlock

@Composable
fun ReasoningBlock(reasoning: String, defaultExpanded: Boolean = false) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Psychology, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("深度思考", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
            }
            if (expanded) {
                MarkdownBlock(
                    content = reasoning,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
```

- [ ] **步骤 2：ToolCallsBlock**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord

@Composable
fun ToolCallsBlock(toolCalls: List<ToolCallRecord>, defaultExpanded: Boolean = false) {
    var expanded by remember(toolCalls.size) { mutableStateOf(defaultExpanded) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Build, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("调用工具 (${toolCalls.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
            }
            if (expanded) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    toolCalls.forEach { ToolCallCard(it) }
                }
            }
        }
    }
}

@Composable
private fun ToolCallCard(call: ToolCallRecord) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (call.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    null, Modifier.size(14.dp),
                    if (call.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(4.dp))
                Text(call.name.ifEmpty { "(未知工具)" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("${call.durationMs}ms", style = MaterialTheme.typography.labelSmall)
            }
            if (call.arguments.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("参数：${call.arguments}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text("结果：${call.result}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }
    }
}
```

- [ ] **步骤 3：ChatMessage**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import com.nltimer.app.experimental.ai_inter.chat.markdown.MarkdownBlock
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ChatMessage(
    msg: ConversationMessageEntity,
    showActions: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = msg.role == "user"
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isUser) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 340.dp),
            ) {
                SelectionContainer {
                    Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        MarkdownBlock(content = msg.content)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (msg.reasoning.isNotBlank()) {
                    ReasoningBlock(reasoning = msg.reasoning, defaultExpanded = false)
                }
                val tools = parseToolCalls(msg.toolCallsJson)
                if (tools.isNotEmpty()) {
                    ToolCallsBlock(toolCalls = tools, defaultExpanded = false)
                }
                SelectionContainer {
                    MarkdownBlock(content = msg.content, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        AnimatedVisibility(visible = showActions, enter = fadeIn(), exit = fadeOut()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "复制", Modifier.size(18.dp)) }
                if (!isUser) {
                    IconButton(onClick = onRegenerate) { Icon(Icons.Default.Refresh, "重生成", Modifier.size(18.dp)) }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", Modifier.size(18.dp)) }
            }
        }
    }
}

private fun parseToolCalls(json: String): List<ToolCallRecord> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val arr: JsonArray = Json { ignoreUnknownKeys = true }.parseToJsonElement(json).jsonArray
        arr.map { el ->
            val o = el.jsonObject
            ToolCallRecord(
                id = o["id"]?.jsonPrimitive?.content.orEmpty(),
                name = o["name"]?.jsonPrimitive?.content.orEmpty(),
                arguments = o["arguments"]?.jsonPrimitive?.content.orEmpty(),
                result = o["result"]?.jsonPrimitive?.content.orEmpty(),
                success = o["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                durationMs = o["durationMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            )
        }
    }.getOrDefault(emptyList())
}
```

- [ ] **步骤 4：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatMessage.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ReasoningBlock.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ToolCallsBlock.kt
git commit -m "feat(ai_inter): 添加 ChatMessage/ReasoningBlock/ToolCallsBlock 三件套

用户消息：右对齐 primaryContainer 圆角气泡，最大宽 340dp
助手消息：左对齐无气泡全宽，含可折叠 reasoning + 工具调用卡片堆叠
ActionRow：fadeIn/Out，复制/重生成/删除三按钮，user 不显示重生成"
```

---

### 任务 6.2：ChatList + 错误浮卡 + StreamingBubble

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatList.kt`

- [ ] **步骤 1：实现 ChatList**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.StreamingState
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import com.nltimer.app.experimental.ai_inter.chat.markdown.MarkdownBlock

@Composable
fun ChatList(
    innerPadding: PaddingValues,
    messages: List<ConversationMessageEntity>,
    streaming: StreamingState,
    isSending: Boolean,
    chatError: String?,
    listState: LazyListState,
    onCopy: (ConversationMessageEntity) -> Unit,
    onRegenerate: () -> Unit,
    onDelete: (ConversationMessageEntity) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(messages.size, isSending) {
        val targetIdx = if (isSending) messages.size else (messages.size - 1).coerceAtLeast(0)
        if (messages.isNotEmpty() || isSending) {
            listState.animateScrollToItem(targetIdx)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(messages, key = { _, m -> m.id }) { idx, msg ->
                ChatMessage(
                    msg = msg,
                    showActions = !(isSending && idx == messages.lastIndex),
                    onCopy = { onCopy(msg) },
                    onRegenerate = onRegenerate,
                    onDelete = { onDelete(msg) },
                )
            }
            if (isSending) {
                item("streaming") { StreamingBubble(streaming) }
            }
        }

        AnimatedVisibility(
            visible = chatError != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(innerPadding),
        ) {
            chatError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = err,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = onDismissError) { Text("关闭") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(streaming: StreamingState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (streaming.reasoning.isNotBlank()) {
            ReasoningBlock(reasoning = streaming.reasoning, defaultExpanded = true)
        }
        if (streaming.toolCalls.isNotEmpty()) {
            ToolCallsBlock(toolCalls = streaming.toolCalls, defaultExpanded = false)
        }
        if (streaming.content.isNotBlank()) {
            MarkdownBlock(content = streaming.content, modifier = Modifier.fillMaxWidth())
        }
        if (streaming.isEmpty) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("正在生成…", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatList.kt
git commit -m "feat(ai_inter): 添加 ChatList 消息列表

LazyColumn + 16dp 边距 + 12dp item 间距；新消息/流式自动滚动到底。
流式过程显示 StreamingBubble（reasoning/tools/content 三件套渐进式渲染）。
chatError 通过底部 errorContainer 浮卡显示，TextButton 可手动关闭。"
```

---

### 任务 6.3：ChatInput + ChatTopBar + ChatDrawer + ExportSheet

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatInput.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatTopBar.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatDrawer.kt`
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ExportSheet.kt`

- [ ] **步骤 1：ChatInput**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("输入消息…") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                maxLines = 5,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Default),
                enabled = !isSending,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onPickModel) {
                    Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("模型", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))

                val containerColor = when {
                    isSending -> MaterialTheme.colorScheme.errorContainer
                    text.isBlank() -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.primary
                }
                val onContainerColor = when {
                    isSending -> MaterialTheme.colorScheme.onErrorContainer
                    text.isBlank() -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onPrimary
                }
                Surface(shape = CircleShape, color = containerColor, modifier = Modifier.size(36.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = if (isSending) onStop else onSend,
                            enabled = isSending || text.isNotBlank(),
                        ) {
                            Icon(
                                imageVector = if (isSending) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (isSending) "停止" else "发送",
                                tint = onContainerColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **步骤 2：ChatTopBar**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    modelName: String,
    onOpenDrawer: () -> Unit,
    onTitleClick: () -> Unit,
    onNewConversation: () -> Unit,
    onClearCurrent: () -> Unit,
    onExport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "会话列表") }
        },
        title = {
            Surface(onClick = onTitleClick, color = Color.Transparent) {
                Column {
                    Text(
                        text = title.ifBlank { "新对话" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (modelName.isNotBlank()) {
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = LocalContentColor.current.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onExport) { Icon(Icons.Default.IosShare, "导出") }
            IconButton(onClick = onNewConversation) { Icon(Icons.Default.Add, "新建对话") }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "更多") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("清空当前对话") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; onClearCurrent() },
                    )
                }
            }
        },
    )
}
```

- [ ] **步骤 3：ChatDrawer**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    conversations: List<ConversationEntity>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var actionTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("对话", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCreate) { Icon(Icons.Default.Add, "新建对话") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                items(conversations, key = { it.id }) { conv ->
                    val selected = conv.id == currentId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSelect(conv.id) },
                                onLongClick = { actionTarget = conv },
                            ),
                    ) {
                        Text(
                            text = conv.title.ifBlank { "新对话" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    actionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(target.title.ifBlank { "新对话" }) },
            text = { Text("选择操作") },
            confirmButton = {
                TextButton(onClick = { renameTarget = target; actionTarget = null }) { Text("重命名") }
            },
            dismissButton = {
                TextButton(onClick = { onDelete(target.id); actionTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    renameTarget?.let { target ->
        var input by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名对话") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target.id, input)
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
        )
    }
}
```

- [ ] **步骤 4：ExportSheet**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nltimer.app.experimental.ai_inter.chat.export.ExportFormat
import com.nltimer.app.experimental.ai_inter.chat.export.ExportOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    onDismiss: () -> Unit,
    onExport: (ExportFormat, ExportOptions) -> String,
) {
    var format by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    var includeTools by remember { mutableStateOf(true) }
    var includeReasoning by remember { mutableStateOf(true) }
    val preview = remember(format, includeTools, includeReasoning) {
        onExport(format, ExportOptions(includeTools, includeReasoning))
    }
    val context: Context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("导出当前对话", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("格式：")
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = format == ExportFormat.MARKDOWN,
                    onClick = { format = ExportFormat.MARKDOWN },
                    label = { Text("Markdown") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = format == ExportFormat.JSON,
                    onClick = { format = ExportFormat.JSON },
                    label = { Text("JSON") },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeTools, onCheckedChange = { includeTools = it })
                Text("包含工具调用详细信息")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeReasoning, onCheckedChange = { includeReasoning = it })
                Text("包含 reasoning 思考过程")
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
            ) {
                Text(
                    text = preview.take(2000) + if (preview.length > 2000) "\n…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("AI 对话", preview))
                    onDismiss()
                }) { Text("复制") }
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (format == ExportFormat.JSON) "application/json" else "text/plain"
                        putExtra(Intent.EXTRA_TEXT, preview)
                    }
                    context.startActivity(Intent.createChooser(intent, "分享对话"))
                    onDismiss()
                }) { Text("分享") }
            }
        }
    }
}
```

- [ ] **步骤 5：编译验证**

运行：`./gradlew :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL

- [ ] **步骤 6：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatInput.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatTopBar.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatDrawer.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ExportSheet.kt
git commit -m "feat(ai_inter): 添加 TopBar/Input/Drawer/ExportSheet 四件套

ChatInput：圆角大号 surfaceContainerLow（无毛玻璃，留 hazeEffect 接入位）+ 模型胶囊 + 发送/停止状态机
ChatTopBar：透明背景 + 标题(点击重命名)/模型副标题 + 导出/新建/更多三按钮
ChatDrawer：会话列表 + 长按弹 ActionDialog（重命名/删除）
ExportSheet：FilterChip 切换 Markdown/JSON + 两 Checkbox + 实时预览前 2000 字 + 复制/分享"
```

---

### 任务 6.4：AiAssistantChatRoute 主入口 + 路由接通 + AiInter 入口卡片

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatRoute.kt`
- 修改：`app/src/main/java/com/nltimer/app/navigation/NLtimerRoutes.kt`
- 修改：`app/src/main/java/com/nltimer/app/navigation/NLtimerNavHost.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterScreen.kt`

- [ ] **步骤 1：在 NLtimerRoutes 加常量**

修改 `NLtimerRoutes.kt`，在 `AI_TEST_CHAT` 之后追加：

```kotlin
    const val AI_ASSISTANT_CHAT = "ai_assistant_chat"
```

并在 `SETTINGS_FULLSCREEN_ROUTES` set 中追加 `AI_ASSISTANT_CHAT`：

```kotlin
    val SETTINGS_FULLSCREEN_ROUTES = setOf(
        THEME_SETTINGS, DIALOG_CONFIG, BEHAVIOR_MANAGEMENT, DATA_MANAGEMENT,
        HOME_LAYOUT_CONFIG, COLOR_PALETTE, CATEGORIES, AI_INTER,
        AI_PROVIDER_CONFIG, AI_TOOLS_LIST, AI_CALL_LOGS, AI_PROMPT_CONFIG,
        AI_TEST_CHAT, AI_ASSISTANT_CHAT, AI_CALL_LOG_DETAIL_PATTERN
    )
```

- [ ] **步骤 2：实现 AiAssistantChatRoute**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.nltimer.app.experimental.ai_inter.chat.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatRoute(
    navController: NavHostController,
    viewModel: AiAssistantChatViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val conversations by viewModel.conversations.collectAsState()
    val currentId by viewModel.currentConversationId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val streaming by viewModel.streamingState.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val chatError by viewModel.chatError.collectAsState()
    val config by viewModel.config.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    val current = conversations.firstOrNull { it.id == currentId }
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showExport by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                conversations = conversations,
                currentId = currentId,
                onSelect = {
                    viewModel.selectConversation(it)
                    scope.launch { drawerState.close() }
                },
                onCreate = {
                    viewModel.newConversation()
                    scope.launch { drawerState.close() }
                },
                onRename = viewModel::renameConversation,
                onDelete = viewModel::deleteConversation,
            )
        },
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                        ),
                    ),
                ),
            )
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    ChatTopBar(
                        title = current?.title ?: "新对话",
                        modelName = config.modelName,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onTitleClick = { if (current != null) showRename = true },
                        onNewConversation = { viewModel.newConversation() },
                        onClearCurrent = { viewModel.clearCurrent() },
                        onExport = { showExport = true },
                    )
                },
                bottomBar = {
                    ChatInput(
                        text = inputText,
                        isSending = isSending,
                        onTextChange = { inputText = it },
                        onSend = {
                            val msg = inputText.trim()
                            if (msg.isNotBlank()) {
                                viewModel.sendMessage(msg)
                                inputText = ""
                            }
                        },
                        onStop = { viewModel.stopStreaming() },
                        onPickModel = {
                            viewModel.refreshModels()
                            showModelSheet = true
                        },
                    )
                },
            ) { innerPadding ->
                ChatList(
                    innerPadding = innerPadding,
                    messages = messages,
                    streaming = streaming,
                    isSending = isSending,
                    chatError = chatError,
                    listState = listState,
                    onCopy = { msg ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("AI 消息", msg.content))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = { viewModel.regenerateLastAssistant() },
                    onDelete = { msg -> viewModel.deleteMessage(msg.id) },
                    onDismissError = { viewModel.clearChatError() },
                )
            }
        }
    }

    if (showRename && current != null) {
        var input by remember(current.id) { mutableStateOf(current.title) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("重命名对话") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameConversation(current.id, input)
                    showRename = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("取消") }
            },
        )
    }

    if (showModelSheet) {
        ModalBottomSheet(onDismissRequest = { showModelSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("选择模型", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("当前：${config.modelName}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                availableModels.forEach { name ->
                    TextButton(
                        onClick = {
                            viewModel.selectModel(name)
                            showModelSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(name) }
                }
                if (availableModels.isEmpty()) {
                    Text(
                        "未拉取到模型列表，请先在 AI Inter → 提供商配置 中获取",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showExport && current != null) {
        ExportSheet(
            onDismiss = { showExport = false },
            onExport = { fmt, opts -> viewModel.exportConversation(current.id, fmt, opts) },
        )
    }
}
```

- [ ] **步骤 3：在 NLtimerNavHost 注册路由**

修改 `NLtimerNavHost.kt`：

顶部追加 import：

```kotlin
import com.nltimer.app.experimental.ai_inter.chat.AiAssistantChatRoute
```

在 `composable(NLtimerRoutes.AI_TEST_CHAT) { AiTestChatRoute() }` 行之后追加：

```kotlin
        composable(
            NLtimerRoutes.AI_ASSISTANT_CHAT,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            AiAssistantChatRoute(navController = navController)
        }
```

- [ ] **步骤 4：在 AiInterScreen 添加入口卡片**

修改 `AiInterScreen.kt`：

修改 `AiInterRoute`：

```kotlin
@Composable
fun AiInterRoute(navController: NavHostController) {
    AiInterScreen(
        onNavigateToProviderConfig = { navController.navigate(NLtimerRoutes.AI_PROVIDER_CONFIG) },
        onNavigateToToolsList = { navController.navigate(NLtimerRoutes.AI_TOOLS_LIST) },
        onNavigateToCallLogs = { navController.navigate(NLtimerRoutes.AI_CALL_LOGS) },
        onNavigateToPromptConfig = { navController.navigate(NLtimerRoutes.AI_PROMPT_CONFIG) },
        onNavigateToTestChat = { navController.navigate(NLtimerRoutes.AI_TEST_CHAT) },
        onNavigateToAssistantChat = { navController.navigate(NLtimerRoutes.AI_ASSISTANT_CHAT) },
    )
}
```

修改 `AiInterScreen`，签名追加 `onNavigateToAssistantChat: () -> Unit,`；在 LazyColumn 内第一个 item（"提供商配置"卡片）之前追加：

```kotlin
            item {
                SettingsEntryCard(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "AI 助手对话",
                    subtitle = "正式对话界面：多会话 / Markdown / 导出",
                    onClick = onNavigateToAssistantChat,
                )
            }
```

把现有"测试对话"卡片的 `Icons.AutoMirrored.Filled.Chat` 改为 `Icons.Default.Science`（避免与新入口图标重复，import：`import androidx.compose.material.icons.filled.Science`）；同时修改其 subtitle 为"开发调试视图：当轮流式 + 工具调用日志"。

- [ ] **步骤 5：编译并人工烟测**

运行：`./gradlew :app:installDebug`

人工验证：
1. 进入 AI Inter → 顶部出现"AI 助手对话"卡片
2. 点击 → 进入新页面，TopBar 显示"新对话 / <模型名>"
3. 抽屉打开 → 列出"新对话"

- [ ] **步骤 6：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatRoute.kt \
        app/src/main/java/com/nltimer/app/navigation/NLtimerRoutes.kt \
        app/src/main/java/com/nltimer/app/navigation/NLtimerNavHost.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/AiInterScreen.kt
git commit -m "feat(ai_inter): 接通 AI 助手对话路由

- AiAssistantChatRoute：ModalNavigationDrawer + Scaffold + 渐变背景 +
  TopBar/Input/List/Drawer/Export 全部接入；模型快切 + 重命名 + 错误浮卡
- NLtimerRoutes 新增 AI_ASSISTANT_CHAT，加入 SETTINGS_FULLSCREEN_ROUTES
- NavHost 注册路由，加水平滑动转场动画
- AiInterScreen 顶部新增「AI 助手对话」入口卡片，旧测试对话改为科学实验图标 + 「开发调试视图」副标题"
```

---

## 阶段 7：毛玻璃 + 跳转浮动按钮（2 commits）

### 任务 7.1：Haze 毛玻璃

**文件：**
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatRoute.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatInput.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatList.kt`

- [ ] **步骤 1：ChatInput 加 hazeState 参数 + hazeEffect**

修改 `ChatInput.kt`，加 import：

```kotlin
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
```

函数签名追加 `hazeState: HazeState`，Surface 的 modifier 改为：

```kotlin
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .hazeEffect(
                state = hazeState,
                style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow),
            ),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.extraLarge,
    ) { ... }
```

- [ ] **步骤 2：ChatList 加 hazeState 参数 + hazeSource**

修改 `ChatList.kt`，加 import：

```kotlin
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
```

函数签名追加 `hazeState: HazeState`，LazyColumn modifier 改为：

```kotlin
        LazyColumn(
            state = listState,
            contentPadding = ...,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().hazeSource(state = hazeState),
        ) { ... }
```

- [ ] **步骤 3：AiAssistantChatRoute 创建 HazeState 并下传**

修改 `AiAssistantChatRoute.kt`，加 import：

```kotlin
import dev.chrisbanes.haze.rememberHazeState
```

在 `listState` 后新增：

```kotlin
    val hazeState = rememberHazeState()
```

ChatList 调用追加 `hazeState = hazeState`；ChatInput 调用追加 `hazeState = hazeState`。

- [ ] **步骤 4：编译并人工烟测**

运行：`./gradlew :app:installDebug`
验证：发消息后滚动，ChatInput 背后能看到背景模糊

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/AiAssistantChatRoute.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatInput.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatList.kt
git commit -m "feat(ai_inter): ChatInput 接入 Haze 毛玻璃效果

ChatList 作为 hazeSource，ChatInput 用 hazeEffect + HazeMaterials.ultraThin。
保持透明 Surface 让模糊穿透显示底部消息。"
```

---

### 任务 7.2：MessageJumper + 端到端烟测

**文件：**
- 创建：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/MessageJumper.kt`
- 修改：`app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatList.kt`

- [ ] **步骤 1：实现 MessageJumper**

```kotlin
package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MessageJumper(
    show: Boolean,
    state: LazyListState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = show,
        modifier = modifier,
        enter = slideInHorizontally { it * 2 },
        exit = slideOutHorizontally { it * 2 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            JumpButton(Icons.Default.KeyboardDoubleArrowUp, "跳到顶部") {
                scope.launch { state.scrollToItem(0) }
            }
            JumpButton(Icons.Default.KeyboardArrowUp, "上翻一项") {
                scope.launch {
                    state.animateScrollToItem((state.firstVisibleItemIndex - 1).coerceAtLeast(0))
                }
            }
            JumpButton(Icons.Default.KeyboardArrowDown, "下翻一项") {
                scope.launch { state.animateScrollToItem(state.firstVisibleItemIndex + 1) }
            }
            JumpButton(Icons.Default.KeyboardDoubleArrowDown, "跳到底部") {
                scope.launch { state.scrollToItem((state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) }
            }
        }
    }
}

@Composable
private fun JumpButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        tonalElevation = 4.dp,
    ) {
        Icon(icon, desc, modifier = Modifier.padding(8.dp).size(20.dp))
    }
}
```

- [ ] **步骤 2：在 ChatList 引入 MessageJumper**

修改 `ChatList.kt`，新增 import：

```kotlin
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
```

在 ChatList 函数内追加：

```kotlin
    val scope = rememberCoroutineScope()
    var recentScroll by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            recentScroll = true
        } else {
            delay(1500)
            recentScroll = false
        }
    }
```

并在 Box 内、错误浮卡 AnimatedVisibility 之后追加：

```kotlin
        MessageJumper(
            show = recentScroll && !listState.isScrollInProgress && !isSending,
            state = listState,
            scope = scope,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
        )
```

- [ ] **步骤 3：编译并端到端人工烟测**

运行：`./gradlew :app:installDebug`

依验收清单逐项检查：
1. 进入 AI Inter → 提供商配置：填 API 地址/Key/模型，获取模型列表
2. 返回 AI Inter → 点击 "AI 助手对话"
3. TopBar 显示"新对话 / <模型名>"
4. 输入栏点击发送 → 出现流式消息，reasoning/toolCalls 折叠可展开
5. ChatList 滚动 → 右侧出现 MessageJumper，4 个按钮工作
6. 抽屉打开 → 切换会话 / 新建 / 长按重命名/删除
7. TopBar Export → ExportSheet 显示预览 → 复制/分享
8. ChatInput 背后能看到模糊
9. 重启 app → 历史会话保留
10. AI_TEST_CHAT 调试视图仍可正常进入

- [ ] **步骤 4：Commit**

```bash
git add app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/MessageJumper.kt \
        app/src/main/java/com/nltimer/app/experimental/ai_inter/chat/components/ChatList.kt
git commit -m "feat(ai_inter): 添加 MessageJumper 浮动跳转按钮

右侧悬浮 4 按钮（跳顶/上翻/下翻/跳底），仅在最近 1.5s 内有滚动时显示。
完成端到端烟测：会话切换/流式/工具调用/导出/毛玻璃/抽屉/跳转全部通畅。"
```

---

## 验收清单

- [ ] AI Inter 入口页顶部出现"AI 助手对话"卡片
- [ ] 路由 AI_ASSISTANT_CHAT 加水平滑动转场，沉浸式无底部导航
- [ ] 首次进入自动创建空"新对话"
- [ ] 抽屉显示会话列表，按 updatedAt 倒序
- [ ] 抽屉长按弹 ActionDialog：重命名 / 删除
- [ ] TopBar 标题点击改名
- [ ] ChatInput 毛玻璃，圆角，模型快切胶囊，发送按钮状态机
- [ ] 输入 → 发送：流式渲染，reasoning 折叠卡片，工具调用卡片
- [ ] 工具调用多轮循环，最多 5 轮
- [ ] 中断按钮：取消后保留部分内容+"(已中断)"
- [ ] 重生成按钮：删最后一条 assistant，重发倒数第二条 user
- [ ] 删除按钮：单条消息删除
- [ ] 复制按钮：剪贴板，toast 提示
- [ ] Markdown 渲染：GFM 段落/列表/标题/链接/粗斜体/代码块（高亮）/表格/图片
- [ ] 滚动出现右侧 MessageJumper 浮动按钮，4 个按钮工作
- [ ] 错误浮卡 底部出现，可手动关闭
- [ ] ExportSheet：Markdown / JSON 切换、勾选项、预览前 2000 字、复制/分享
- [ ] DB migration v3→v4 升级不丢 ai_call_log
- [ ] 重启 app 历史会话与消息保留
- [ ] AI_TEST_CHAT 调试视图仍可正常进入（保留共存）

---

## 自检结果

**规格覆盖度：** spec 15 章节全部映射到任务：
- §1 背景目标 → 计划开头目标/架构
- §2 架构（路由/文件树/依赖图）→ 任务 1.1 / 6.4 / 各阶段文件清单
- §3 数据模型（Entity/Migration/DAO）→ 任务 1.2 / 1.3
- §4 UI 设计（8 子节）→ 阶段 6 任务 6.1-6.4
- §5 Markdown 管线 → 任务 4.1-4.3 / 5.1
- §6 ViewModel → 任务 3.1 / 3.2
- §7 导出 → 任务 2.1
- §8 依赖引入 → 任务 1.1 / 1.3
- §9 关键交互流程 → 任务 6.4 + 验收清单
- §10 错误处理 → ViewModel finally / ChatList 错误浮卡 / Highlight 降级
- §11 测试策略 → 任务 1.2/1.3/2.1 单元测试 + 验收清单烟测
- §12 性能 → MarkdownBlock 用 Dispatchers.Default + produceState 缓存
- §13 安全 → Exporter 不输出 API Key
- §14 升级回滚 → Migration_3_4 + 保留 fallbackToDestructiveMigration
- §15 扩展位 → 不在本计划

**占位符扫描：** 无 TODO/待定。任务 3.2 `regenerateLastAssistant` 复用 `sendMessage` 是有意行为（spec §9.4 明确定义"重发最后一句"）。

**类型一致性：**
- `ConversationDao` 与 `ConversationMessageDao` 的方法签名在任务 1.2 定义、3.1-3.2 调用一致（observeAll/upsert/rename/touch/delete/get / observeByConversation/nextOrder/insert/delete/deleteFromOrder/deleteAllInConversation）
- `StreamingState(reasoning, content, toolCalls)` 在任务 3.1 定义、ChatList StreamingBubble、ChatMessage 使用一致
- `ExportFormat` / `ExportOptions(includeTools, includeReasoning)` 在任务 2.1 定义、3.1 `exportConversation` 调用、6.3 ExportSheet `onExport` 间接调用一致
- `ToolCallRecord(id, name, arguments, result, success, durationMs)` 复用 `com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord`（现有），不重复定义
- `MarkdownBlock(content, modifier, style, onClickCitation)` 在任务 4.2 定义、ReasoningBlock / ChatMessage / ChatList 调用一致

无命名漂移。

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-05-18-ai-assistant-chat-plan.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

选哪种方式？
