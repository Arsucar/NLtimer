package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BehaviorEventValueEntity 行为事件字段值实体（EAV 行）
 * 对应 behavior_event_value 表，按 eventId + fieldId 存储事件单条字段值
 *
 * @property valueText 文本/单选字段的值
 * @property valueNumber 数值/星级字段的值（REAL 存储）
 * 事件删除时随 eventId 级联删除；模板字段删除时随 fieldId 级联删除
 */
@Entity(
    tableName = "behavior_event_value",
    foreignKeys = [
        ForeignKey(
            entity = BehaviorEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EventTemplateFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("eventId"),
        Index(value = ["fieldId", "valueNumber"]),
        Index(value = ["fieldId", "valueText"]),
    ],
)
data class BehaviorEventValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: Long,
    val fieldId: Long,
    val valueText: String? = null,
    val valueNumber: Double? = null,
)
