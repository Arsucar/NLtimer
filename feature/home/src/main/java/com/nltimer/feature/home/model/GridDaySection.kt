package com.nltimer.feature.home.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import java.time.LocalDate

@Immutable
data class GridDaySection(
    val date: LocalDate,
    val label: String,
    val rows: PersistentList<GridRowUiState>,
)
