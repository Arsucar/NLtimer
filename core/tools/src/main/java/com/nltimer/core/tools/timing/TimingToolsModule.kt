package com.nltimer.core.tools.timing

import com.nltimer.core.tools.ToolDefinition
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

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

    @Binds
    @IntoSet
    abstract fun bindGetWeeklySummaryTool(impl: GetWeeklySummaryTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindUpdateBehaviorTool(impl: UpdateBehaviorTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindListGoalsTool(impl: ListGoalsTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindActivateGoalTool(impl: ActivateGoalTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindDeleteGoalTool(impl: DeleteGoalTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindReorderGoalsTool(impl: ReorderGoalsTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindGetBehaviorDetailTool(impl: GetBehaviorDetailTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindGetTimeRangeSummaryTool(impl: GetTimeRangeSummaryTool): ToolDefinition

    @Binds
    @IntoSet
    abstract fun bindSearchIconsTool(impl: SearchIconsTool): ToolDefinition
}
