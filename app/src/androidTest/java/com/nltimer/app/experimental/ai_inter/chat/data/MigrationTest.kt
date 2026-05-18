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
        helper.createDatabase(testDbName, 3).apply {
            execSQL("""
                INSERT INTO ai_call_logs (timestamp, type, status, durationMs, model, tools, prompt, response, errorMessage, requestUrl, requestTokens, responseTokens, reasoning, toolCallsJson)
                VALUES (1000, 'Test', 'Success', 100, 'gpt', '', 'hi', 'hello', NULL, '', 0, 0, '', '')
            """.trimIndent())
            close()
        }

        helper.runMigrationsAndValidate(testDbName, 4, true, MIGRATION_3_4).use { db ->
            db.query("SELECT prompt FROM ai_call_logs WHERE timestamp = 1000").use {
                assertEquals(true, it.moveToFirst())
                assertEquals("hi", it.getString(0))
            }

            db.execSQL("INSERT INTO conversation VALUES ('c1', 'title', 100, 100)")
            db.execSQL("INSERT INTO conversation_message VALUES ('m1', 'c1', 0, 'user', 'msg', '', '', 100)")

            db.query("SELECT content FROM conversation_message WHERE id = 'm1'").use {
                assertEquals(true, it.moveToFirst())
                assertEquals("msg", it.getString(0))
            }
        }
    }
}
