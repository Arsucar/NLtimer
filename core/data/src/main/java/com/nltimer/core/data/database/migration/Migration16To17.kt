package com.nltimer.core.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * MIGRATION_16_17 结构化事件记录器建表迁移
 *
 * 一次性创建 5 张表（全部为新建表，无需重建旧表，因此不需要 PRAGMA foreign_keys 切换）：
 * 1. event_template            打点模板
 * 2. event_template_field      模板字段（FK→event_template CASCADE）
 * 3. behavior_event            行为事件（behaviorId 可空=独立事件；FK→behaviors/event_template 均 CASCADE）
 * 4. behavior_event_value      EAV 字段值（FK→behavior_event / event_template_field 均 CASCADE）
 * 5. event_template_tag_binding 模板-标签绑定（双 FK CASCADE + 联合主键）
 *
 * 列定义与 @Entity 注解产物一致（无 @ColumnInfo(defaultValue)，所以不写 DEFAULT 子句）；
 * 索引命名遵循 Room 生成规则 index_<表>_<列...>，含 DESC 列序索引（@Index orders 声明）。
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `event_template` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `event_template_field` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `optionsJson` TEXT, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`templateId`) REFERENCES `event_template`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `behavior_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `behaviorId` INTEGER, `activityId` INTEGER, `templateId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`behaviorId`) REFERENCES `behaviors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`templateId`) REFERENCES `event_template`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `behavior_event_value` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, `fieldId` INTEGER NOT NULL, `valueText` TEXT, `valueNumber` REAL, FOREIGN KEY(`eventId`) REFERENCES `behavior_event`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`fieldId`) REFERENCES `event_template_field`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `event_template_tag_binding` (`templateId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`templateId`, `tagId`), FOREIGN KEY(`templateId`) REFERENCES `event_template`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_template_field_templateId` ON `event_template_field` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_behaviorId` ON `behavior_event` (`behaviorId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_templateId` ON `behavior_event` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_activityId_timestamp` ON `behavior_event` (`activityId` ASC, `timestamp` DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_timestamp` ON `behavior_event` (`timestamp` DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_value_eventId` ON `behavior_event_value` (`eventId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_value_fieldId_valueNumber` ON `behavior_event_value` (`fieldId`, `valueNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_behavior_event_value_fieldId_valueText` ON `behavior_event_value` (`fieldId`, `valueText`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_template_tag_binding_templateId` ON `event_template_tag_binding` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_template_tag_binding_tagId` ON `event_template_tag_binding` (`tagId`)")
    }
}
