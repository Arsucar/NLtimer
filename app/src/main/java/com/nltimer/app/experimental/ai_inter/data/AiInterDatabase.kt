package com.nltimer.app.experimental.ai_inter.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AiCallLogEntity::class], version = 3, exportSchema = false)
abstract class AiInterDatabase : RoomDatabase() {
    abstract fun aiCallLogDao(): AiCallLogDao
}
