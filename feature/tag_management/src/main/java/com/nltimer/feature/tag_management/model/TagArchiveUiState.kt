package com.nltimer.feature.tag_management.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.model.ArchiveGroup
import com.nltimer.core.data.model.Tag

@Immutable
data class TagArchiveUiState(
    val groups: List<ArchiveGroup<Tag>> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
