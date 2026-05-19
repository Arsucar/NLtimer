package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.GridCellUiState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun BehaviorLogCard(
    behavior: GridCellUiState,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    logStyle: LogLayoutStyle = LogLayoutStyle(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
) {
    val cardBackground = if (behavior.isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = styledAlpha(0.15f))
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = styledAlpha(0.2f))
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = styledAlpha(0.5f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .behaviorCardStyle(cardBackground, borderColor, logStyle)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(logStyle.iconSpacing.dp),
                modifier = Modifier.weight(1f),
            ) {
                IconRenderer(
                    iconKey = behavior.activityIconKey,
                    defaultEmoji = "❓",
                    iconSize = logStyle.iconSize.dp,
                )
                Text(
                    text = behavior.activityName ?: "未知",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            behavior.status?.let { status ->
                val (statusLabel, bgColor, textColor) = when (status) {
                    BehaviorNature.ACTIVE ->
                        Triple("进行中", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    BehaviorNature.COMPLETED ->
                        Triple("已完成", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                    BehaviorNature.PENDING ->
                        Triple("目标", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL)))
                        .background(bgColor)
                        .padding(horizontal = logStyle.statusBadgePaddingH.dp, vertical = logStyle.statusBadgePaddingV.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val startText = behavior.startTime?.format(timeFormatter) ?: "--:--"
            val endText = behavior.endTime?.format(timeFormatter) ?: "进行中"
            Text(
                text = "起始: $startText → 结束: $endText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val duration = if (behavior.isCurrent && behavior.startEpochMs != null) {
                LiveElapsedDuration(
                    startEpochMs = behavior.startEpochMs,
                    isCurrent = true,
                    fallbackDurationMs = behavior.durationMs ?: (behavior.actualDuration ?: 0L),
                )
            } else {
                behavior.durationMs ?: (behavior.actualDuration ?: 0L)
            }
            if (duration > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "用时: ${formatDuration(duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        BehaviorTagRow(behavior.tags, logStyle = logStyle, tagDisplayConfig = tagDisplayConfig)

        behavior.note?.let { note ->
            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "备注: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        val details = buildList {
            if (behavior.pomodoroCount > 0) add("番茄钟: ${behavior.pomodoroCount}")
            behavior.estimatedDuration?.let { add("预估: ${formatDuration(it)}") }
            behavior.actualDuration?.let { add("实际: ${formatDuration(it)}") }
            behavior.achievementLevel?.let { add("完成度: $it") }
            add("计划内: ${if (behavior.wasPlanned) "是" else "否"}")
        }
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString("    "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = styledAlpha(0.8f))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "behaviorId: ${behavior.behaviorId ?: "-"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = styledAlpha(0.5f))
        )
    }
}
