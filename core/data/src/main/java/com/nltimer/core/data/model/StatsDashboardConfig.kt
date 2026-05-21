package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

@Immutable
@Serializable
data class StatsDashboardConfig(
    val panels: @Serializable(with = StatsPanelListSerializer::class) ImmutableList<StatsPanelConfig> = persistentListOf(),
    val defaultTimeRange: StatsTimeRange = StatsTimeRange(StatsTimeRangeType.WEEK),
)

@Immutable
@Serializable
data class StatsPanelConfig(
    val id: String = UUID.randomUUID().toString(),
    val type: StatsPanelType = StatsPanelType.SUMMARY_CARD,
    val title: String = "",
    val timeRange: StatsTimeRange? = null,
    val filters: StatsFilters = StatsFilters(),
    val displayOptions: StatsDisplayOpts = StatsDisplayOpts(),
    val colSpan: Int = 4,
    val metricKind: StatsMetricKind? = null,
)

@Serializable
enum class StatsPanelType {
    PIE_CHART,
    BAR_CHART,
    TIMELINE_HEATMAP,
    SUMMARY_CARD,
    METRIC_CARD,
    TREND_LINE,
    RANKING_LIST,
    COMPARISON,
    AI_INSIGHT,
}

@Serializable
enum class StatsMetricKind {
    TOTAL_HOURS,
    COMPLETED_COUNT,
    COMPLETION_RATE,
    PLAN_ADHERENCE,
    ACTIVE_COUNT,
    PENDING_COUNT,
}

@Immutable
@Serializable
data class StatsFilters(
    val activityIds: @Serializable(with = LongImmutableListSerializer::class) ImmutableList<Long> = persistentListOf(),
    val tagIds: @Serializable(with = LongImmutableListSerializer::class) ImmutableList<Long> = persistentListOf(),
    val groupIds: @Serializable(with = LongImmutableListSerializer::class) ImmutableList<Long> = persistentListOf(),
    val wasPlanned: Boolean? = null,
)

@Serializable
enum class StatsTimeRangeType { DAY, WEEK, MONTH, CUSTOM }

@Immutable
@Serializable
data class StatsTimeRange(
    val type: StatsTimeRangeType = StatsTimeRangeType.WEEK,
    val offset: Int = 0,
    val customStart: Long? = null,
    val customEnd: Long? = null,
)

@Immutable
@Serializable
data class StatsDisplayOpts(
    val sortBy: StatsSortField = StatsSortField.DURATION,
    val sortOrder: StatsSortOrder = StatsSortOrder.DESC,
    val maxItems: Int = 10,
    val showIcon: Boolean = true,
    val showPercentage: Boolean = true,
    val colorScheme: StatsColorScheme = StatsColorScheme.ACTIVITY_COLOR,
)

@Serializable
enum class StatsSortField { DURATION, COUNT, NAME, ACHIEVEMENT }

@Serializable
enum class StatsSortOrder { ASC, DESC }

@Serializable
enum class StatsColorScheme { ACTIVITY_COLOR, AUTO, MONO }

fun defaultStatsDashboardConfig(): StatsDashboardConfig = StatsDashboardConfig(
    panels = persistentListOf(
        StatsPanelConfig(id = "metric_hours", type = StatsPanelType.METRIC_CARD, title = "总时长", metricKind = StatsMetricKind.TOTAL_HOURS, colSpan = 2),
        StatsPanelConfig(id = "metric_done", type = StatsPanelType.METRIC_CARD, title = "完成数", metricKind = StatsMetricKind.COMPLETED_COUNT, colSpan = 2),
        StatsPanelConfig(id = "metric_rate", type = StatsPanelType.METRIC_CARD, title = "完成率", metricKind = StatsMetricKind.COMPLETION_RATE, colSpan = 2),
        StatsPanelConfig(id = "metric_plan", type = StatsPanelType.METRIC_CARD, title = "计划达成", metricKind = StatsMetricKind.PLAN_ADHERENCE, colSpan = 2),
        StatsPanelConfig(id = "pie", type = StatsPanelType.PIE_CHART, title = "活动占比"),
        StatsPanelConfig(id = "ranking", type = StatsPanelType.RANKING_LIST, title = "活动排行"),
    ),
)

internal object StatsPanelListSerializer : KSerializer<ImmutableList<StatsPanelConfig>> {
    private val delegate = ListSerializer(StatsPanelConfig.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun serialize(encoder: Encoder, value: ImmutableList<StatsPanelConfig>) {
        delegate.serialize(encoder, value.toList())
    }
    override fun deserialize(decoder: Decoder): ImmutableList<StatsPanelConfig> {
        return delegate.deserialize(decoder).toImmutableList()
    }
}

internal object LongImmutableListSerializer : KSerializer<ImmutableList<Long>> {
    private val longSer = object : KSerializer<Long> {
        override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Long", kotlinx.serialization.descriptors.PrimitiveKind.LONG)
        override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
        override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
    }
    private val delegate = ListSerializer(longSer)
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun serialize(encoder: Encoder, value: ImmutableList<Long>) {
        delegate.serialize(encoder, value.toList())
    }
    override fun deserialize(decoder: Decoder): ImmutableList<Long> {
        return delegate.deserialize(decoder).toImmutableList()
    }
}
