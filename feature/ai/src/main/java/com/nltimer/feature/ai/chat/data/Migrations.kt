package com.nltimer.feature.ai.chat.data

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
