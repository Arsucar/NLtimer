package com.nltimer.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.BehaviorEventDao
import com.nltimer.core.data.database.dao.BehaviorEventValueDao
import com.nltimer.core.data.database.dao.EventTemplateDao
import com.nltimer.core.data.database.dao.EventTemplateFieldDao
import com.nltimer.core.data.database.dao.EventTemplateTagBindingDao
import com.nltimer.core.data.database.dao.IconSearchMissDao
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.dao.TagGroupDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.BehaviorEntity
import com.nltimer.core.data.database.entity.BehaviorEventEntity
import com.nltimer.core.data.database.entity.BehaviorEventValueEntity
import com.nltimer.core.data.database.entity.BehaviorTagCrossRefEntity
import com.nltimer.core.data.database.entity.EventTemplateEntity
import com.nltimer.core.data.database.entity.EventTemplateFieldEntity
import com.nltimer.core.data.database.entity.EventTemplateTagBindingEntity
import com.nltimer.core.data.database.entity.IconSearchMissEntity
import com.nltimer.core.data.database.entity.TagEntity
import com.nltimer.core.data.database.entity.TagGroupEntity
import com.nltimer.core.data.database.migration.MIGRATION_10_11
import com.nltimer.core.data.database.migration.MIGRATION_11_12
import com.nltimer.core.data.database.migration.MIGRATION_12_13
import com.nltimer.core.data.database.migration.MIGRATION_13_14
import com.nltimer.core.data.database.migration.MIGRATION_14_15
import com.nltimer.core.data.database.migration.MIGRATION_15_16
import com.nltimer.core.data.database.migration.MIGRATION_16_17
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
        TagGroupEntity::class,
        BehaviorEntity::class,
        ActivityTagBindingEntity::class,
        BehaviorTagCrossRefEntity::class,
        IconSearchMissEntity::class,
        EventTemplateEntity::class,
        EventTemplateFieldEntity::class,
        BehaviorEventEntity::class,
        BehaviorEventValueEntity::class,
        EventTemplateTagBindingEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class NLtimerDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun activityGroupDao(): ActivityGroupDao
    abstract fun tagDao(): TagDao
    abstract fun tagGroupDao(): TagGroupDao
    abstract fun behaviorDao(): BehaviorDao
    abstract fun iconSearchMissDao(): IconSearchMissDao
    abstract fun eventTemplateDao(): EventTemplateDao
    abstract fun eventTemplateFieldDao(): EventTemplateFieldDao
    abstract fun behaviorEventDao(): BehaviorEventDao
    abstract fun behaviorEventValueDao(): BehaviorEventValueDao
    abstract fun eventTemplateTagBindingDao(): EventTemplateTagBindingDao

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
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
        )
    }
}
