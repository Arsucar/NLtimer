package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.TagGroupEntity

@Immutable
data class TagGroup(
    val id: Long = 0,
    val name: String,
    val iconKey: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
) {
    fun toEntity() = TagGroupEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        sortOrder = sortOrder,
        isArchived = isArchived,
        archivedAt = archivedAt,
    )

    companion object {
        fun fromEntity(entity: TagGroupEntity) = TagGroup(
            id = entity.id,
            name = entity.name,
            iconKey = entity.iconKey,
            sortOrder = entity.sortOrder,
            isArchived = entity.isArchived,
            archivedAt = entity.archivedAt,
        )
    }
}
