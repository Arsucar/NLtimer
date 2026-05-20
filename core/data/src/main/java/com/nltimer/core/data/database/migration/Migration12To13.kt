package com.nltimer.core.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS icon_search_miss (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                query TEXT NOT NULL,
                library TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
