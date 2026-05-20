package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tag_groups", indices = [Index(value = ["name"], unique = true)])
data class TagGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
