package com.nltimer.core.behaviorui.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.SecondsStrategy
import com.nltimer.core.data.util.hasTimeConflict
import com.nltimer.core.designsystem.component.DragMenuState
import com.nltimer.core.designsystem.theme.PathDrawMode
import com.nltimer.core.tools.match.NoteScanResult
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Suppress("LongParameterList")
@Composable
internal fun rememberAddBehaviorState(
    mode: BehaviorNature,
    initialStartTime: LocalDateTime?,
    initialEndTime: LocalDateTime?,
    initialActivityId: Long?,
    initialTagIds: List<Long>,
    initialNote: String?,
    editBehaviorId: Long?,
    existingBehaviors: List<Behavior>,
    dialogConfig: DialogGridConfig,
    initialEstimatedDurationMs: Long? = null,
): AddBehaviorState {
    return remember(
        mode,
        initialStartTime,
        initialEndTime,
        initialActivityId,
        initialTagIds,
        initialNote,
        editBehaviorId,
        existingBehaviors,
        dialogConfig,
    ) {
        AddBehaviorState(
            mode = mode,
            initialStartTime = initialStartTime,
            initialEndTime = initialEndTime,
            initialActivityId = initialActivityId,
            initialTagIds = initialTagIds,
            initialNote = initialNote,
            editBehaviorId = editBehaviorId,
            existingBehaviors = existingBehaviors,
            dialogConfig = dialogConfig,
            initialEstimatedDurationMs = initialEstimatedDurationMs,
        )
    }.also { state ->
        LaunchedEffect(initialActivityId) {
            if (initialActivityId != null) state.selectedActivityId = initialActivityId
        }
    }
}

internal class AddBehaviorState(
    private val mode: BehaviorNature,
    private val initialStartTime: LocalDateTime?,
    private val initialEndTime: LocalDateTime?,
    initialActivityId: Long?,
    initialTagIds: List<Long>,
    initialNote: String?,
    private val editBehaviorId: Long?,
    private val existingBehaviors: List<Behavior>,
    dialogConfig: DialogGridConfig,
    initialEstimatedDurationMs: Long? = null,
) {
    var selectedActivityId by mutableStateOf(initialActivityId)
    var selectedTagIds by mutableStateOf(initialTagIds.toSet())

    val sheetOpenTime: LocalDateTime = LocalDateTime.now()
    private val now = sheetOpenTime
    var userAdjustedTime by mutableStateOf(false)
        private set
    var startTime by mutableStateOf(initialStartTime ?: now)
    var endTime by mutableStateOf(initialEndTime ?: now)

    /**
     * endTime 是否随系统时钟自动推进。
     *
     * 启用条件：当前为完成（COMPLETED）弹窗、调用方未指定结束时间、也不在编辑既有行为
     * —— 这种"现在刚做完"的新建场景下，"用时"应像秒表一样实时增长。
     * 一旦用户手动调整时间，立即关闭以保留用户选择。
     */
    var endTimeAutoTracking by mutableStateOf(
        mode == BehaviorNature.COMPLETED && initialEndTime == null && editBehaviorId == null
    )
        private set

    fun markUserAdjustedTime() {
        userAdjustedTime = true
        endTimeAutoTracking = false
    }

    val duration: Duration by derivedStateOf {
        val d = Duration.between(startTime, endTime)
        if (d.isNegative) Duration.ZERO else d
    }

    var note by mutableStateOf(initialNote ?: "")

    var estimatedDurationMs by mutableStateOf(initialEstimatedDurationMs)

    val hasTimeConflict: Boolean by derivedStateOf {
        if (mode == BehaviorNature.PENDING) return@derivedStateOf false
        val nowEpoch = System.currentTimeMillis()
        val startEpoch = startTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endEpoch = if (mode == BehaviorNature.COMPLETED) {
            endTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } else null
        hasTimeConflict(
            newStart = startEpoch,
            newEnd = endEpoch,
            newStatus = mode,
            existingBehaviors = existingBehaviors,
            currentTime = nowEpoch,
            ignoreBehaviorId = editBehaviorId,
        )
    }

    var showAddActivityDialog by mutableStateOf(false)
    var showAddTagDialog by mutableStateOf(false)
    var showActivityPicker by mutableStateOf(false)
    var showTagPicker by mutableStateOf(false)
    var showTimeAdjustments by mutableStateOf(false)
    val dragMenuState = DragMenuState()

    val effectiveMode: PathDrawMode = if (dialogConfig.pathDrawMode == PathDrawMode.Random) {
        val candidates = PathDrawMode.entries.filter {
            it != PathDrawMode.Random && it != PathDrawMode.None && it != PathDrawMode.WrigglingMaggot
        }
        candidates.random()
    } else {
        dialogConfig.pathDrawMode
    }

    var boxPositionInWindow by mutableStateOf(Offset.Zero)
    var innerBoxPositionInWindow by mutableStateOf(Offset.Zero)

    fun resolveStartTime(strategy: SecondsStrategy, confirmTime: LocalDateTime): LocalDateTime {
        // 补记空闲段会携带毫秒级边界；用户未改时间时必须保留，避免边界重叠。
        if (mode == BehaviorNature.COMPLETED) {
            return if (userAdjustedTime) startTime.withSecond(0).withNano(0) else initialStartTime ?: startTime
        }
        return if (userAdjustedTime) {
            startTime.withSecond(0).withNano(0)
        } else {
            val sourceSeconds = when (strategy) {
                SecondsStrategy.OPEN_TIME -> sheetOpenTime.second
                SecondsStrategy.CONFIRM_TIME -> confirmTime.second
            }
            startTime.withSecond(sourceSeconds).withNano(0)
        }
    }

    fun resolveEndTime(): LocalDateTime? {
        if (mode != BehaviorNature.COMPLETED) return null
        return if (userAdjustedTime) endTime.withSecond(0).withNano(0) else initialEndTime ?: endTime
    }

    /**
     * 把备注扫描结果合并到当前选中状态（不破坏用户已手动选中的项）。
     *
     * 合并规则：
     * - 活动（单选）：仅当 [selectedActivityId] 为 null 且扫描结果有命中时写入；已选活动**不被覆盖**
     * - 标签（多选）：把命中标签 ID 并入现有集合，已有选中保留
     *
     * @return 实际产生的变更摘要，调用方可据此显示反馈（例如 Toast）
     */
    fun applyNoteScan(result: NoteScanResult): NoteScanApplyOutcome {
        val activityAdded = if (selectedActivityId == null && result.activityId != null) {
            selectedActivityId = result.activityId
            true
        } else false

        val newTagIds = result.tagIds - selectedTagIds
        if (newTagIds.isNotEmpty()) {
            selectedTagIds = selectedTagIds + newTagIds
        }

        return NoteScanApplyOutcome(
            activityAdded = activityAdded,
            activityHeld = !activityAdded && result.activityId != null && selectedActivityId != result.activityId,
            tagsAdded = newTagIds.size,
        )
    }

    /**
     * 把 @/# directive 结果合并到选中状态。与 [applyNoteScan] 的差异：
     * 这里 activity 会**覆盖**已选项（@ 表达用户明确意图）；tags 仍 union。
     */
    fun applyDirectiveOutcome(
        outcome: com.nltimer.core.tools.match.ApplyNoteDirectivesUseCase.Outcome,
    ): NoteDirectiveApplyOutcome {
        val activityOverridden = if (outcome.lastActivityId != null) {
            selectedActivityId = outcome.lastActivityId
            true
        } else false
        val before = selectedTagIds.size
        selectedTagIds = selectedTagIds + outcome.addedTagIds
        return NoteDirectiveApplyOutcome(
            activityOverridden = activityOverridden,
            tagsAdded = selectedTagIds.size - before,
        )
    }
}

/**
 * `applyNoteScan` 的副作用摘要，用于上层反馈。
 *
 * @property activityAdded 本次扫描结果是否实际写入了 [AddBehaviorState.selectedActivityId]
 * @property activityHeld 扫描命中活动但因用户已选而保持原状（用于"已保留你的活动选择"提示）
 * @property tagsAdded 本次新增到 [AddBehaviorState.selectedTagIds] 的标签数量（去重后）
 */
internal data class NoteScanApplyOutcome(
    val activityAdded: Boolean,
    val activityHeld: Boolean,
    val tagsAdded: Int,
) {
    val hasAnyChange: Boolean get() = activityAdded || tagsAdded > 0
}

/**
 * `applyDirectiveOutcome` 的副作用摘要，用于上层 Toast 反馈。
 *
 * @property activityOverridden directive 是否覆盖了 selectedActivityId
 * @property tagsAdded 本次新增到选中集合的标签数（去重后）
 */
internal data class NoteDirectiveApplyOutcome(
    val activityOverridden: Boolean,
    val tagsAdded: Int,
)
