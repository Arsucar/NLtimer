package com.nltimer.feature.stats.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.StatsResult
import com.nltimer.feature.stats.ui.CardInnerPadding

@Composable
internal fun ComparisonCard(result: StatsResult) {
    val totalMinutes = result.totalMinutes
    val estimated = result.totalEstimatedMinutes
    val actual = result.totalActualMinutes

    PressableStatsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInnerPadding),
        ) {
            Text(
                text = "计划 vs 实际",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ComparisonRow(
                label = "预估时长",
                value = String.format("%.1fh", estimated / 60f),
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ComparisonRow(
                label = "实际时长",
                value = String.format("%.1fh", actual / 60f),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ComparisonRow(
                label = "完成率",
                value = result.completionRate,
                color = MaterialTheme.colorScheme.secondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ComparisonRow(
                label = "计划达成",
                value = result.planAdherenceRate ?: "--",
                color = MaterialTheme.colorScheme.errorContainer,
            )

            if (totalMinutes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val diff = actual - estimated
                val diffText = if (diff >= 0) "+${diff}min" else "${diff}min"
                val diffColor = if (diff >= 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
                Text(
                    text = "偏差: $diffText",
                    style = MaterialTheme.typography.labelLarge,
                    color = diffColor,
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}
