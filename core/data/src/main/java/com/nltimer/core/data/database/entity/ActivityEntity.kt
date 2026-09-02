package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ActivityEntity 活动数据库实体
 * 对应 activities 表，存储活动名称、图标、分组、预设/归档状态和时间戳
 */
@Entity(tableName = "activities", indices = [Index(value = ["name"], unique = true), Index("isArchived"), Index("groupId")])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String? = null,
    val keywords: String? = null,
    val groupId: Long? = null,
    val isPreset: Boolean = false,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveNote: String? = null,
    val color: Long? = null,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
