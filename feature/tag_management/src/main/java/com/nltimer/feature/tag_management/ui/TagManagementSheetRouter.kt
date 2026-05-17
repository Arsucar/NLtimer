package com.nltimer.feature.tag_management.ui

import androidx.compose.runtime.Composable
import com.nltimer.feature.tag_management.model.DialogState
import com.nltimer.feature.tag_management.model.TagManagementUiState
import com.nltimer.feature.tag_management.ui.components.dialogs.AddCategoryDialog
import com.nltimer.feature.tag_management.ui.components.dialogs.AddTagFormSheet
import com.nltimer.core.designsystem.component.ConfirmDialog
import com.nltimer.feature.tag_management.ui.components.dialogs.EditTagFormSheet
import com.nltimer.feature.tag_management.ui.components.dialogs.RenameCategoryDialog
import com.nltimer.feature.tag_management.viewmodel.TagManagementViewModel

@Composable
fun TagManagementSheetRouter(
    uiState: TagManagementUiState,
    viewModel: TagManagementViewModel,
) {
    uiState.dialogState?.let { dialog ->
        when (dialog) {
            is DialogState.AddTag -> {
                AddTagFormSheet(
                    initialCategory = dialog.category,
                    categories = uiState.categoryNames,
                    allActivities = uiState.allActivities,
                    activityGroups = uiState.activityGroups,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { name, color, icon, priority, category, keywords, activityId ->
                        viewModel.addTag(name, color, icon, priority, category, keywords, activityId)
                    },
                )
            }
            is DialogState.EditTag -> {
                EditTagFormSheet(
                    tag = dialog.tag,
                    categories = uiState.categoryNames,
                    allActivities = uiState.allActivities,
                    activityGroups = uiState.activityGroups,
                    initialActivityId = dialog.activityId,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { tag, activityId ->
                        viewModel.updateTag(tag, activityId)
                    },
                    onDelete = { viewModel.showDeleteTagDialog(dialog.tag) },
                )
            }
            is DialogState.DeleteTag -> {
                ConfirmDialog(
                    title = "删除标签",
                    message = "确定要删除标签「${dialog.tag.name}」吗？此操作不可撤销。",
                    confirmText = "删除",
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { viewModel.deleteTag(dialog.tag) },
                )
            }
            is DialogState.MoveTag -> {
                MoveTagDialogWrapper(
                    tag = dialog.tag,
                    currentCategory = dialog.currentCategory,
                    categories = uiState.categories.map { it.categoryName },
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { targetCategory ->
                        viewModel.moveTagToCategory(dialog.tag.id, targetCategory)
                    },
                )
            }
            is DialogState.AddCategory -> {
                AddCategoryDialog(
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { viewModel.addCategory(it) },
                )
            }
            is DialogState.RenameCategory -> {
                RenameCategoryDialog(
                    currentName = dialog.name,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { viewModel.renameCategory(dialog.name, it) },
                )
            }
            is DialogState.DeleteCategory -> {
                ConfirmDialog(
                    title = "删除分类",
                    message = "删除「${dialog.name}」分类？该分类下的 ${dialog.tagCount} 个标签将变为未分类。",
                    confirmText = "删除",
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { viewModel.deleteCategory(dialog.name) },
                )
            }
        }
    }
}
