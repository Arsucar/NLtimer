package com.nltimer.feature.home.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import java.time.LocalTime

/**
 * 网格视图中单行的 UI 状态。
 * 包含时间信息、锁定状态及该行内的所有单元格。
 */
@Immutable
data class GridRowUiState(
    val rowId: String, // 行唯一标识，用于 LazyList key
    val startTime: LocalTime, // 该行对应的时间起点
    val isCurrentRow: Boolean, // 是否为当前时间所在行
    val isLocked: Boolean, // 该行是否锁定（禁止编辑）
    val cells: PersistentList<GridCellUiState>,
)
