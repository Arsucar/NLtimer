package com.nltimer.feature.debug.ui.preview

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.detectDragGestures
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import android.graphics.PathMeasure as AndroidPathMeasure
import java.time.Duration
import java.time.LocalDateTime
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode
import com.nltimer.core.behaviorui.sheet.ActivityGridComponent
import com.nltimer.core.behaviorui.sheet.ChipItem
import com.nltimer.core.behaviorui.sheet.DualTimePicker
import com.nltimer.core.behaviorui.sheet.NoteInputComponent
import com.nltimer.core.behaviorui.sheet.TimeAdjustmentComponent

data class GridConfig(
    val displayMode: MutableState<ChipDisplayMode>,
    val layoutMode: MutableState<GridLayoutMode>,
    val columnLines: MutableState<Int>,
    val horizontalLines: MutableState<Int>,
    val useColorForText: MutableState<Boolean>,
)

@Preview(showBackground = true)
@Composable
fun ActivityRecordCombinedPreview() {
    var showSheet by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(LocalDateTime.now()) }
    var endTime by remember { mutableStateOf(LocalDateTime.now()) }
    var duration by remember { mutableStateOf(Duration.ZERO) }
    var drawMode by remember { mutableStateOf(PathDrawMode.StartToEnd) }

    val activityConfig = remember {
        GridConfig(
            displayMode = mutableStateOf(ChipDisplayMode.Filled),
            layoutMode = mutableStateOf(GridLayoutMode.Horizontal),
            columnLines = mutableStateOf(2),
            horizontalLines = mutableStateOf(2),
            useColorForText = mutableStateOf(true),
        )
    }
    val tagConfig = remember {
        GridConfig(
            displayMode = mutableStateOf(ChipDisplayMode.Filled),
            layoutMode = mutableStateOf(GridLayoutMode.Horizontal),
            columnLines = mutableStateOf(2),
            horizontalLines = mutableStateOf(2),
            useColorForText = mutableStateOf(true),
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GridConfigBlock(label = "活动", config = activityConfig)
            GridConfigBlock(label = "标签", config = tagConfig)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PathDrawMode.entries.forEach { mode ->
                    Surface(
                        onClick = { drawMode = mode },
                        shape = RoundedCornerShape(6.dp),
                        color = if (mode == drawMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = when (mode) {
                                PathDrawMode.StartToEnd -> "单向"
                                PathDrawMode.BothSidesToMiddle -> "双向"
                                PathDrawMode.Random -> "随机"
                                PathDrawMode.None -> "无"
                                PathDrawMode.WrigglingMaggot -> "蛆动"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (mode == drawMode) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (mode == drawMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    startTime = LocalDateTime.now()
                    endTime = LocalDateTime.now()
                    showSheet = true
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("打开活动记录组合弹窗")
            }
        }
    }

    if (showSheet) {
        ActivityRecordCombinedSheet(
            startTime = startTime,
            endTime = endTime,
            activityConfig = activityConfig,
            tagConfig = tagConfig,
            drawMode = drawMode,
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
private fun GridConfigBlock(
    label: String,
    config: GridConfig,
) {
    var modeExpanded by remember { mutableStateOf(false) }
    var layoutExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$label 配置",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "样式",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Surface(
                            onClick = { modeExpanded = true },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = config.displayMode.value.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = modeExpanded,
                            onDismissRequest = { modeExpanded = false },
                        ) {
                            ChipDisplayMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.name,
                                            fontWeight = if (mode == config.displayMode.value) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        config.displayMode.value = mode
                                        modeExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "布局",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Surface(
                            onClick = { layoutExpanded = true },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = config.layoutMode.value.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = layoutExpanded,
                            onDismissRequest = { layoutExpanded = false },
                        ) {
                            GridLayoutMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.name,
                                            fontWeight = if (mode == config.layoutMode.value) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        config.layoutMode.value = mode
                                        layoutExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (config.layoutMode.value == GridLayoutMode.Vertical) {
                StepperControl(
                    label = "每列行数",
                    value = config.columnLines,
                    min = 1,
                    max = 10,
                )
            }

            if (config.layoutMode.value == GridLayoutMode.Horizontal) {
                StepperControl(
                    label = "最多行数（0=无限）",
                    value = config.horizontalLines,
                    min = 0,
                    max = 10,
                    infiniteAtMin = true,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            ToggleControl(
                label = "文字使用活动色",
                checked = config.useColorForText.value,
                onCheckedChange = { config.useColorForText.value = it },
            )
        }
    }
}

@Composable
private fun ToggleControl(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = { onCheckedChange(false) },
            shape = RoundedCornerShape(6.dp),
            color = if (!checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "强调色",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (!checked) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (!checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Surface(
            onClick = { onCheckedChange(true) },
            shape = RoundedCornerShape(6.dp),
            color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "活动色",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun StepperControl(
    label: String,
    value: MutableState<Int>,
    min: Int,
    max: Int,
    infiniteAtMin: Boolean = false,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = { if (value.value > min) value.value-- },
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "\u2212",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Text(
            text = if (infiniteAtMin && value.value == min) "\u221E" else "${value.value}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Surface(
            onClick = {
                if (value.value < max) value.value++
                else if (infiniteAtMin) value.value = min
            },
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityRecordCombinedSheet(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    activityConfig: GridConfig,
    tagConfig: GridConfig,
    drawMode: PathDrawMode,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fakeActivities = listOf(
        ChipItem(id = 1, name = "学习", color = Color(0xFF1B5E20)),
        ChipItem(id = 2, name = "读书", color = Color(0xFF43A047)),
        ChipItem(id = 3, name = "英语", color = Color(0xFF66BB6A)),
        ChipItem(id = 4, name = "微积分", color = Color(0xFF757575)),
        ChipItem(id = 5, name = "休息", color = Color(0xFF212121)),
        ChipItem(id = 6, name = "睡觉", color = Color(0xFF00BFA5)),
        ChipItem(id = 7, name = "冥想", color = Color(0xFF006064)),
        ChipItem(id = 8, name = "信息流", color = Color(0xFFB71C1C)),
        ChipItem(id = 9, name = "生活", color = Color(0xFF8D6E63)),
        ChipItem(id = 10, name = "吃饭", color = Color(0xFFD2B48C)),
        ChipItem(id = 11, name = "多巴胺", color = Color(0xFFB8860B)),
        ChipItem(id = 12, name = "运动", color = Color(0xFFFF7043)),
    )
    val sampleTags = listOf(
        ChipItem(id = 101, name = "标签1", color = Color(0xFF1B5E20)),
        ChipItem(id = 102, name = "标签123", color = Color(0xFF43A047)),
        ChipItem(id = 103, name = "标签123456", color = Color(0xFF66BB6A)),
        ChipItem(id = 104, name = "标签123456789", color = Color(0xFF81C784)),
        ChipItem(id = 105, name = "\u231A标", color = Color(0xFF757575)),
    )

    var localStartTime by remember { mutableStateOf(startTime) }
    var localEndTime by remember { mutableStateOf(endTime) }
    var duration by remember { mutableStateOf(Duration.ZERO) }

    val emphasisColor = MaterialTheme.colorScheme.secondary
    var pathLength by remember { mutableFloatStateOf(0f) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1500),
        label = "path_draw_progress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "jumping_transition")
    val jumpProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_line_progress"
    )

    val effectiveMode = remember(drawMode) {
        if (drawMode == PathDrawMode.Random) {
            val candidates = PathDrawMode.entries.filter {
                it != PathDrawMode.Random && it != PathDrawMode.None && it != PathDrawMode.WrigglingMaggot
            }
            candidates.random()
        } else {
            drawMode
        }
    }

    val context = LocalContext.current
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoveredOption by remember { mutableStateOf<String?>(null) }
    val optionsLayoutBounds = remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var boxPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var buttonRowPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var optionsRowHeight by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(effectiveMode) {
        targetProgress = 0f
        targetProgress = 1f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { boxPositionInWindow = it.positionInWindow() }
        ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp)
                    .graphicsLayer { clip = false }
                    .drawWithContent {
                        drawContent()
                        val strokeWidthPx = 3.dp.toPx()
                        val halfStroke = strokeWidthPx / 2
                        val r = 28.dp.toPx()
                        val w = size.width
                        val h = size.height
                        val extendedH = h * 1.2f
                        val path = Path().apply {
                            moveTo(halfStroke, extendedH)
                            lineTo(halfStroke, r)
                            arcTo(
                                rect = Rect(halfStroke, halfStroke, r * 2 - halfStroke, r * 2 - halfStroke),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            lineTo(w - r, halfStroke)
                            arcTo(
                                rect = Rect(w - r * 2 + halfStroke, halfStroke, w - halfStroke, r * 2 - halfStroke),
                                startAngleDegrees = 270f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            lineTo(w - halfStroke, extendedH)
                        }

                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(path, false)
                        val totalLength = pathMeasure.length
                        if (totalLength > 0 && pathLength == 0f) {
                            pathLength = totalLength
                        }

                        val animatedPath = Path()
                        if (effectiveMode != PathDrawMode.None && animatedProgress > 0f) {
                            when (effectiveMode) {
                                PathDrawMode.StartToEnd -> {
                                    val stopDistance = totalLength * animatedProgress
                                    pathMeasure.getSegment(0f, stopDistance, animatedPath)
                                    drawPath(
                                        path = animatedPath,
                                        color = emphasisColor,
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                }
                                PathDrawMode.BothSidesToMiddle -> {
                                    val halfLength = totalLength / 2f
                                    val drawLength = halfLength * animatedProgress
                                    val startSegment = Path()
                                    pathMeasure.getSegment(0f, drawLength, startSegment)
                                    drawPath(
                                        path = startSegment,
                                        color = emphasisColor,
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                    val endSegment = Path()
                                    pathMeasure.getSegment(
                                        totalLength - drawLength,
                                        totalLength,
                                        endSegment
                                    )
                                    drawPath(
                                        path = endSegment,
                                        color = emphasisColor,
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                }
                                PathDrawMode.WrigglingMaggot -> {
                                    val segmentCount = 24
                                    val segmentLengthPx = 12f
                                    val waveCount = 2
                                    val amplitudeRatio = 0.03f

                                    val androidPathMeasure = AndroidPathMeasure().apply {
                                        setPath(path.asAndroidPath(), false)
                                    }
                                    val position = FloatArray(2)
                                    val tangent = FloatArray(2)

                                    for (i in 0 until segmentCount) {
                                        val baseRatio = i.toFloat() / segmentCount
                                        val offset = sin(
                                            (baseRatio * waveCount + jumpProgress) * 2 * PI.toFloat()
                                        ) * amplitudeRatio
                                        val sampleRatio = (baseRatio + offset).coerceIn(0f, 1f)
                                        val distance = sampleRatio * totalLength

                                        androidPathMeasure.getPosTan(distance, position, tangent)

                                        val angle = atan2(tangent[1].toDouble(), tangent[0].toDouble()).toFloat()
                                        val cosA = cos(angle)
                                        val sinA = sin(angle)

                                        val halfLen = segmentLengthPx / 2f
                                        val startX = position[0] - halfLen * cosA
                                        val startY = position[1] - halfLen * sinA
                                        val endX = position[0] + halfLen * cosA
                                        val endY = position[1] + halfLen * sinA

                                        drawLine(
                                            color = emphasisColor,
                                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                                            strokeWidth = 3.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                PathDrawMode.Random, PathDrawMode.None -> {
                                }
                            }
                        }
                    }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "测试文本",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = emphasisColor,
                                ),
                            )
                        }
//                        TimeAdjustmentComponent(
//                            currentTime = localStartTime,
//                            onTimeChanged = { localStartTime = it }
//                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DualTimePicker(
                            startTime = localStartTime,
                            endTime = localEndTime,
                            onDurationChanged = { duration = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
//                        TimeAdjustmentComponent(
//                            currentTime = localEndTime,
//                            onTimeChanged = { localEndTime = it }
//                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ActivityGridComponent(
                            chips = fakeActivities,
                            onChipClick = { },
                            functionChipLabel = "活动",
                            functionChipIcon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "活动",
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            functionChipOnClick = { },
                            displayMode = activityConfig.displayMode.value,
                            layoutMode = activityConfig.layoutMode.value,
                            maxLinesPerColumn = activityConfig.columnLines.value,
                            maxLinesHorizontal = horizontalLines(activityConfig),
                            chipFixedWidth = 80.dp,
                            useActivityColorForText = activityConfig.useColorForText.value,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ActivityGridComponent(
                            chips = sampleTags,
                            onChipClick = { },
                            functionChipLabel = "标签",
                            functionChipIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label,
                                    contentDescription = "标签",
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            functionChipOnClick = { },
                            displayMode = tagConfig.displayMode.value,
                            layoutMode = tagConfig.layoutMode.value,
                            maxLinesPerColumn = tagConfig.columnLines.value,
                            maxLinesHorizontal = horizontalLines(tagConfig),
                            chipFixedWidth = 50.dp,
                            useActivityColorForText = tagConfig.useColorForText.value,
                        )
                        var noteText by remember { mutableStateOf("") }
                        NoteInputComponent(
                            note = noteText,
                            onNoteChange = { noteText = it },
                            onTopButton = { },
                            onBottomButton = { }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { buttonRowPositionInWindow = it.positionInWindow() },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            var buttonPositionInWindow by remember { mutableStateOf(Offset.Zero) }
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(2f)
                                    .height(40.dp)
                                    .onGloballyPositioned { layoutCoordinates ->
                                        buttonPositionInWindow = layoutCoordinates.positionInWindow()
                                    }
                                    .offset {
                                        IntOffset(
                                            dragOffset.x.roundToInt(),
                                            dragOffset.y.roundToInt()
                                        )
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDragging = true
                                                println("Drag started")
                                            },
                                            onDragEnd = {
                                                println("Drag ended at $dragOffset, hovered: $hoveredOption")
                                                if (hoveredOption != null) {
                                                    Toast.makeText(context, "触发功能: $hoveredOption", Toast.LENGTH_SHORT).show()
                                                }
                                                isDragging = false
                                                dragOffset = Offset.Zero
                                                hoveredOption = null
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                dragOffset = Offset.Zero
                                                hoveredOption = null
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                                
                                                // Calculate current pointer position in window coordinates
                                                // change.position is relative to the button's TOP-LEFT before offset
                                                // So we need: buttonPositionInWindow + dragOffset + change.position
                                                val currentPointerPosition = buttonPositionInWindow + dragOffset + change.position
                                                
                                                // Hit detection
                                                val hit = optionsLayoutBounds.value.entries.find { entry ->
                                                    entry.value.contains(currentPointerPosition)
                                                }?.key
                                                if (hit != hoveredOption) {
                                                    hoveredOption = hit
                                                }
                                            }
                                        )
                                    },
                            ) {
                                Text("拖动按钮", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                            ) {
                                Text("确认", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        if (isDragging) {
            val density = LocalDensity.current
            val gapPx = with(density) { 8.dp.toPx() }
            val optionsY = buttonRowPositionInWindow.y - boxPositionInWindow.y - optionsRowHeight - gapPx
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp)
                    .offset { IntOffset(0, optionsY.roundToInt()) }
                    .onGloballyPositioned { coords ->
                        optionsRowHeight = coords.size.height.toFloat()
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf("测试1", "测试2", "测试3", "添加自定义功能")
                val currentBounds = optionsLayoutBounds.value.toMutableMap()
                options.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { layoutCoordinates ->
                                val position = layoutCoordinates.positionInWindow()
                                val size = layoutCoordinates.size
                                currentBounds[option] = Rect(
                                    position.x,
                                    position.y,
                                    position.x + size.width,
                                    position.y + size.height
                                )
                                optionsLayoutBounds.value = currentBounds.toMap()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (hoveredOption == option)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hoveredOption == option)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

private fun horizontalLines(config: GridConfig): Int {
    val v = config.horizontalLines.value
    return if (v == 0) Int.MAX_VALUE else v
}

@Composable
private fun CombinedTimeAdjustment(
    currentTime: LocalDateTime,
    onTimeChanged: (LocalDateTime) -> Unit,
) {
    TimeAdjustmentComponent(
        currentTime = currentTime,
        onTimeChanged = onTimeChanged,
    )
}
