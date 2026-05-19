package com.nltimer.core.tools.library

import com.nltimer.core.tools.ToolDefinition
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Library 类工具 Hilt 多绑定模块
 *
 * 该模块容纳"知识库/资源库"相关工具：标签、活动、分类的查询与创建。
 * 后续 M2 会追加 createActivity / createTag / createActivityCategory / createTagCategory。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryToolsModule {

    @Binds
    @IntoSet
    abstract fun bindListTagsTool(impl: ListTagsTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindCreateActivityCategoryTool(impl: CreateActivityCategoryTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindCreateTagCategoryTool(impl: CreateTagCategoryTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindCreateActivityTool(impl: CreateActivityTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindCreateTagTool(impl: CreateTagTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindBatchCreateActivitiesTool(impl: BatchCreateActivitiesTool): ToolDefinition
}
