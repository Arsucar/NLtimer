package com.nltimer.feature.stats.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.StatsResult
import com.nltimer.feature.stats.ui.CardInnerPadding

@Composable
internal fun TrendCard(result: StatsResult) {
    val breakdown = result.dailyBreakdown
    val labels = breakdown.map { it.dayOfWeek }
    val values = breakdown.map { it.totalMinutes }
    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1)

    PressableStatsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInnerPadding),
        ) {
            Text(
                text = "每日趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LineBarChart(
                labels = labels,
                values = values,
                maxValue = maxVal,
                barColor = MaterialTheme.colorScheme.primaryContainer,
                lineColor = MaterialTheme.colorScheme.primary,
                dotColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
    }
}

@Composable
internal fun LineBarChart(
    labels: List<String>,
    values: List<Int>,
    maxValue: Int,
    barColor: Color,
    lineColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        animatedProgress.animateTo(1f, tween(800))
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val progress = animatedProgress.value
        val barCount = values.size
        val barSpacing = 8.dp.toPx()
        val totalBarWidth = (size.width - barSpacing * (barCount + 1)) / barCount
        val chartHeight = size.height - 30.dp.toPx()
        val barHeight = chartHeight * 0.55f
        val dotRadius = 4.dp.toPx()

        val barPoints = mutableListOf<Offset>()
        for (i in values.indices) {
            val x = barSpacing + i * (totalBarWidth + barSpacing) + totalBarWidth / 2f
            val fraction = (values[i].toFloat() / maxValue) * progress
            val barTop = chartHeight - barHeight * fraction
            val barBottom = chartHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x - totalBarWidth / 2f, barTop),
                size = Size(totalBarWidth, barBottom - barTop),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            val lineY = chartHeight - chartHeight * 0.7f * fraction
            barPoints.add(Offset(x, lineY))

            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                x,
                size.height,
                android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    color = labelColor.hashCode()
                    alpha = 180
                },
            )
        }

        for (i in 0 until barPoints.size - 1) {
            drawLine(
                color = lineColor,
                start = barPoints[i],
                end = barPoints[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        barPoints.forEach { point ->
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = point,
            )
        }
    }
}
