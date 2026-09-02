package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.TagEntity

@Immutable
data class Tag(
    val id: Long,
    val name: String,
    val color: Long?,
    val iconKey: String?,
    val category: String?,
    val groupId: Long?,
    val priority: Int,
    val usageCount: Int,
    val sortOrder: Int,
    val keywords: String?,
    val isArchived: Boolean,
    val archivedAt: Long? = null,
    val archiveNote: String? = null,
) {
    fun toEntity() = TagEntity(
        id = id,
        name = name,
        color = color,
        iconKey = iconKey,
        category = category,
        groupId = groupId,
        priority = priority,
        usageCount = usageCount,
        sortOrder = sortOrder,
        keywords = keywords,
        isArchived = isArchived,
        archivedAt = archivedAt,
        archiveNote = archiveNote,
    )

    companion object {
        fun fromEntity(entity: TagEntity) = Tag(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            iconKey = entity.iconKey,
            category = entity.category,
            groupId = entity.groupId,
            priority = entity.priority,
            usageCount = entity.usageCount,
            sortOrder = entity.sortOrder,
            keywords = entity.keywords,
            isArchived = entity.isArchived,
            archivedAt = entity.archivedAt,
            archiveNote = entity.archiveNote,
        )
    }
}
