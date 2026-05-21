package com.nltimer.feature.ai.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nltimer.feature.ai.chat.data.ConversationDao
import com.nltimer.feature.ai.chat.data.ConversationEntity
import com.nltimer.feature.ai.chat.data.ConversationMessageDao
import com.nltimer.feature.ai.chat.data.ConversationMessageEntity

@Database(
    entities = [
        AiCallLogEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AiInterDatabase : RoomDatabase() {
    abstract fun aiCallLogDao(): AiCallLogDao
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationMessageDao(): ConversationMessageDao
}
