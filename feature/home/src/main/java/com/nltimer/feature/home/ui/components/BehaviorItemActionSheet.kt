package com.nltimer.feature.home.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nltimer.feature.home.model.GridCellUiState

@Immutable
data class BehaviorItemAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
)

object BehaviorItemActions {
    const val DETAIL = "detail"
    const val DELETE = "delete"

    val Default: List<BehaviorItemAction> = listOf(
        BehaviorItemAction(
            id = DETAIL,
            label = "详情",
            icon = Icons.Outlined.Info,
        ),
        BehaviorItemAction(
            id = DELETE,
            label = "删除",
            icon = Icons.Outlined.Delete,
            isDestructive = true,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorItemActionSheet(
    cell: GridCellUiState,
    onDismiss: () -> Unit,
    onAction: (BehaviorItemAction) -> Unit,
    actions: List<BehaviorItemAction> = BehaviorItemActions.Default,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = cell.activityName?.takeIf { it.isNotBlank() }
        ?: "行为 #${cell.behaviorId ?: "N/A"}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            actions.forEach { action ->
                val contentColor = if (action.isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                ListItem(
                    headlineContent = {
                        Text(
                            text = action.label,
                            color = contentColor,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = contentColor,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(action) },
                )
            }
        }
    }
}
