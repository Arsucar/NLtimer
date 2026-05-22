package com.nltimer.feature.categories.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.nltimer.core.behaviorui.sheet.CategorizableItem
import com.nltimer.core.behaviorui.sheet.CategoryGroupCard
import com.nltimer.core.designsystem.component.BottomBarDragFab
import com.nltimer.core.designsystem.component.EmptyStateView
import com.nltimer.core.designsystem.component.LoadingScreen
import com.nltimer.core.designsystem.component.LocalNavBarWidth
import com.nltimer.core.designsystem.component.ConfirmDialog
import com.nltimer.core.designsystem.component.TextInputDialog
import com.nltimer.core.designsystem.component.rememberDragFabState
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.core.designsystem.theme.appOutlinedTextFieldColors
import com.nltimer.feature.categories.model.CategoriesUiState
import com.nltimer.feature.categories.model.DialogState
import com.nltimer.feature.categories.model.SectionType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState,
    renameConflict: String?,
    onShowAddCategory: (SectionType) -> Unit,
    onRenameCategory: (SectionType, String) -> Unit,
    _onDeleteCategory: (SectionType, String) -> Unit,
    onToggleGroupExpand: (Long) -> Unit,
    onSetAllGroupsExpanded: (Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmAdd: (SectionType, String) -> Unit,
    onConfirmRename: (SectionType, String, String) -> Unit,
    onConfirmDelete: (SectionType, String) -> Unit,
    onClearConflict: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragFabState = rememberDragFabState()
    val allExpanded = uiState.groups.isNotEmpty() &&
        uiState.groups.all { it.id in uiState.expandedGroupIds }
    val isCenterFab = LocalTheme.current.bottomBarMode == BottomBarMode.CENTER_FAB
    val navBarWidth = LocalNavBarWidth.current.value
    val expandFabStartPadding = if (isCenterFab && navBarWidth > 0.dp) {
        navBarWidth + 92.dp
    } else {
        80.dp
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { dragFabState.boxPositionInWindow = it.positionInWindow() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                LoadingScreen()
            } else if (uiState.groups.all { it.items.isEmpty() }) {
                EmptyStateView(
                    message = "暂无分类",
                    subtitle = "点击右下角按钮添加分类",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = uiState.groups,
                        key = { _, group -> group.id },
                    ) { _, group ->
                        val items = remember(group.items) {
                            group.items.mapIndexed { index, item ->
                                CategoryCategorizableItem(index, item.name)
                            }
                        }
                        CategoryGroupCard(
                            index = 0,
                            groupName = group.name,
                            items = items,
                            collapsed = group.id !in uiState.expandedGroupIds,
                            showDragHandle = false,
                            showItemIcon = false,
                            emptyText = "暂无分类",
                            onItemSelected = {},
                            onItemLongClick = { id ->
                                val name = group.items.getOrNull(id.toInt())?.name
                                if (name != null) onRenameCategory(group.type, name)
                            },
                            onAddItem = { onShowAddCategory(group.type) },
                            onToggleCollapsed = { onToggleGroupExpand(group.id) },
                        )
                    }
                }
            }
        }

        BottomBarDragFab(
            state = dragFabState,
            icon = Icons.Default.Add,
            dragOptions = listOf("添加活动分类", "添加标签分类"),
            onClick = { onShowAddCategory(SectionType.ACTIVITY) },
            onOptionSelected = { option ->
                when (option) {
                    "添加活动分类" -> onShowAddCategory(SectionType.ACTIVITY)
                    "添加标签分类" -> onShowAddCategory(SectionType.TAG)
                }
            },
        )

        FilledTonalIconButton(
            onClick = { onSetAllGroupsExpanded(!allExpanded) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = expandFabStartPadding, bottom = 8.dp)
                .size(56.dp),
        ) {
            Icon(
                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                contentDescription = if (allExpanded) "一键收纳" else "一键展开",
            )
        }
    }

    uiState.dialogState?.let { dialog ->
        CategoryDialog(
            dialogState = dialog,
            onDismiss = onDismissDialog,
            onConfirmAdd = onConfirmAdd,
            onConfirmRename = onConfirmRename,
            onConfirmDelete = onConfirmDelete,
            renameConflict = renameConflict,
            onClearConflict = onClearConflict,
        )
    }
}

private class CategoryCategorizableItem(
    private val index: Int,
    private val name: String,
) : CategorizableItem {
    override val itemId: Long = index.toLong()
    override val itemName: String = name
    override val category: String? = null
    override val usageCount: Int = 0
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = null
    override val color: Long? = null
}

@Composable
private fun CategoryDialog(
    dialogState: DialogState,
    onDismiss: () -> Unit,
    onConfirmAdd: (SectionType, String) -> Unit,
    onConfirmRename: (SectionType, String, String) -> Unit,
    onConfirmDelete: (SectionType, String) -> Unit,
    renameConflict: String?,
    onClearConflict: () -> Unit,
) {
    when (dialogState) {
        is DialogState.AddCategory -> {
            TextInputDialog(
                title = "新建分类",
                label = "分类名称",
                onConfirm = { name -> onConfirmAdd(dialogState.sectionType, name) },
                onDismiss = onDismiss,
            )
        }

        is DialogState.RenameCategory -> {
            RenameDialog(
                oldName = dialogState.oldName,
                conflict = renameConflict,
                onDismiss = onDismiss,
                onConfirm = { newName ->
                    onConfirmRename(dialogState.sectionType, dialogState.oldName, newName)
                },
                onClearConflict = onClearConflict,
            )
        }

        is DialogState.DeleteCategory -> {
            ConfirmDialog(
                title = "删除分类",
                message = "确定要删除分类「${dialogState.category}」吗？该分类下的所有内容将变为未分类状态。",
                confirmText = "删除",
                onDismiss = onDismiss,
                onConfirm = { onConfirmDelete(dialogState.sectionType, dialogState.category) },
                confirmTextColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun RenameDialog(
    oldName: String,
    conflict: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClearConflict: () -> Unit,
) {
    var newName by remember(oldName) { mutableStateOf(oldName) }
    val isConflict = conflict != null

    AlertDialog(
        onDismissRequest = {
            onClearConflict()
            onDismiss()
        },
        title = { Text("重命名分类") },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        if (isConflict) onClearConflict()
                    },
                    label = { Text("新名称") },
                    singleLine = true,
                    isError = isConflict,
                    supportingText = if (isConflict) {
                        { Text("该分类已存在") }
                    } else null,
                    colors = appOutlinedTextFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName.trim()) },
                enabled = newName.isNotBlank() && !isConflict,
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onClearConflict()
                onDismiss()
            }) {
                Text("取消")
            }
        },
    )
}
