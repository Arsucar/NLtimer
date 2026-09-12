package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Composable
fun TimeAdjustmentCard(
    currentTime: LocalDateTime,
    onTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    maxTime: LocalDateTime? = null,
    prevEndTime: LocalDateTime? = null,
    onUserAdjusted: () -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimeAdjustmentComponent(
                currentTime = currentTime,
                onTimeChanged = onTimeChanged,
                maxTime = maxTime,
                prevEndTime = prevEndTime,
                onUserAdjusted = onUserAdjusted,
            )
        }
    }
}

@Composable
internal fun TimeAdjustmentOverlay(
    mode: BehaviorNature,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    innerBoxPositionInWindow: Offset,
    boxPositionInWindow: Offset,
    onStartTimeChanged: (LocalDateTime) -> Unit,
    onEndTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    prevEndTime: LocalDateTime? = null,
    onUserAdjustedStart: () -> Unit = {},
    onUserAdjustedEnd: () -> Unit = {},
) {
    if (mode == BehaviorNature.PENDING) return

    when (mode) {
        BehaviorNature.COMPLETED -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .offset {
                        IntOffset(
                            0,
                            (innerBoxPositionInWindow.y - boxPositionInWindow.y + 90.dp.toPx()).roundToInt()
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeAdjustmentCard(
                    currentTime = startTime,
                    onTimeChanged = onStartTimeChanged,
                    modifier = Modifier.weight(1f),
                    prevEndTime = prevEndTime,
                    onUserAdjusted = onUserAdjustedStart,
                )
                TimeAdjustmentCard(
                    currentTime = endTime,
                    onTimeChanged = onEndTimeChanged,
                    modifier = Modifier.weight(1f),
                    maxTime = LocalDateTime.now(),
                    prevEndTime = prevEndTime,
                    onUserAdjusted = onUserAdjustedEnd,
                )
            }
        }
        BehaviorNature.ACTIVE -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .offset {
                        IntOffset(
                            0,
                            (innerBoxPositionInWindow.y - boxPositionInWindow.y + 90.dp.toPx()).roundToInt()
                        )
                    },
                horizontalArrangement = Arrangement.Center,
            ) {
                TimeAdjustmentCard(
                    currentTime = startTime,
                    onTimeChanged = onStartTimeChanged,
                    modifier = Modifier.weight(1f),
                    maxTime = LocalDateTime.now(),
                    prevEndTime = prevEndTime,
                    onUserAdjusted = onUserAdjustedStart,
                )
            }
        }
        BehaviorNature.PENDING -> {}
    }
}
