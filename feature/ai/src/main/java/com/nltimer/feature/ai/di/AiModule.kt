package com.nltimer.feature.ai.di

import android.content.Context
import androidx.room.Room
import com.nltimer.feature.ai.chat.data.ConversationDao
import com.nltimer.feature.ai.chat.data.ConversationMessageDao
import com.nltimer.feature.ai.chat.data.MIGRATION_3_4
import com.nltimer.feature.ai.data.AiCallLogDao
import com.nltimer.feature.ai.data.AiInterDatabase
import com.nltimer.feature.ai.data.AiInterRepository
import com.nltimer.core.ai.config.AiConfigProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    abstract fun bindAiConfigProvider(impl: AiInterRepository): AiConfigProvider
}

@Module
@InstallIn(SingletonComponent::class)
object AiDatabaseModule {

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
