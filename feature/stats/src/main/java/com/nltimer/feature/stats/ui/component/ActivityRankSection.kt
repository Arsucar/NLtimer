package com.nltimer.feature.stats.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.data.model.StatsResult
import com.nltimer.feature.stats.ui.CardInnerPadding

@Composable
internal fun ActivityRankSection(result: StatsResult) {
    val activities = result.activityStats.sortedByDescending { it.durationMinutes }
    val maxDuration = activities.firstOrNull()?.durationMinutes?.toFloat() ?: 1f

    PressableStatsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInnerPadding),
        ) {
            Text(
                text = "活动排行",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            activities.forEachIndexed { index, stat ->
                ActivityRankItem(
                    rank = index + 1,
                    name = stat.activityName,
                    emoji = stat.iconKey ?: "",
                    count = stat.count,
                    durationMinutes = stat.durationMinutes,
                    maxDuration = maxDuration,
                    color = CategoryColors[index % CategoryColors.size],
                )
                if (index < activities.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun ActivityRankItem(
    rank: Int,
    name: String,
    emoji: String,
    count: Int,
    durationMinutes: Int,
    maxDuration: Float,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = color.copy(alpha = 0.15f))
            }
            Text(
                text = if (emoji.isNotBlank()) emoji else "${(rank)}",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format("%.1fh", durationMinutes / 60f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { durationMinutes.toFloat() / maxDuration },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${count}次",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
