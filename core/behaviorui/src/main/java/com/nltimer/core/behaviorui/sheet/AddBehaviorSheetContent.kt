package com.nltimer.core.behaviorui.sheet

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.SecondsStrategy
import com.nltimer.core.data.model.Tag
import com.nltimer.core.designsystem.component.DragMenuOptions
import com.nltimer.core.designsystem.component.DragMenuOptionsPlacement
import com.nltimer.core.designsystem.component.DraggableMenuAnchor
import com.nltimer.core.tools.match.ApplyNoteDirectivesUseCase
import com.nltimer.core.tools.match.NoteProcessOutcome
import com.nltimer.core.tools.match.NoteScanResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

@Suppress("LongParameterList", "LongMethod", "UNUSED_PARAMETER")
@Composable
internal fun AddBehaviorSheetContent(
    modifier: Modifier = Modifier,
    mode: BehaviorNature = BehaviorNature.COMPLETED,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    dialogConfig: DialogGridConfig,
    initialStartTime: LocalDateTime? = null,
    initialEndTime: LocalDateTime? = null,
    prevEndTime: LocalDateTime? = null,
    initialActivityId: Long? = null,
    initialTagIds: List<Long> = emptyList(),
    initialNote: String? = null,
    initialEstimatedDurationMs: Long? = null,
    editBehaviorId: Long? = null,
    existingBehaviors: List<Behavior> = emptyList(),
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onConfirm: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onDismiss: () -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onAddActivity: (name: String, iconKey: String?, color: Long?, groupId: Long?, keywords: String?, tagIds: List<Long>) -> Unit = { _, _, _, _, _, _ -> },
    onAddTag: (name: String, color: Long?, icon: String?, priority: Int, category: String?, keywords: String?, activityId: Long?) -> Unit = { _, _, _, _, _, _, _ -> },
    onProcessNote: OnProcessNote = { _, _ -> NoteProcessOutcome.Empty },
    onMatchNote: (String) -> NoteScanResult = { NoteScanResult(null, emptySet()) },
    onQueryTagsForActivity: suspend (Long) -> List<Long> = { emptyList() },
    onQueryActivitiesForTag: suspend (Long) -> List<Long> = { emptyList() },
) {
    // existingBehaviors 仍由公共 Sheet API 透传（保持签名稳定），冲突由 UseCase 校验。
    val state = rememberAddBehaviorState(
        mode = mode,
        initialStartTime = initialStartTime,
        initialEndTime = initialEndTime,
        initialActivityId = initialActivityId,
        initialTagIds = initialTagIds,
        initialNote = initialNote,
        editBehaviorId = editBehaviorId,
        dialogConfig = dialogConfig,
        initialEstimatedDurationMs = initialEstimatedDurationMs,
    )

    EndTimeAutoTickEffect(state)

    val emphasisColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                val position = it.positionInWindow()
                state.boxPositionInWindow = position
                state.dragMenuState.containerPositionInWindow = position
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BehaviorSheetBackground(
                pathDrawMode = state.effectiveMode,
                emphasisColor = emphasisColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    SheetMainContent(
                        state = state,
                        mode = mode,
                        activities = activities,
                        allTags = allTags,
                        dialogConfig = dialogConfig,
                        emphasisColor = emphasisColor,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss,
                        onProcessNote = onProcessNote,
                        onMatchNote = onMatchNote,
                        onQueryTagsForActivity = onQueryTagsForActivity,
                        onQueryActivitiesForTag = onQueryActivitiesForTag,
                    )
                }
            }
        }

        if (state.showTimeAdjustments && mode != BehaviorNature.PENDING) {
            TimeAdjustmentOverlay(
                mode = mode,
                startTime = state.startTime,
                endTime = state.endTime,
                innerBoxPositionInWindow = state.innerBoxPositionInWindow,
                boxPositionInWindow = state.boxPositionInWindow,
                onStartTimeChanged = { state.startTime = it },
                onEndTimeChanged = { state.endTime = it },
                prevEndTime = prevEndTime,
                onUserAdjustedStart = { state.markUserAdjustedStart() },
                onUserAdjustedEnd = { state.markUserAdjustedEnd() },
            )
        }

        if (state.dragMenuState.isDragging) {
            DragOptionsOverlay(state = state)
        }
    }

    SheetPickerDialogs(
        showAddActivityDialog = state.showAddActivityDialog,
        showAddTagDialog = state.showAddTagDialog,
        showActivityPicker = state.showActivityPicker,
        showTagPicker = state.showTagPicker,
        activities = activities,
        activityGroups = activityGroups,
        allTags = allTags,
        selectedActivityId = state.selectedActivityId,
        selectedTagIds = state.selectedTagIds,
        activityLastUsedMap = activityLastUsedMap,
        tagLastUsedMap = tagLastUsedMap,
        tagCategoryOrder = tagCategoryOrder,
        onAddActivityDialogDismiss = { state.showAddActivityDialog = false },
        onAddTagDialogDismiss = { state.showAddTagDialog = false },
        onActivityPickerDismiss = { state.showActivityPicker = false },
        onTagPickerDismiss = { state.showTagPicker = false },
        onActivitySelected = { state.selectedActivityId = it },
        onTagsSelected = { state.selectedTagIds = it },
        onActivityGroupsReordered = onActivityGroupsReordered,
        onTagCategoriesReordered = onTagCategoriesReordered,
        onAddActivity = onAddActivity,
        onAddTag = onAddTag,
        onShowAddActivityDialog = { state.showAddActivityDialog = true },
        onShowAddTagDialog = { state.showAddTagDialog = true },
    )
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun SheetMainContent(
    state: AddBehaviorState,
    mode: BehaviorNature,
    activities: List<Activity>,
    allTags: List<Tag>,
    dialogConfig: DialogGridConfig,
    emphasisColor: Color,
    onConfirm: (Long, List<Long>, LocalDateTime, LocalDateTime?, BehaviorNature, String?, Long?) -> Unit,
    onDismiss: () -> Unit,
    onProcessNote: OnProcessNote,
    onMatchNote: (String) -> NoteScanResult,
    onQueryTagsForActivity: suspend (Long) -> List<Long>,
    onQueryActivitiesForTag: suspend (Long) -> List<Long>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activityChips = remember(activities) { activities.map { ChipItem(it) } }
    val tagChips = remember(allTags) { allTags.map { ChipItem(it) } }
    val horizontalLinesForActivities = remember(dialogConfig.activityHorizontalLines) {
        if (dialogConfig.activityHorizontalLines == 0) Int.MAX_VALUE else dialogConfig.activityHorizontalLines
    }
    val horizontalLinesForTags = remember(dialogConfig.tagHorizontalLines) {
        if (dialogConfig.tagHorizontalLines == 0) Int.MAX_VALUE else dialogConfig.tagHorizontalLines
    }
    // 竖向布局下列数会作为 StaggeredHorizontalGrid 的 maxLines（IntArray(maxLines)），
    // 0 会触发数组越界；设置页最小值为 1，但防御性钳制避免历史脏数据崩溃。
    val columnLinesForActivities = remember(dialogConfig.activityColumnLines) {
        dialogConfig.activityColumnLines.coerceAtLeast(1)
    }
    val columnLinesForTags = remember(dialogConfig.tagColumnLines) {
        dialogConfig.tagColumnLines.coerceAtLeast(1)
    }

    val blurRadius by animateDpAsState(
        targetValue = if (state.showTimeAdjustments) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "blur_animation"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (state.showTimeAdjustments) 0.7f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha_animation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
            .then(
                if (state.showTimeAdjustments) {
                    Modifier.pointerInput(state.showTimeAdjustments) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (event.type == PointerEventType.Press) {
                                    state.showTimeAdjustments = false
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        if (mode == BehaviorNature.COMPLETED) {
            DurationDisplayRow(duration = state.duration, emphasisColor = emphasisColor)

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { state.innerBoxPositionInWindow = it.positionInWindow() }
        ) {
            Column {
                TimePickerSection(
                    mode = mode,
                    state = state,
                    emphasisColor = emphasisColor,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .blur(blurRadius)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Spacer(modifier = Modifier.height(1.dp))
            ActivityGridComponent(
                chips = activityChips,
                onChipClick = { id ->
                    state.selectedActivityId = id
                    scope.launch {
                        val tagIds = onQueryTagsForActivity(id)
                        if (tagIds.isNotEmpty()) {
                            state.selectedTagIds = state.selectedTagIds + tagIds.toSet()
                        }
                    }
                },
                selectedId = state.selectedActivityId,
                displayMode = dialogConfig.activityDisplayMode,
                layoutMode = dialogConfig.activityLayoutMode,
                maxLinesPerColumn = columnLinesForActivities,
                maxLinesHorizontal = horizontalLinesForActivities,
                useActivityColorForText = dialogConfig.activityUseColorForText,
                functionChipLabel = "活动",
                functionChipIcon = {
                    Icon(Icons.Default.Settings, contentDescription = "活动", modifier = Modifier.size(14.dp))
                },
                functionChipOnClick = { state.showActivityPicker = true },
                functionChipOnLongClick = { state.showAddActivityDialog = true },
            )
            Spacer(modifier = Modifier.height(10.dp))
            ActivityGridComponent(
                chips = tagChips,
                onChipClick = { tagId ->
                    val wasSelected = tagId in state.selectedTagIds
                    state.selectedTagIds = if (wasSelected) {
                        state.selectedTagIds - tagId
                    } else {
                        state.selectedTagIds + tagId
                    }
                    if (!wasSelected) {
                        scope.launch {
                            val activityIds = onQueryActivitiesForTag(tagId)
                            if (activityIds.isNotEmpty()) {
                                state.selectedActivityId = activityIds.first()
                            }
                        }
                    }
                },
                selectedIds = state.selectedTagIds,
                multiSelect = true,
                displayMode = dialogConfig.tagDisplayMode,
                layoutMode = dialogConfig.tagLayoutMode,
                maxLinesPerColumn = columnLinesForTags,
                maxLinesHorizontal = horizontalLinesForTags,
                useActivityColorForText = dialogConfig.tagUseColorForText,
                functionChipLabel = "标签",
                functionChipIcon = {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "标签", modifier = Modifier.size(14.dp))
                },
                functionChipOnClick = { state.showTagPicker = true },
                functionChipOnLongClick = { state.showAddTagDialog = true },
            )
            Spacer(modifier = Modifier.height(10.dp))

            NoteAutoMatchEffect(
                enabled = dialogConfig.autoMatchNote,
                note = state.note,
                onMatchNote = onMatchNote,
                onApplyScan = { result ->
                    state.applyNoteScan(result)
                    scope.launch {
                        applyLinkage(state, result.activityId, result.tagIds, onQueryTagsForActivity, onQueryActivitiesForTag)
                    }
                },
            )

            NoteInputComponent(
                note = state.note,
                onNoteChange = { state.note = it },
                onTopButton = {
                    val raw = state.note
                    if (raw.isBlank()) {
                        Toast.makeText(context, "请输入备注后再识别", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            val processed = onProcessNote(raw, state.selectedActivityId)
                            state.note = processed.cleanedNote
                            val directiveApply = state.applyDirectiveOutcome(processed.directiveOutcome)
                            val scanResult = processed.scanResult
                            val scanApply = state.applyNoteScan(scanResult)
                            // linkage 活动源取 directive/scan 合并后的最终选中值：
                            // directive 的 @活动 会覆盖选中，若仍用 scanResult.activityId
                            // 会漏掉新建/复用活动的绑定标签联动。
                            applyLinkage(
                                state,
                                state.selectedActivityId,
                                scanResult.tagIds,
                                onQueryTagsForActivity,
                                onQueryActivitiesForTag,
                            )
                            Toast.makeText(
                                context,
                                buildFeedbackMessage(processed.directiveOutcome, directiveApply, scanApply),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
                onBottomButton = { },
            )

            ConfirmButtonRow(
                state = state,
                mode = mode,
                secondsStrategy = dialogConfig.secondsStrategy,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DurationDisplayRow(
    duration: Duration,
    emphasisColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val durationText = "${hours}时${minutes}分"
        Text(
            text = "用时：$durationText",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = emphasisColor,
            ),
        )
    }
}

@Composable
private fun TimePickerSection(
    mode: BehaviorNature,
    state: AddBehaviorState,
    emphasisColor: Color = MaterialTheme.colorScheme.secondary,
) {
    when (mode) {
        BehaviorNature.COMPLETED -> {
            DualTimePicker(
                startTime = state.startTime,
                endTime = state.endTime,
                animate = !state.showTimeAdjustments,
                onTimesChanged = { start, end ->
                    if (hasMinuteLevelChange(state.startTime, start)) {
                        state.startTime = start
                        state.markUserAdjustedStart()
                    }
                    if (hasMinuteLevelChange(state.endTime, end)) {
                        state.endTime = end
                        state.markUserAdjustedEnd()
                    }
                },
                onLeftCenterClick = { state.showTimeAdjustments = !state.showTimeAdjustments },
                onRightCenterClick = { state.showTimeAdjustments = !state.showTimeAdjustments },
            )
        }
        BehaviorNature.ACTIVE -> {
            SingleTimePicker(
                startTime = state.startTime,
                animate = !state.showTimeAdjustments,
                onTimeChanged = { state.startTime = it; state.markUserAdjustedStart() },
                onCenterClick = { state.showTimeAdjustments = !state.showTimeAdjustments },
            )
        }
        BehaviorNature.PENDING -> {
            DurationPicker(
                durationMs = state.estimatedDurationMs,
                onDurationChanged = { state.estimatedDurationMs = it },
                emphasisColor = emphasisColor,
                animate = !state.showTimeAdjustments,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ConfirmButtonRow(
    state: AddBehaviorState,
    mode: BehaviorNature,
    secondsStrategy: SecondsStrategy,
    onConfirm: (Long, List<Long>, LocalDateTime, LocalDateTime?, BehaviorNature, String?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableMenuAnchor(
            state = state.dragMenuState,
            modifier = Modifier
                .weight(2f)
                .height(40.dp),
            constrainToScreen = false,
            onOptionSelected = { option ->
                Toast.makeText(context, "触发功能: $option", Toast.LENGTH_SHORT).show()
            },
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text("Gesture", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Button(
            onClick = {
                if (mode == BehaviorNature.COMPLETED
                    && !state.startTime.isBefore(state.endTime)
                ) {
                    Toast.makeText(context, "开始时间必须早于结束时间", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // 与 AddBehaviorUseCase.validateTimeConstraints 对齐：ACTIVE 开始时间不可晚于现在
                if (mode == BehaviorNature.ACTIVE
                    && state.startTime.isAfter(LocalDateTime.now())
                ) {
                    Toast.makeText(context, "开始时间不能大于当前时间", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val confirmTime = LocalDateTime.now()
                val resolvedStartTime = state.resolveStartTime(secondsStrategy, confirmTime)
                val resolvedEndTime = state.resolveEndTime()
                state.selectedActivityId?.let { activityId ->
                    onConfirm(
                        activityId,
                        state.selectedTagIds.toList(),
                        resolvedStartTime,
                        resolvedEndTime,
                        mode,
                        state.note.ifBlank { null },
                        state.estimatedDurationMs,
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.weight(1f).height(40.dp),
            enabled = state.selectedActivityId != null,
        ) {
            Text(text = "确认", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun DragOptionsOverlay(state: AddBehaviorState) {
    DragMenuOptions(
        state = state.dragMenuState,
        options = listOf("测试1", "测试2", "测试3", "添加自定义功能"),
        placement = DragMenuOptionsPlacement.AboveAnchorBottom,
    )
}

/**
 * 自动智能识别副作用：开启时监听备注文本，经 [NOTE_AUTO_MATCH_DEBOUNCE_MS] 防抖后
 * 调用 [onMatchNote] 并把结果合并到状态。
 *
 * 静默模式（无 Toast）—— 与手动按钮触发的反馈路径区分：自动触发会随用户敲字高频发生，
 * 弹 Toast 会打扰；手动按钮仍保留原 Toast 反馈作为强制重扫入口。
 *
 * 防抖实现：[LaunchedEffect] 以 `enabled` 与 `note` 为 key，每次 note 变化都会取消旧协程
 * 重启新协程，旧的 [delay] 被取消即等于"输入仍在进行"，达到时间窗口才执行匹配。
 */
@Composable
private fun NoteAutoMatchEffect(
    enabled: Boolean,
    note: String,
    onMatchNote: (String) -> NoteScanResult,
    onApplyScan: (NoteScanResult) -> Unit,
) {
    val currentOnMatch by rememberUpdatedState(onMatchNote)
    val currentOnApply by rememberUpdatedState(onApplyScan)
    LaunchedEffect(enabled, note) {
        if (!enabled || note.isBlank()) return@LaunchedEffect
        delay(NOTE_AUTO_MATCH_DEBOUNCE_MS)
        currentOnApply(currentOnMatch(note))
    }
}

private const val NOTE_AUTO_MATCH_DEBOUNCE_MS = 300L

/**
 * 当 [AddBehaviorState.endTimeAutoTracking] 为 true 时，每秒把 endTime 推进到当前系统时间，
 * 使"用时"读数像秒表一样实时增长。
 *
 * tick 间隔 1 秒：分钟跨越时能及时刷新分钟级展示；秒级变化时由于 Compose
 * `mutableStateOf` 结构等值短路，分/时显示字符串不变即不会触发选择器滚动。
 */
@Composable
private fun EndTimeAutoTickEffect(state: AddBehaviorState) {
    LaunchedEffect(state.endTimeAutoTracking) {
        if (!state.endTimeAutoTracking) return@LaunchedEffect
        while (true) {
            state.endTime = LocalDateTime.now()
            delay(END_TIME_TICK_INTERVAL_MS)
        }
    }
}

private const val END_TIME_TICK_INTERVAL_MS = 1000L

/**
 * 根据备注扫描结果执行标签-活动联动：活动→绑定标签追加，标签→绑定活动（仅当活动未选中时）。
 */
private suspend fun applyLinkage(
    state: AddBehaviorState,
    matchedActivityId: Long?,
    matchedTagIds: Set<Long>,
    onQueryTagsForActivity: suspend (Long) -> List<Long>,
    onQueryActivitiesForTag: suspend (Long) -> List<Long>,
) {
    if (matchedActivityId != null) {
        val tagIds = onQueryTagsForActivity(matchedActivityId)
        if (tagIds.isNotEmpty()) {
            state.selectedTagIds = state.selectedTagIds + tagIds.toSet()
        }
    }
    for (tagId in matchedTagIds) {
        val activityIds = onQueryActivitiesForTag(tagId)
        if (activityIds.isNotEmpty() && state.selectedActivityId == null) {
            state.selectedActivityId = activityIds.first()
        }
    }
}

/**
 * 智能识别按钮按下后的 Toast 文案生成。
 *
 * 优先级：新增 > 命中现有 > 反向扫描有变化 > 完全无识别。
 * 单项时附名字，多项时只报数量。
 */
private fun buildFeedbackMessage(
    directiveOutcome: ApplyNoteDirectivesUseCase.Outcome,
    directiveApply: NoteDirectiveApplyOutcome,
    scanApply: NoteScanApplyOutcome,
): String {
    val createdActivities = directiveOutcome.createdActivityNames
    val createdTags = directiveOutcome.createdTagNames
    if (createdActivities.isNotEmpty() || createdTags.isNotEmpty()) {
        return buildString {
            append("已新增")
            if (createdActivities.isNotEmpty()) {
                if (createdActivities.size == 1) append("活动『${createdActivities.first()}』")
                else append("${createdActivities.size}个活动")
            }
            if (createdActivities.isNotEmpty() && createdTags.isNotEmpty()) append("和")
            if (createdTags.isNotEmpty()) {
                if (createdTags.size == 1) append("标签『${createdTags.first()}』")
                else append("${createdTags.size}个标签")
            }
        }
    }
    val matchedActivities = directiveOutcome.matchedActivityNames
    val matchedTags = directiveOutcome.matchedTagNames
    if (matchedActivities.isNotEmpty() || matchedTags.isNotEmpty()) {
        return buildString {
            append("已识别")
            if (matchedActivities.isNotEmpty()) {
                if (matchedActivities.size == 1) append("活动『${matchedActivities.first()}』")
                else append("${matchedActivities.size}个活动")
            }
            if (matchedActivities.isNotEmpty() && matchedTags.isNotEmpty()) append("和")
            if (matchedTags.isNotEmpty()) {
                if (matchedTags.size == 1) append("标签『${matchedTags.first()}』")
                else append("${matchedTags.size}个标签")
            }
        }
    }
    return when {
        scanApply.hasAnyChange -> buildString {
            append("已识别")
            if (scanApply.activityAdded) append("活动")
            if (scanApply.activityAdded && scanApply.tagsAdded > 0) append("和")
            if (scanApply.tagsAdded > 0) append("${scanApply.tagsAdded}个标签")
        }
        else -> "未识别到活动或标签"
    }
}
