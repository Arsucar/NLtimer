package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledCorner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 时间侧边栏 Composable。
 * 显示活跃小时和当前小时的刻度列表，支持点击和拖拽选择小时。
 *
 * @param activeHours 有行为记录的小时集合
 * @param currentHour 当前选中的小时
 * @param onHourClick 点击小时回调
 * @param modifier 修饰符
 */
@Composable
fun TimeSideBar(
    activeHours: Set<Int>,
    currentHour: Int,
    onHourClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 合并活跃小时和当前小时，去重并按时间倒序排列：
    // 顶部 = 较大小时（深夜/晚），底部 = 较小小时（早晨），与网格"顶部行=最新时间"的列序对齐。
    val displayedHours = remember(activeHours, currentHour) {
        (activeHours + currentHour).sortedDescending()
    }

    var showBubble by remember { mutableStateOf(false) }
    var bubbleHour by remember { mutableIntStateOf(0) }
    var bubbleY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val bubbleOffsetY by remember(bubbleY) {
        derivedStateOf { with(density) { bubbleY.toDp() } - 24.dp }
    }

    val currentDisplayedHours by rememberUpdatedState(displayedHours)

    // 浮动气泡显示当前拖拽到的小时
    Box(modifier = modifier.width(20.dp)) {
        if (showBubble) {
            Box(
                modifier = Modifier
                    .offset(x = (-52).dp, y = bubbleOffsetY)
                    .requiredWidth(48.dp)
                    .height(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${bubbleHour}:00",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (showBubble) {
                        val index = (bubbleY / size.height * displayedHours.size)
                            .toInt()
                            .coerceIn(0, displayedHours.lastIndex)
                        val hour = displayedHours[index]
                        if (hour != bubbleHour) {
                            bubbleHour = hour
                            onHourClick(hour)
                        }
                    }
                }
                // 垂直拖拽手势：拖动时更新气泡位置并触发 onHourClick
                .pointerInput(activeHours) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            bubbleY = offset.y
                            val index = (offset.y / size.height * currentDisplayedHours.size)
                                .toInt()
                                .coerceIn(0, currentDisplayedHours.lastIndex)
                            bubbleHour = currentDisplayedHours[index]
                            showBubble = true
                            onHourClick(bubbleHour)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            bubbleY = (bubbleY + dragAmount).coerceIn(0f, size.height.toFloat())
                            val index = (bubbleY / size.height * currentDisplayedHours.size)
                                .toInt()
                                .coerceIn(0, currentDisplayedHours.lastIndex)
                            val hour = currentDisplayedHours[index]
                            if (hour != bubbleHour) {
                                bubbleHour = hour
                                onHourClick(hour)
                            }
                        },
                        onDragEnd = { showBubble = false },
                        onDragCancel = { showBubble = false },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // 逐个渲染小时文本，区分颜色：当前、活跃、普通
            displayedHours.forEach { hour ->
                val isActive = hour in activeHours
                val isCurrent = hour == currentHour

                Text(
                    modifier = Modifier.clickable { onHourClick(hour) },
                    text = hour.toString(),
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.tertiary
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = styledAlpha(0.6f))
                    },
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,


                )
            }
        }
    }
}
