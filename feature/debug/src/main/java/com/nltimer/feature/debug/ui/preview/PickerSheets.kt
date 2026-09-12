package com.nltimer.feature.debug.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Tag
import com.nltimer.core.behaviorui.sheet.TagPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerSheet(
    tags: List<Tag>,
    selectedIds: Set<Long>,
    onIdsChanged: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
        ) {
            Text(
                "选择关联标签",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            TagPicker(
                tags = tags,
                selectedTagIds = selectedIds,
                onTagToggle = { id ->
                    onIdsChanged(if (id in selectedIds) selectedIds - id else selectedIds + id)
                },
            )
        }
    }
}


