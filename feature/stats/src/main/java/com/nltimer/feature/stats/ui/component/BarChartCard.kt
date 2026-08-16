package com.nltimer.feature.stats.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.ActivityStat
import com.nltimer.core.data.model.StatsResult
import com.nltimer.feature.stats.ui.CardInnerPadding

@Composable
internal fun BarChartCard(
    result: StatsResult,
    onBarClick: ((ActivityStat) -> Unit)? = null,
) {
    val activities = result.activityStats.sortedByDescending { it.durationMinutes }
    if (activities.isEmpty()) return

    val names = activities.map { it.activityName }
    val values = activities.map { it.durationMinutes }
    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1)

    var selectedIndex by remember { mutableIntStateOf(-1) }

    PressableStatsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInnerPadding),
        ) {
            Text(
                text = "时长柱状图",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedBarChart(
                labels = names,
                values = values,
                maxValue = maxVal,
                selectedIndex = selectedIndex,
                onBarTap = { index ->
                    selectedIndex = index
                    onBarClick?.invoke(activities[index])
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
    }
}

@Composable
private fun AnimatedBarChart(
    labels: List<String>,
    values: List<Int>,
    maxValue: Int,
    selectedIndex: Int,
    onBarTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        animatedProgress.animateTo(1f, tween(800))
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColors = CategoryColors

    Canvas(
        modifier = modifier.pointerInput(values.size) {
            detectTapGestures { offset ->
                val barCount = values.size
                if (barCount == 0) return@detectTapGestures
                val spacing = 6.dp.toPx()
                val totalBarWidth = (size.width - spacing * (barCount + 1)) / barCount
                val tappedIndex = ((offset.x - spacing) / (totalBarWidth + spacing)).toInt()
                if (tappedIndex in values.indices) {
                    onBarTap(tappedIndex)
                }
            }
        },
    ) {
        val progress = animatedProgress.value
        val barCount = values.size
        val spacing = 6.dp.toPx()
        val totalBarWidth = (size.width - spacing * (barCount + 1)) / barCount
        val chartHeight = size.height - 30.dp.toPx()

        val labelPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 10.dp.toPx()
            setColor(labelColor.hashCode())
            alpha = 180
        }

        for (i in values.indices) {
            val x = spacing + i * (totalBarWidth + spacing)
            val fraction = (values[i].toFloat() / maxValue) * progress
            val barHeight = chartHeight * fraction
            val barTop = chartHeight - barHeight

            val barColor = barColors[i % barColors.size]
            val alpha = if (selectedIndex < 0 || selectedIndex == i) 1f else 0.4f
            drawRoundRect(
                color = barColor.copy(alpha = alpha),
                topLeft = Offset(x, barTop),
                size = Size(totalBarWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            val labelX = x + totalBarWidth / 2f
            labelPaint.alpha = if (selectedIndex < 0 || selectedIndex == i) 180 else 80
            drawContext.canvas.nativeCanvas.drawText(
                labels[i].take(2),
                labelX,
                size.height,
                labelPaint,
            )
        }
    }
}
