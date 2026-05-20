package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nltimer.core.data.database.entity.IconSearchMissEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IconSearchMissDao {
    @Insert
    suspend fun insert(entity: IconSearchMissEntity): Long

    @Query("SELECT * FROM icon_search_miss ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<IconSearchMissEntity>>

    @Query("SELECT * FROM icon_search_miss ORDER BY timestamp DESC")
    suspend fun getAll(): List<IconSearchMissEntity>

    @Query("SELECT COUNT(*) FROM icon_search_miss")
    suspend fun count(): Int

    @Query("DELETE FROM icon_search_miss")
    suspend fun clear()
}
