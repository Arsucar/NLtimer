package com.nltimer.core.data.di

import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.repository.CategoryRepository
import com.nltimer.core.data.repository.DataExportImportRepository
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.repository.impl.ActivityManagementRepositoryImpl
import com.nltimer.core.data.repository.impl.ActivityRepositoryImpl
import com.nltimer.core.data.repository.impl.BehaviorEventRepositoryImpl
import com.nltimer.core.data.repository.impl.BehaviorRepositoryImpl
import com.nltimer.core.data.repository.impl.CategoryRepositoryImpl
import com.nltimer.core.data.repository.impl.DataExportImportRepositoryImpl
import com.nltimer.core.data.repository.impl.EventTemplateRepositoryImpl
import com.nltimer.core.data.repository.impl.TagRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    abstract fun bindBehaviorRepository(impl: BehaviorRepositoryImpl): BehaviorRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindActivityManagementRepository(
        impl: ActivityManagementRepositoryImpl,
    ): ActivityManagementRepository

    @Binds
    abstract fun bindDataExportImportRepository(
        impl: DataExportImportRepositoryImpl,
    ): DataExportImportRepository

    @Binds
    abstract fun bindEventTemplateRepository(impl: EventTemplateRepositoryImpl): EventTemplateRepository

    @Binds
    abstract fun bindBehaviorEventRepository(impl: BehaviorEventRepositoryImpl): BehaviorEventRepository
}
