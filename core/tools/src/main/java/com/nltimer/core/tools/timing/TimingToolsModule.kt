package com.nltimer.core.tools.timing

import com.nltimer.core.tools.ToolDefinition
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Timing 类工具 Hilt 多绑定模块
 *
 * 通过 @IntoSet 把每个 ToolDefinition 实现贡献到全局 Set<ToolDefinition>，
 * 由 [com.nltimer.core.tools.ToolRegistry] 构造函数自动消费。
 *
 * 后续新增 timing 工具时，在此模块追加一个 @Binds @IntoSet 方法即可，
 * 无需修改 ToolRegistry 或 ToolsModule。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimingToolsModule {

    @Binds
    @IntoSet
    abstract fun bindListActivitiesTool(impl: ListActivitiesTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindQueryCurrentBehaviorTool(impl: QueryCurrentBehaviorTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindStartBehaviorTool(impl: StartBehaviorTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindEndBehaviorTool(impl: EndBehaviorTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindRecordBehaviorTool(impl: RecordBehaviorTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindCreateGoalTool(impl: CreateGoalTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindDeleteBehaviorTool(impl: DeleteBehaviorTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindListBehaviorsTool(impl: ListBehaviorsTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindGetDailySummaryTool(impl: GetDailySummaryTool): ToolDefinition
}
