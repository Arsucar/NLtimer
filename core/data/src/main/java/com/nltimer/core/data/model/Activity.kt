package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.ActivityEntity

/**
 * Activity 活动领域模型
 * 表示一个可计时、可归档的活动条目，对应数据库中的 activities 表
 */
@Immutable
data class Activity(
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
) {
    fun toEntity() = ActivityEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        keywords = keywords,
        groupId = groupId,
        isPreset = isPreset,
        isArchived = isArchived,
        archivedAt = archivedAt,
        archiveNote = archiveNote,
        color = color,
        usageCount = usageCount,
    )

    companion object {
        fun fromEntity(entity: ActivityEntity) = Activity(
            id = entity.id,
            name = entity.name,
            iconKey = entity.iconKey,
            keywords = entity.keywords,
            groupId = entity.groupId,
            isPreset = entity.isPreset,
            isArchived = entity.isArchived,
            archivedAt = entity.archivedAt,
            archiveNote = entity.archiveNote,
            color = entity.color,
            usageCount = entity.usageCount,
        )
    }
}
