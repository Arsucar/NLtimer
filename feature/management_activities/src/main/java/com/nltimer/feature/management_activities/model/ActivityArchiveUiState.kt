package com.nltimer.feature.management_activities.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ArchiveGroup

@Immutable
data class ArchivedActivityItem(
    val activity: Activity,
    val groupName: String,
)

@Immutable
data class ActivityArchiveUiState(
    val groups: List<ArchiveGroup<ArchivedActivityItem>> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
