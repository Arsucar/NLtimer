package com.nltimer.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.BehaviorEntity
import com.nltimer.core.data.database.entity.BehaviorTagCrossRefEntity
import com.nltimer.core.data.database.entity.TagEntity
import com.nltimer.core.data.database.migration.MIGRATION_10_11
import com.nltimer.core.data.database.migration.MIGRATION_11_12
import com.nltimer.core.data.database.migration.MIGRATION_3_4
import com.nltimer.core.data.database.migration.MIGRATION_4_5
import com.nltimer.core.data.database.migration.MIGRATION_5_6
import com.nltimer.core.data.database.migration.MIGRATION_6_7
import com.nltimer.core.data.database.migration.MIGRATION_7_8
import com.nltimer.core.data.database.migration.MIGRATION_8_9
import com.nltimer.core.data.database.migration.MIGRATION_9_10

@Database(
    entities = [
        ActivityEntity::class,
        ActivityGroupEntity::class,
        TagEntity::class,
        BehaviorEntity::class,
        ActivityTagBindingEntity::class,
        BehaviorTagCrossRefEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class NLtimerDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun activityGroupDao(): ActivityGroupDao
    abstract fun tagDao(): TagDao
    abstract fun behaviorDao(): BehaviorDao

    companion object {
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
        )
    }
}
