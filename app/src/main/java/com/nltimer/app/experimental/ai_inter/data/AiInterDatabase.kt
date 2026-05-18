package com.nltimer.app.experimental.ai_inter.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity

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
