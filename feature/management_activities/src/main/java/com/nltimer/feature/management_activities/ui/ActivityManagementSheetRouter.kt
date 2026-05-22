package com.nltimer.feature.management_activities.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.feature.management_activities.model.ActivityManagementUiState
import com.nltimer.feature.management_activities.model.DialogState
import com.nltimer.feature.management_activities.ui.components.ActivityDetailSheet
import com.nltimer.feature.management_activities.ui.components.dialogs.AddActivityFormSheet
import com.nltimer.feature.management_activities.ui.components.dialogs.AddGroupDialog
import com.nltimer.core.designsystem.component.ConfirmDialog
import com.nltimer.feature.management_activities.ui.components.dialogs.EditActivityFormSheet
import com.nltimer.feature.management_activities.ui.components.dialogs.MoveToGroupDialog
import com.nltimer.feature.management_activities.viewmodel.ActivityManagementViewModel

@Composable
fun ActivityManagementSheetRouter(
    uiState: ActivityManagementUiState,
    viewModel: ActivityManagementViewModel,
) {
    when (val dialog = uiState.dialogState) {
        is DialogState.AddActivity -> {
            AddActivityFormSheet(
                allGroups = uiState.allGroups,
                allTags = uiState.allTags,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, iconKey, color, groupId, keywords, tagIds ->
                    viewModel.addActivity(name, iconKey, color, groupId, keywords, tagIds)
                },
            )
        }

        is DialogState.AddActivityToGroup -> {
            AddActivityFormSheet(
                allGroups = uiState.allGroups,
                allTags = uiState.allTags,
                initialGroupId = dialog.group.id,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, iconKey, color, groupId, keywords, tagIds ->
                    viewModel.addActivity(name, iconKey, color, groupId, keywords, tagIds)
                },
            )
        }

        is DialogState.EditActivity -> {
            EditActivityFormSheet(
                activity = dialog.activity,
                allGroups = uiState.allGroups,
                allTags = uiState.allTags,
                initialTagIds = dialog.tagIds,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { updatedActivity, tagIds ->
                    viewModel.updateActivity(updatedActivity, tagIds)
                },
                onDelete = { viewModel.showDeleteActivityDialog(dialog.activity) },
            )
        }

        is DialogState.AddGroup -> {
            AddGroupDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name -> viewModel.addGroup(name) },
            )
        }

        is DialogState.RenameGroup -> {
            RenameGroupDialog(
                currentName = dialog.group.name,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName ->
                    viewModel.renameGroup(dialog.group.id, newName)
                },
            )
        }

        is DialogState.DeleteGroup -> {
            ConfirmDialog(
                title = "删除分组",
                message = "确定要删除\u300C${dialog.group.name}\u300D\uFF1F\n该分组下的所有活动将变为未分类状态。",
                confirmText = "删除",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.deleteGroup(dialog.group.id) },
            )
        }

        is DialogState.DeleteActivity -> {
            ConfirmDialog(
                title = "删除活动",
                message = "确定要删除\u300C${dialog.activity.name}\u300D\uFF1F此操作不可撤销。",
                confirmText = "删除",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.deleteActivity(dialog.activity.id) },
            )
        }

        is DialogState.MoveToGroup -> {
            MoveToGroupDialog(
                activity = dialog.activity,
                allGroups = uiState.allGroups,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { targetGroupId ->
                    viewModel.moveActivityToGroup(dialog.activity.id, targetGroupId)
                },
            )
        }

        is DialogState.ActivityDetail -> {
            val stats by viewModel.currentActivityStats.collectAsStateWithLifecycle()
            ActivityDetailSheet(
                activity = dialog.activity,
                stats = stats,
                _allGroups = uiState.allGroups,
                onDismiss = { viewModel.dismissDialog() },
                onEdit = { viewModel.showEditActivityDialog(it) },
                onDelete = { viewModel.showDeleteActivityDialog(dialog.activity) },
            )
        }

        null -> {}
    }
}
