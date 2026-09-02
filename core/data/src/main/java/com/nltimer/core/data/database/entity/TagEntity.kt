package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true),
        Index("isArchived"),
        Index("category"),
        Index("groupId"),
    ],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long? = null,
    val iconKey: String? = null,
    val category: String? = null,
    val groupId: Long? = null,
    val priority: Int = 0,
    val usageCount: Int = 0,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveNote: String? = null,
    val keywords: String? = null,
)
