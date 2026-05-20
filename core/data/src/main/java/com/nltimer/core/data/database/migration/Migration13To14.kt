package com.nltimer.core.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tag_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                iconKey TEXT,
                sortOrder INTEGER NOT NULL,
                isArchived INTEGER NOT NULL,
                archivedAt INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tag_groups_name ON tag_groups(name)")
        db.execSQL("ALTER TABLE tags ADD COLUMN groupId INTEGER")
        db.execSQL("ALTER TABLE activity_groups ADD COLUMN iconKey TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_groupId ON tags(groupId)")
    }
}
