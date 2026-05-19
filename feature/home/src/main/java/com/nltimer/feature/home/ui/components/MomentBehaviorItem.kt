package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nltimer.core.designsystem.icon.IconRenderer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.data.util.hhmmFormatter
import com.nltimer.feature.home.model.GridCellUiState

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun MomentBehaviorItem(
    behavior: GridCellUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    logStyle: LogLayoutStyle = LogLayoutStyle(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
) {
    val isActive = behavior.isCurrent && behavior.status == BehaviorNature.ACTIVE
    val isPending = behavior.status == BehaviorNature.PENDING

    val cardBackground = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }
    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isPending -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                IconRenderer(
                    iconKey = behavior.activityIconKey,
                    defaultEmoji = "❓",
                    iconSize = 18.dp,
                )
                Text(
                    text = behavior.activityName ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when {
                isActive -> {
                    Text(
                        text = "进行中",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                isPending -> {
                    Text(
                        text = "目标",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                else -> {
                    val startStr = behavior.startTime?.format(hhmmFormatter) ?: ""
                    val endStr = behavior.endTime?.format(hhmmFormatter) ?: ""
                    if (startStr.isNotEmpty()) {
                        Text(
                            text = if (endStr.isNotEmpty()) "$startStr - $endStr" else startStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (behavior.tags.isNotEmpty() || !behavior.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                behavior.tags.forEach { tag ->
                    TagChip(tag = tag, tagDisplayConfig = tagDisplayConfig)
                }
                if (!behavior.note.isNullOrBlank()) {
                    Text(
                        text = behavior.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }

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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
