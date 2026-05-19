package com.nltimer.feature.home.ui.components.moment

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.designsystem.icon.IconRenderer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.designsystem.theme.BorderTokens
import com.nltimer.core.designsystem.theme.LocalTimerTypography
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledBorder
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.ui.components.LiveElapsedDuration
import com.nltimer.feature.home.ui.components.SlideActionPill

private val FOCUS_CARD_HEIGHT = 260.dp

@Composable
internal fun ActiveCard(
    cell: GridCellUiState,
    onComplete: () -> Unit,
    momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
    modifier: Modifier = Modifier,
) {
    val startMs = cell.startEpochMs ?: System.currentTimeMillis()
    val elapsedTime = LiveElapsedDuration(
        startEpochMs = startMs,
        isCurrent = true,
        fallbackDurationMs = 0L,
    )
    val durationText = formatDuration(elapsedTime)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardShape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_FULL))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(FOCUS_CARD_HEIGHT)
            .clip(cardShape)
            .border(
                width = styledBorder(BorderTokens.STANDARD),
                color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .height(FOCUS_CARD_HEIGHT)
                .padding(momentStyle.cardPadding.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconRenderer(
                    iconKey = cell.activityIconKey,
                    defaultEmoji = "📌",
                    iconSize = 32.dp,
                )
                Text(
                    text = cell.activityName ?: "",
                    style = LocalTimerTypography.current.timeStyle.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.height(12.dp))

            SlideActionPill(
                onActivate = onComplete,
                activeLabel = "滑动完成",
                activatedLabel = "释放完成",
                leadingIcon = Icons.Filled.Check,
                activatedIcon = Icons.Filled.Check,
            )

            TagNoteRow(tags = cell.tags, note = cell.note, tagDisplayConfig = tagDisplayConfig)

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = durationText,
                    style = LocalTimerTypography.current.timeStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.8f)),
                )
                Text(
                    text = "正在专注...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.5f)),
                )
            }
        }
    }
}
