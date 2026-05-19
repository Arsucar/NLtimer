package com.nltimer.feature.behavior_management.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.util.epochToLocalTime
import com.nltimer.core.data.util.formatDurationCompact
import com.nltimer.core.data.util.hhmmFormatter
import com.nltimer.core.designsystem.component.toComposeColor
import com.nltimer.core.designsystem.theme.styledAlpha


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BehaviorTimelineItem(
    behaviorWithDetails: BehaviorWithDetails,
    isLast: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bwd = behaviorWithDetails
    val behavior = bwd.behavior
    val activity = bwd.activity
    val tags = bwd.tags

    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = styledAlpha(0.5f))
    val dotColor = activity.color.toComposeColor()

    val startTime = behavior.startTime.epochToLocalTime().format(hhmmFormatter)
    val endTime = behavior.endTime?.let { it.epochToLocalTime().format(hhmmFormatter) }

    val durationText = behavior.endTime?.let { end ->
        formatDurationCompact(end - behavior.startTime)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 8.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(
                text = startTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .drawBehind {
                    if (!isLast) {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2, size.height),
                            end = Offset(size.width / 2, size.height + 200f),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = "状态指示" }
                    .then(
                        Modifier.drawBehind {
                            drawCircle(color = dotColor)
                        }
                    ),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                durationText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
            val hasSecondRow = tags.isNotEmpty() || behavior.status != BehaviorNature.COMPLETED
            if (hasSecondRow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (tags.isNotEmpty()) {
                        Text(
                            text = tags.joinToString("·") { it.name },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    val statusText = behavior.status.displaySymbol
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
