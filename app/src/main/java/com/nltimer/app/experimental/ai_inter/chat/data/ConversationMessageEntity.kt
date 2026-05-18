package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_message",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("conversationId")],
)
data class ConversationMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val order: Int,
    val role: String,
    val content: String,
    val reasoning: String = "",
    val toolCallsJson: String = "",
    val createdAt: Long,
)
