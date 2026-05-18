package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_message WHERE conversationId = :id ORDER BY `order` ASC")
    fun observeByConversation(id: String): Flow<List<ConversationMessageEntity>>

    @Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM conversation_message WHERE conversationId = :id")
    suspend fun nextOrder(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ConversationMessageEntity)

    @Query("DELETE FROM conversation_message WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversation_message WHERE conversationId = :id AND `order` >= :fromOrder")
    suspend fun deleteFromOrder(id: String, fromOrder: Int)

    @Query("DELETE FROM conversation_message WHERE conversationId = :id")
    suspend fun deleteAllInConversation(id: String)
}
