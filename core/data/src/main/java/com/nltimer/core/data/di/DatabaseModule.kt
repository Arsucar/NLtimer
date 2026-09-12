package com.nltimer.core.data.di

import android.content.Context
import androidx.room.Room
import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.BehaviorEventDao
import com.nltimer.core.data.database.dao.BehaviorEventValueDao
import com.nltimer.core.data.database.dao.EventTemplateDao
import com.nltimer.core.data.database.dao.EventTemplateFieldDao
import com.nltimer.core.data.database.dao.EventTemplateTagBindingDao
import com.nltimer.core.data.database.dao.IconSearchMissDao
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.dao.TagGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Suppress("SpreadOperator") // Room 的 addMigrations 接收 vararg，数组必须使用展开运算符
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NLtimerDatabase =
        Room.databaseBuilder(
            context,
            NLtimerDatabase::class.java,
            "nltimer-database",
        )
            .fallbackToDestructiveMigration(true)
            .addMigrations(*NLtimerDatabase.ALL_MIGRATIONS)
            .setQueryExecutor(Executors.newFixedThreadPool(4, namedThreadFactory("room-query")))
            .build()

    private fun namedThreadFactory(prefix: String): ThreadFactory {
        val count = AtomicInteger(1)
        return ThreadFactory { runnable ->
            Thread(runnable, "$prefix-${count.getAndIncrement()}")
        }
    }

    @Provides
    fun provideActivityDao(database: NLtimerDatabase): ActivityDao =
        database.activityDao()

    @Provides
    fun provideActivityGroupDao(database: NLtimerDatabase): ActivityGroupDao =
        database.activityGroupDao()

    @Provides
    fun provideTagDao(database: NLtimerDatabase): TagDao =
        database.tagDao()

    @Provides
    fun provideTagGroupDao(database: NLtimerDatabase): TagGroupDao =
        database.tagGroupDao()

    @Provides
    fun provideBehaviorDao(database: NLtimerDatabase): BehaviorDao =
        database.behaviorDao()

    @Provides
    fun provideIconSearchMissDao(database: NLtimerDatabase): IconSearchMissDao =
        database.iconSearchMissDao()

    @Provides
    fun provideEventTemplateDao(database: NLtimerDatabase): EventTemplateDao =
        database.eventTemplateDao()

    @Provides
    fun provideEventTemplateFieldDao(database: NLtimerDatabase): EventTemplateFieldDao =
        database.eventTemplateFieldDao()

    @Provides
    fun provideBehaviorEventDao(database: NLtimerDatabase): BehaviorEventDao =
        database.behaviorEventDao()

    @Provides
    fun provideBehaviorEventValueDao(database: NLtimerDatabase): BehaviorEventValueDao =
        database.behaviorEventValueDao()

    @Provides
    fun provideEventTemplateTagBindingDao(database: NLtimerDatabase): EventTemplateTagBindingDao =
        database.eventTemplateTagBindingDao()
}
