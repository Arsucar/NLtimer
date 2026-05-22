package com.nltimer.feature.categories.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.feature.categories.viewmodel.CategoriesViewModel

@Composable
fun CategoriesRoute(
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val renameConflict by viewModel.renameConflict.collectAsStateWithLifecycle()

    CategoriesScreen(
        uiState = uiState,
        renameConflict = renameConflict,
        onShowAddCategory = viewModel::showAddCategoryDialog,
        onRenameCategory = viewModel::showRenameCategoryDialog,
        _onDeleteCategory = viewModel::showDeleteCategoryDialog,
        onToggleGroupExpand = viewModel::toggleGroupExpand,
        onSetAllGroupsExpanded = viewModel::setAllGroupsExpanded,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmAdd = viewModel::confirmAddCategory,
        onConfirmRename = viewModel::confirmRenameCategory,
        onConfirmDelete = viewModel::confirmDeleteCategory,
        onClearConflict = viewModel::clearConflict,
    )
}
