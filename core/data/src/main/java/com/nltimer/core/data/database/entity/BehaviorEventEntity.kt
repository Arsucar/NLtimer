package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BehaviorEventEntity 行为事件实体
 * 对应 behavior_event 表，记录一次结构化事件（打点）
 *
 * behaviorId 为空表示独立事件（无活跃计时也可打点）；
 * 非空时通过外键关联 behaviors 表，行为删除默认级联删除事件
 * （保留事件转独立 = behaviorId 置空，ConvertEventToIndependentUseCase）。
 * activityId 为冗余字段（无外键），便于按活动聚合展示。
 */
@Entity(
    tableName = "behavior_event",
    foreignKeys = [
        ForeignKey(
            entity = BehaviorEntity::class,
            parentColumns = ["id"],
            childColumns = ["behaviorId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EventTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("behaviorId"),
        Index("templateId"),
        Index(value = ["activityId", "timestamp"], orders = [Index.Order.ASC, Index.Order.DESC]),
        Index(value = ["timestamp"], orders = [Index.Order.DESC]),
    ],
)
data class BehaviorEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val behaviorId: Long? = null,
    val activityId: Long? = null,
    val templateId: Long,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
