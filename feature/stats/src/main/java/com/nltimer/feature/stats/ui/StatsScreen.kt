package com.nltimer.feature.stats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.ActivityStat
import com.nltimer.core.data.model.EventsViewMode
import com.nltimer.core.data.model.StatsMetricKind
import com.nltimer.core.data.model.StatsPanelConfig
import com.nltimer.core.data.model.StatsPanelType
import com.nltimer.core.data.model.StatsResult
import com.nltimer.core.data.model.StatsTimeRange
import com.nltimer.core.data.model.StatsTimeRangeType
import com.nltimer.core.designsystem.component.LocalNavBarWidth
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.feature.stats.model.EventPanelFilters
import com.nltimer.feature.stats.model.EventsPanelUiState
import com.nltimer.feature.stats.model.StatsUiState
import com.nltimer.feature.stats.ui.component.ActivityRankSection
import com.nltimer.feature.stats.ui.component.EventsPanelContent
import com.nltimer.feature.stats.ui.component.BarChartCard
import com.nltimer.feature.stats.ui.component.CategoryShareCard
import com.nltimer.feature.stats.ui.component.ComparisonCard
import com.nltimer.feature.stats.ui.component.CoreMetricsGrid
import com.nltimer.feature.stats.ui.component.SingleMetricCard
import com.nltimer.feature.stats.ui.component.StatsGridContainer
import com.nltimer.feature.stats.ui.component.TrendCard

internal val CardShape = RoundedCornerShape(22.dp)
internal val CardSpacing = 14.dp
internal val CardInnerPadding = 18.dp
internal val ContentHorizontalPadding = 16.dp
private val OverlayNavBarHeight = 72.dp
private val EditBarContentHeight = 72.dp
private val MinEditGridBottomPadding = 160.dp
private val MinBrowseGridBottomPadding = 100.dp

private val ImplementedPanelTypes = setOf(
    StatsPanelType.SUMMARY_CARD,
    StatsPanelType.METRIC_CARD,
    StatsPanelType.TREND_LINE,
    StatsPanelType.PIE_CHART,
    StatsPanelType.RANKING_LIST,
    StatsPanelType.BAR_CHART,
    StatsPanelType.COMPARISON,
    StatsPanelType.EVENTS,
)

private val PanelTypeLabels = mapOf(
    StatsPanelType.SUMMARY_CARD to "概览卡片",
    StatsPanelType.METRIC_CARD to "指标卡片",
    StatsPanelType.BAR_CHART to "柱状图",
    StatsPanelType.PIE_CHART to "饼图",
    StatsPanelType.RANKING_LIST to "排行榜",
    StatsPanelType.TREND_LINE to "趋势线",
    StatsPanelType.TIMELINE_HEATMAP to "时间热力图",
    StatsPanelType.COMPARISON to "对比",
    StatsPanelType.AI_INSIGHT to "AI 洞察",
    StatsPanelType.EVENTS to "复盘事件",
)

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onTimeRangeChange: (StatsTimeRange) -> Unit,
    onToggleEditMode: () -> Unit,
    onMovePanel: (fromIndex: Int, toIndex: Int) -> Unit,
    onPersistPanels: () -> Unit,
    onRemovePanel: (panelId: String) -> Unit,
    onAddPanel: (StatsPanelType) -> Unit,
    onResetToDefault: () -> Unit,
    eventsPanel: EventsPanelUiState = EventsPanelUiState(),
    onEventsViewModeChange: (EventsViewMode) -> Unit = {},
    onEventsFocusTemplateChange: (Long?) -> Unit = {},
    onEventsFilterChange: (EventPanelFilters) -> Unit = {},
    onBarClick: ((ActivityStat) -> Unit)? = null,
    onDismissActivity: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val immersiveTopPadding = LocalImmersiveTopPadding.current
    val isCenterFab = LocalTheme.current.bottomBarMode == BottomBarMode.CENTER_FAB
    val navBarWidth = LocalNavBarWidth.current.value
    val overlayNavClearance = if (isCenterFab || navBarWidth > 0.dp) OverlayNavBarHeight else 0.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gridBottomPadding = if (uiState.isEditMode) {
        maxOf(
            MinEditGridBottomPadding,
            EditBarContentHeight + overlayNavClearance + navBarBottom + 16.dp,
        )
    } else {
        maxOf(MinBrowseGridBottomPadding, overlayNavClearance + navBarBottom + 16.dp)
    }
    var showAddPanelDialog by remember { mutableStateOf(false) }

    if (showAddPanelDialog) {
        AddPanelDialog(
            onDismiss = { showAddPanelDialog = false },
            onAdd = { type ->
                onAddPanel(type)
                showAddPanelDialog = false
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        val panels = uiState.dashboardConfig.panels

        Column(modifier = Modifier.fillMaxSize()) {
            StatsGridContainer(
                panels = panels,
                isEditMode = uiState.isEditMode,
                onReorder = onMovePanel,
                onReorderPersist = onPersistPanels,
                onRemove = onRemovePanel,
                panelContent = { panel ->
                    PanelContent(
                        panel = panel,
                        result = uiState.statsResult,
                        eventsPanel = eventsPanel,
                        onEventsViewModeChange = onEventsViewModeChange,
                        onEventsFocusTemplateChange = onEventsFocusTemplateChange,
                        onEventsFilterChange = onEventsFilterChange,
                        onBarClick = onBarClick,
                    )
                },
                modifier = Modifier.weight(1f),
                topPadding = 16.dp + immersiveTopPadding,
                bottomContentPadding = gridBottomPadding,
                headerContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TimeRangeChips(
                            currentTimeRange = uiState.currentTimeRange,
                            onTimeRangeChange = onTimeRangeChange,
                            isEditMode = uiState.isEditMode,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onToggleEditMode,
                            modifier = Modifier.padding(start = 4.dp),
                        ) {
                            Icon(
                                imageVector = if (uiState.isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (uiState.isEditMode) "完成编辑" else "编辑面板",
                                tint = if (uiState.isEditMode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )

            // 详情区域
            uiState.selectedActivity?.let { activity ->
                ActivityDetailPanel(
                    activity = activity,
                    onDismiss = onDismissActivity,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        if (uiState.statsResult == null && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = overlayNavClearance),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentHorizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { showAddPanelDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("添加面板")
                    }
                    FilledTonalButton(
                        onClick = onResetToDefault,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text("恢复默认")
                    }
                    FilledTonalButton(
                        onClick = onToggleEditMode,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("取消编辑")
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelContent(
    panel: StatsPanelConfig,
    result: StatsResult?,
    eventsPanel: EventsPanelUiState = EventsPanelUiState(),
    onEventsViewModeChange: (EventsViewMode) -> Unit = {},
    onEventsFocusTemplateChange: (Long?) -> Unit = {},
    onEventsFilterChange: (EventPanelFilters) -> Unit = {},
    onBarClick: ((ActivityStat) -> Unit)? = null,
) {
    when (panel.type) {
        StatsPanelType.SUMMARY_CARD -> CoreMetricsGrid(result ?: return)
        StatsPanelType.METRIC_CARD -> {
            val kind = panel.metricKind ?: StatsMetricKind.TOTAL_HOURS
            SingleMetricCard(result = result ?: return, metricKind = kind)
        }
        StatsPanelType.TREND_LINE -> {
            if (result != null && result.dailyBreakdown.isNotEmpty()) {
                TrendCard(result)
            } else {
                EmptyPanel("暂无趋势数据")
            }
        }
        StatsPanelType.PIE_CHART -> {
            if (result != null && result.activityStats.isNotEmpty()) {
                CategoryShareCard(result)
            } else {
                EmptyPanel("暂无分类数据")
            }
        }
        StatsPanelType.RANKING_LIST -> {
            if (result != null && result.activityStats.isNotEmpty()) {
                ActivityRankSection(result)
            } else {
                EmptyPanel("暂无排行数据")
            }
        }
        StatsPanelType.BAR_CHART -> {
            if (result != null && result.activityStats.isNotEmpty()) {
                BarChartCard(result, onBarClick)
            } else {
                EmptyPanel("暂无柱状图数据")
            }
        }
        StatsPanelType.COMPARISON -> ComparisonCard(result ?: return)
        StatsPanelType.EVENTS -> EventsPanelContent(
            state = eventsPanel,
            onViewModeChange = onEventsViewModeChange,
            onFocusTemplateChange = onEventsFocusTemplateChange,
            onFilterChange = onEventsFilterChange,
        )
        else -> EmptyPanel("${panel.title}（即将支持）")
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimeRangeChips(
    currentTimeRange: StatsTimeRange,
    onTimeRangeChange: (StatsTimeRange) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.alpha(if (isEditMode) 0.5f else 1f),
    ) {
        listOf(
            "今日" to StatsTimeRangeType.DAY,
            "本周" to StatsTimeRangeType.WEEK,
            "本月" to StatsTimeRangeType.MONTH,
        ).forEach { (label, type) ->
            val selected = currentTimeRange.type == type && currentTimeRange.offset == 0
            FilterChip(
                selected = selected,
                onClick = { if (!isEditMode) onTimeRangeChange(StatsTimeRange(type = type)) },
                label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun AddPanelDialog(
    onDismiss: () -> Unit,
    onAdd: (StatsPanelType) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加面板") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatsPanelType.entries.forEach { type ->
                    val label = PanelTypeLabels[type] ?: type.name
                    val implemented = type in ImplementedPanelTypes
                    Surface(
                        onClick = { if (implemented) onAdd(type) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (implemented) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                color = if (implemented) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!implemented) {
                                Text(
                                    text = "即将支持",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun ActivityDetailPanel(
    activity: ActivityStat,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "活动详情",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "活动名称：${activity.activityName}",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = "总时长：${activity.durationMinutes} 分钟",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "次数：${activity.count} 次",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (activity.avgAchievement != null) {
                Text(
                    text = "平均完成度：${activity.avgAchievement}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
