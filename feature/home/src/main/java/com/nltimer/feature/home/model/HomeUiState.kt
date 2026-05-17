package com.nltimer.feature.home.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.BehaviorNature
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime

enum class AddSheetMode(val nature: BehaviorNature) {
    COMPLETED(BehaviorNature.COMPLETED),
    CURRENT(BehaviorNature.ACTIVE),
    TARGET(BehaviorNature.PENDING),
}

@Immutable
data class HomeUiState(
    val items: PersistentList<HomeListItem> = persistentListOf(),
    val gridSections: PersistentList<GridDaySection> = persistentListOf(),
    val momentCells: PersistentList<GridCellUiState> = persistentListOf(),
    val isLoadingMore: Boolean = false,
    val hasReachedEarliest: Boolean = false,
    val addSheetMode: AddSheetMode? = null,
    val selectedTimeHour: Int = 0,
    val isLoading: Boolean = true,
    val isIdleMode: Boolean = false,
    val hasActiveBehavior: Boolean = false,
    val isDetailSheetVisible: Boolean = false,
    val detailBehavior: BehaviorDetailUiState? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val lastBehaviorEndTime: LocalDateTime? = null,
    val idleStartTime: LocalDateTime? = null,
    val idleEndTime: LocalDateTime? = null,
    val editBehaviorId: Long? = null,
    val editInitialActivityId: Long? = null,
    val editInitialTagIds: PersistentList<Long> = persistentListOf(),
    val editInitialNote: String? = null,
)
