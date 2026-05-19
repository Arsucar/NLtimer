package com.nltimer.feature.home.ui.components.moment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.feature.home.model.TagUiState
import com.nltimer.feature.home.ui.components.TagChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagNoteRow(
    tags: List<TagUiState>,
    note: String?,
    tagDisplayConfig: com.nltimer.core.data.model.TagDisplayConfig = com.nltimer.core.data.model.TagDisplayConfig(),
) {
    if (tags.isEmpty() && note.isNullOrBlank()) return
    Spacer(Modifier.height(4.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tags.forEach { tag ->
            TagChip(tag = tag, tagDisplayConfig = tagDisplayConfig)
        }
        if (!note.isNullOrBlank()) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.6f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(120.dp),
            )
        }
    }
}
