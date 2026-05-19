package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * BehaviorEntity 行为记录数据库实体
 * 对应 behaviors 表，记录活动的计时起止、状态、备注、番茄钟、完成度等信息
 * 通过 activityId 外键关联 activities 表
 */
@Entity(
    tableName = "behaviors",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("activityId"),
        Index(value = ["startTime", "sequence"]),
        Index("status"),
        Index(value = ["startTime", "status"]),
        Index(value = ["activityId", "status"]),
    ],
)
data class BehaviorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String = "pending",
    val note: String? = null,
    val pomodoroCount: Int = 0,
    val sequence: Int = 0,
    val estimatedDuration: Long? = null,
    val actualDuration: Long? = null,
    val achievementLevel: Int? = null,
    val wasPlanned: Boolean = false,
)
