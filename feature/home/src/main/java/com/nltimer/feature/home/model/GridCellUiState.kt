package com.nltimer.feature.home.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.BehaviorNature
import kotlinx.collections.immutable.PersistentList
import java.time.LocalDateTime

/**
 * 网格视图中单个单元格的 UI 状态。
 * 表示一次行为记录的显示信息，或空白占位单元格。
 *
 * startTime / endTime 携带完整日期信息（LocalDateTime），用于跨天回填补记弹窗时保留正确日期。
 */
@Immutable
data class GridCellUiState(
    val behaviorId: Long?, // null 表示空白/占位单元格
    val activityIconKey: String?,
    val activityName: String?,
    val tags: PersistentList<TagUiState>,
    val status: BehaviorNature?,
    val isCurrent: Boolean, // 是否为正在进行的活跃行为
    val wasPlanned: Boolean = false, // 是否为预先计划的行为
    val achievementLevel: Int? = null,
    val estimatedDuration: Long? = null,
    val actualDuration: Long? = null,
    val durationMs: Long? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val isAddPlaceholder: Boolean = false,
    val note: String? = null,
    val pomodoroCount: Int = 0,
    val formattedDuration: String = "",
    val platinumStrength: Float = 0f,
)
