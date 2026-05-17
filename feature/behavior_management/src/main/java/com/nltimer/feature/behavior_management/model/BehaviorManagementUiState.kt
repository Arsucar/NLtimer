package com.nltimer.feature.behavior_management.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.feature.behavior_management.export.BehaviorExportData
import java.time.LocalDate

data class BehaviorManagementUiState(
    val timeRange: TimeRangePreset = TimeRangePreset.ONE_DAY,
    val rangeStartDate: LocalDate = LocalDate.now(),
    val rangeStartHour: Int = 0,
    val selectedActivityGroup: String? = null,
    val selectedTagCategory: String? = null,
    val selectedStatus: BehaviorNature? = null,
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.LIST,
    val behaviors: ImmutableList<BehaviorWithDetails> = persistentListOf(),
    val isImporting: Boolean = false,
    val importPreview: ImportPreview? = null,
    val importData: BehaviorExportData? = null,
    val selectedBehaviorIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val editBehaviorId: Long? = null,
)

enum class ViewMode { LIST, TIMELINE }
