package com.nltimer.feature.stats.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.data.model.StatsMetricKind
import com.nltimer.core.data.model.StatsResult
import com.nltimer.feature.stats.ui.CardInnerPadding
import com.nltimer.feature.stats.ui.CardSpacing

internal data class MetricData(
    val caption: String,
    val value: String,
    val icon: String,
    val colors: Pair<Color, Color>,
)

internal val MetricColors = listOf(
    Color(0xFFE8DEF8) to Color(0xFF4F378B),
    Color(0xFFD0BCFF) to Color(0xFF6750A4),
    Color(0xFFCCC2DC) to Color(0xFF625B71),
    Color(0xFFEFCAE8) to Color(0xFF7D5260),
    Color(0xFFE0E0E0) to Color(0xFF444444),
    Color(0xFFB3E5FC) to Color(0xFF0277BD),
)

internal val MetricIcons = listOf("\u23F1", "\u2713", "\uD83D\uDCC8", "\uD83D\uDCCB", "\uD83D\uDFE2", "\u23F3")

@Composable
internal fun CoreMetricsGrid(result: StatsResult) {
    val hours = result.totalMinutes / 60f
    val metrics = listOf(
        MetricData("总时长", String.format("%.1fh", hours), MetricIcons[0], MetricColors[0]),
        MetricData("完成数", "${result.completedCount}", MetricIcons[1], MetricColors[1]),
        MetricData("完成率", result.completionRate, MetricIcons[2], MetricColors[2]),
        MetricData("计划达成", result.planAdherenceRate ?: "--", MetricIcons[3], MetricColors[3]),
    )

    Column(verticalArrangement = Arrangement.spacedBy(CardSpacing)) {
        Row(horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
            MetricCard(metrics[0], modifier = Modifier.weight(1f))
            MetricCard(metrics[1], modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
            MetricCard(metrics[2], modifier = Modifier.weight(1f))
            MetricCard(metrics[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun SingleMetricCard(
    result: StatsResult,
    metricKind: StatsMetricKind,
    modifier: Modifier = Modifier,
) {
    val data = resolveMetricData(result, metricKind)
    PressableStatsCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(CardInnerPadding),
        ) {
            Text(
                text = data.icon,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Text(
                    text = data.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = data.colors.second,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun MetricCard(data: MetricData, modifier: Modifier = Modifier) {
    PressableStatsCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .padding(CardInnerPadding),
        ) {
            Text(
                text = data.icon,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Text(
                    text = data.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = data.colors.second,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun resolveMetricData(result: StatsResult, kind: StatsMetricKind): MetricData {
    val hours = result.totalMinutes / 60f
    return when (kind) {
        StatsMetricKind.TOTAL_HOURS -> MetricData("总时长", String.format("%.1fh", hours), MetricIcons[0], MetricColors[0])
        StatsMetricKind.COMPLETED_COUNT -> MetricData("完成数", "${result.completedCount}", MetricIcons[1], MetricColors[1])
        StatsMetricKind.COMPLETION_RATE -> MetricData("完成率", result.completionRate, MetricIcons[2], MetricColors[2])
        StatsMetricKind.PLAN_ADHERENCE -> MetricData("计划达成", result.planAdherenceRate ?: "--", MetricIcons[3], MetricColors[3])
        StatsMetricKind.ACTIVE_COUNT -> MetricData("进行中", "${result.activeCount}", MetricIcons[4], MetricColors[4])
        StatsMetricKind.PENDING_COUNT -> MetricData("待处理", "${result.pendingCount}", MetricIcons[5], MetricColors[5])
    }
}
