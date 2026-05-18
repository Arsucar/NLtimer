package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conv: ConversationEntity)

    @Query("UPDATE conversation SET title = :title, updatedAt = :ts WHERE id = :id")
    suspend fun rename(id: String, title: String, ts: Long)

    @Query("UPDATE conversation SET updatedAt = :ts WHERE id = :id")
    suspend fun touch(id: String, ts: Long)

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun delete(id: String)
}
