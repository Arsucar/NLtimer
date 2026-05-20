package com.nltimer.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "icon_search_miss")
data class IconSearchMissEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "library")
    val library: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
