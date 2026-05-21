package com.nltimer.feature.stats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.StatsMetricKind
import com.nltimer.core.data.model.StatsPanelConfig
import com.nltimer.core.data.model.StatsPanelType
import com.nltimer.core.data.model.StatsResult
import com.nltimer.core.data.model.StatsTimeRange
import com.nltimer.core.data.model.StatsTimeRangeType
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.feature.stats.model.StatsUiState
import com.nltimer.feature.stats.ui.component.ActivityRankSection
import com.nltimer.feature.stats.ui.component.BarChartCard
import com.nltimer.feature.stats.ui.component.CategoryShareCard
import com.nltimer.feature.stats.ui.component.ComparisonCard
import com.nltimer.feature.stats.ui.component.CoreMetricsGrid
import com.nltimer.feature.stats.ui.component.SingleMetricCard
import com.nltimer.feature.stats.ui.component.TrendCard

internal val CardShape = RoundedCornerShape(22.dp)
internal val CardSpacing = 14.dp
internal val CardInnerPadding = 18.dp
internal val ContentHorizontalPadding = 16.dp

private val ImplementedPanelTypes = setOf(
    StatsPanelType.SUMMARY_CARD,
    StatsPanelType.METRIC_CARD,
    StatsPanelType.TREND_LINE,
    StatsPanelType.PIE_CHART,
    StatsPanelType.RANKING_LIST,
    StatsPanelType.BAR_CHART,
    StatsPanelType.COMPARISON,
)

private val MetricKindLabels = mapOf(
    StatsMetricKind.TOTAL_HOURS to "总时长",
    StatsMetricKind.COMPLETED_COUNT to "完成数",
    StatsMetricKind.COMPLETION_RATE to "完成率",
    StatsMetricKind.PLAN_ADHERENCE to "计划达成",
    StatsMetricKind.ACTIVE_COUNT to "进行中",
    StatsMetricKind.PENDING_COUNT to "待处理",
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
)

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onTimeRangeChange: (StatsTimeRange) -> Unit,
    onToggleEditMode: () -> Unit,
    onMovePanel: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemovePanel: (panelId: String) -> Unit,
    onAddPanel: (StatsPanelType) -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val immersiveTopPadding = LocalImmersiveTopPadding.current
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = ContentHorizontalPadding,
                end = ContentHorizontalPadding,
                top = 16.dp + immersiveTopPadding,
                bottom = if (uiState.isEditMode) 160.dp else 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(CardSpacing),
            horizontalArrangement = Arrangement.spacedBy(CardSpacing),
        ) {
            item(span = { GridItemSpan(4) }) {
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
            }

            if (uiState.isLoading) {
                item(span = { GridItemSpan(4) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.statsResult != null && !uiState.isLoading) {
                val result = uiState.statsResult
                itemsIndexed(
                    items = panels,
                    key = { _, panel -> panel.id },
                    span = { _, panel ->
                        GridItemSpan(panel.colSpan.coerceIn(1, 4))
                    },
                ) { index, panel ->
                    PanelSlot(
                        panel = panel,
                        panelIndex = index,
                        totalPanels = panels.size,
                        result = result,
                        isEditMode = uiState.isEditMode,
                        onMoveUp = { onMovePanel(index, index - 1) },
                        onMoveDown = { onMovePanel(index, index + 1) },
                        onRemove = { onRemovePanel(panel.id) },
                    )
                }
            }

            if (uiState.statsResult == null && !uiState.isLoading) {
                item(span = { GridItemSpan(4) }) {
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
            }
        }

        if (uiState.isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentHorizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                }
            }
        }
    }
}

@Composable
private fun PanelSlot(
    panel: StatsPanelConfig,
    panelIndex: Int,
    totalPanels: Int,
    result: StatsResult,
    isEditMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val panelContent: @Composable () -> Unit = when (panel.type) {
        StatsPanelType.SUMMARY_CARD -> { { CoreMetricsGrid(result) } }
        StatsPanelType.METRIC_CARD -> {
            val kind = panel.metricKind ?: StatsMetricKind.TOTAL_HOURS
            { SingleMetricCard(result = result, metricKind = kind) }
        }
        StatsPanelType.TREND_LINE -> {
            if (result.dailyBreakdown.isNotEmpty()) {
                { TrendCard(result) }
            } else {
                { EmptyPanel("暂无趋势数据") }
            }
        }
        StatsPanelType.PIE_CHART -> {
            if (result.activityStats.isNotEmpty()) {
                { CategoryShareCard(result) }
            } else {
                { EmptyPanel("暂无分类数据") }
            }
        }
        StatsPanelType.RANKING_LIST -> {
            if (result.activityStats.isNotEmpty()) {
                { ActivityRankSection(result) }
            } else {
                { EmptyPanel("暂无排行数据") }
            }
        }
        StatsPanelType.BAR_CHART -> {
            if (result.activityStats.isNotEmpty()) {
                { BarChartCard(result) }
            } else {
                { EmptyPanel("暂无柱状图数据") }
            }
        }
        StatsPanelType.COMPARISON -> { { ComparisonCard(result) } }
        else -> { { EmptyPanel("${panel.title}（即将支持）") } }
    }

    if (isEditMode) {
        EditModePanelWrapper(
            panelTitle = PanelTypeLabels[panel.type] ?: panel.title,
            panelIndex = panelIndex,
            totalPanels = totalPanels,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove,
        ) {
            panelContent()
        }
    } else {
        panelContent()
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
private fun EditModePanelWrapper(
    panelTitle: String,
    panelIndex: Int,
    totalPanels: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = panelTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onMoveUp,
                    enabled = panelIndex > 0,
                    modifier = Modifier.padding(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "上移",
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = panelIndex < totalPanels - 1,
                    modifier = Modifier.padding(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "下移",
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.padding(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            content()
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
