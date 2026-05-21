package com.nltimer.feature.stats.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.StatsResult
import com.nltimer.feature.stats.ui.CardInnerPadding
import kotlin.math.min

internal val CategoryColors = listOf(
    Color(0xFF6750A4),
    Color(0xFFE8DEF8),
    Color(0xFF625B71),
    Color(0xFF7D5260),
    Color(0xFFB3261E),
    Color(0xFFD0BCFF),
    Color(0xFF21005D),
    Color(0xFF49454F),
    Color(0xFF0288D1),
    Color(0xFF00C853),
)

@Composable
internal fun CategoryShareCard(result: StatsResult) {
    val activities = result.activityStats
    val totalMinutes = result.totalMinutes.toFloat().coerceAtLeast(1f)

    PressableStatsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "活动占比",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    DonutChart(
                        segments = activities.mapIndexed { index, stat ->
                            val fraction = stat.durationMinutes.toFloat() / totalMinutes
                            CategoryColors[index % CategoryColors.size] to fraction
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", result.totalMinutes / 60f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "小时",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    activities.forEachIndexed { index, stat ->
                        val percentage = String.format(
                            "%.0f%%",
                            stat.durationMinutes.toFloat() / totalMinutes * 100,
                        )
                        val color = CategoryColors[index % CategoryColors.size]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawCircle(color = color)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stat.activityName,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            Text(
                                text = percentage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DonutChart(
    segments: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        animatedProgress.animateTo(1f, tween(800))
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 28.dp.toPx()
        val radius = (min(size.width, size.height) - strokeWidth) / 2f
        val topLeft = Offset(
            (size.width - radius * 2 - strokeWidth) / 2f + strokeWidth / 2f,
            (size.height - radius * 2 - strokeWidth) / 2f + strokeWidth / 2f,
        )
        val arcSize = Size(radius * 2, radius * 2)
        val progress = animatedProgress.value

        var startAngle = -90f
        for ((color, fraction) in segments) {
            val sweep = fraction * 360f * progress
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            startAngle += sweep
        }
    }
}
