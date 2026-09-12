package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.behaviorui.R
import com.nltimer.core.designsystem.theme.styledAlpha
import java.time.LocalDateTime

@Composable
fun TimeAdjustmentComponent(
    currentTime: LocalDateTime,
    onTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    maxTime: LocalDateTime? = null,
    prevEndTime: LocalDateTime? = null,
    onUserAdjusted: () -> Unit = {},
) {
    val prevEndLabel = stringResource(R.string.time_adjustment_prev_end)
    val prevEndCd = stringResource(R.string.time_adjustment_prev_end_cd)
    val nowLabel = stringResource(R.string.time_adjustment_now)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val row1 = listOf(
            TimeAdjButton(
                text = prevEndLabel,
                contentDescription = prevEndCd,
                onClick = {
                    onUserAdjusted()
                    onTimeChanged(prevEndAdjustmentTarget(prevEndTime, LocalDateTime.now()))
                },
            ),
            TimeAdjButton("-1") { onUserAdjusted(); onTimeChanged(currentTime.plusMinutes(-1)) },
            TimeAdjButton("-5") { onUserAdjusted(); onTimeChanged(currentTime.plusMinutes(-5)) },
            TimeAdjButton("-15") { onUserAdjusted(); onTimeChanged(currentTime.plusMinutes(-15)) },
        )
        val row2 = listOf(
            TimeAdjButton(nowLabel) {
                onUserAdjusted()
                onTimeChanged(LocalDateTime.now().withSecond(0).withNano(0))
            },
            TimeAdjButton("+1") {
                onUserAdjusted()
                val newTime = currentTime.plusMinutes(1)
                onTimeChanged(if (maxTime != null && newTime > maxTime) maxTime else newTime)
            },
            TimeAdjButton("+5") {
                onUserAdjusted()
                val newTime = currentTime.plusMinutes(5)
                onTimeChanged(if (maxTime != null && newTime > maxTime) maxTime else newTime)
            },
            TimeAdjButton("+15") {
                onUserAdjusted()
                val newTime = currentTime.plusMinutes(15)
                onTimeChanged(if (maxTime != null && newTime > maxTime) maxTime else newTime)
            },
        )

        listOf(row1, row2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowItems.forEach { item ->
                    TimeButton(
                        text = item.text,
                        onClick = item.onClick,
                        modifier = Modifier.weight(1f),
                        contentDescription = item.contentDescription,
                    )
                }
            }
        }
    }
}

private data class TimeAdjButton(
    val text: String,
    val contentDescription: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun TimeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = styledAlpha(0.8f)))
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            maxLines = 1
        )
    }
}
