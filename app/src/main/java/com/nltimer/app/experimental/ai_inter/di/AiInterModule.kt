package com.nltimer.app.experimental.ai_inter.di

import android.content.Context
import androidx.room.Room
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationDao
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageDao
import com.nltimer.app.experimental.ai_inter.chat.data.MIGRATION_3_4
import com.nltimer.app.experimental.ai_inter.data.AiCallLogDao
import com.nltimer.app.experimental.ai_inter.data.AiInterDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiInterModule {

    @Provides
    @Singleton
    fun provideAiInterDatabase(@ApplicationContext context: Context): AiInterDatabase {
        return Room.databaseBuilder(
            context,
            AiInterDatabase::class.java,
            "ai_inter_database",
        )
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideAiCallLogDao(database: AiInterDatabase): AiCallLogDao {
        return database.aiCallLogDao()
    }

    @Provides
    fun provideConversationDao(database: AiInterDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideConversationMessageDao(database: AiInterDatabase): ConversationMessageDao {
        return database.conversationMessageDao()
    }
}
