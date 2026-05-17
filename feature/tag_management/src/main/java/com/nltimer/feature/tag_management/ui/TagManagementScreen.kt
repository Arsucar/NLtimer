package com.nltimer.feature.tag_management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.behaviorui.sheet.CategoryGroupCard
import com.nltimer.core.behaviorui.sheet.CategorizableItem
import com.nltimer.core.data.model.DisplayColorMode
import com.nltimer.core.data.model.Tag
import com.nltimer.core.designsystem.component.BottomBarDragFab
import com.nltimer.core.designsystem.component.LocalNavBarWidth
import com.nltimer.core.designsystem.component.LoadingScreen
import com.nltimer.core.designsystem.component.rememberDragFabState
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.feature.tag_management.model.CategoryWithTags
import com.nltimer.feature.tag_management.viewmodel.TagManagementViewModel
import kotlin.math.abs

private fun computeDragTargetIndex(
    itemLayouts: Map<Int, Pair<Float, Float>>,
    source: Int,
    offsetY: Float,
): Int {
    val sourceInfo = itemLayouts[source] ?: return source
    val sourceCenter = sourceInfo.first + sourceInfo.second / 2f + offsetY
    return itemLayouts
        .filterKeys { it != source }
        .minByOrNull { (_, info) -> abs(sourceCenter - (info.first + info.second / 2f)) }
        ?.key
        ?.takeIf { index ->
            val info = itemLayouts[index] ?: return@takeIf false
            abs(sourceCenter - (info.first + info.second / 2f)) < (sourceInfo.second + info.second) / 2f
        }
        ?: source
}

private fun applyDragShiftOffsets(
    itemLayouts: Map<Int, Pair<Float, Float>>,
    shiftOffsets: MutableMap<Int, Float>,
    source: Int,
    target: Int,
) {
    shiftOffsets.clear()
    if (source == target) return
    val sourceHeight = itemLayouts[source]?.second ?: return
    val shift = sourceHeight + 8f
    if (target > source) {
        for (index in (source + 1)..target) shiftOffsets[index] = -shift
    } else {
        for (index in target until source) shiftOffsets[index] = shift
    }
}

@Composable
fun TagManagementScreen(
    viewModel: TagManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dragFabState = rememberDragFabState()
    val reorderedCategories = remember { mutableStateListOf<CategoryWithTags>() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    val itemLayouts = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val shiftOffsets = remember { mutableStateMapOf<Int, Float>() }
    val allExpanded = uiState.categories.isNotEmpty() &&
        uiState.categories.all { it.categoryName in uiState.expandedCategoryNames }
    val isCenterFab = LocalTheme.current.bottomBarMode == BottomBarMode.CENTER_FAB
    val navBarWidth = LocalNavBarWidth.current.value
    val expandFabStartPadding = if (isCenterFab && navBarWidth > 0.dp) {
        navBarWidth + 92.dp
    } else {
        80.dp
    }

    LaunchedEffect(uiState.categories) {
        if (draggedIndex == -1 && reorderedCategories.toList() != uiState.categories) {
            reorderedCategories.clear()
            reorderedCategories.addAll(uiState.categories)
        }
    }

    val computeTargetIndex: (Int, Float) -> Int = { source, offsetY ->
        computeDragTargetIndex(itemLayouts, source, offsetY)
    }
    val updateShiftOffsets: (Int, Int) -> Unit = { source, target ->
        applyDragShiftOffsets(itemLayouts, shiftOffsets, source, target)
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { dragFabState.boxPositionInWindow = it.positionInWindow() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                LoadingScreen()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp + LocalImmersiveTopPadding.current, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        val items = uiState.uncategorizedTags.map { tag ->
                            ManagementTagItem(tag)
                        }
                        CategoryGroupCard(
                            index = 0,
                            groupName = "默认",
                            items = items,
                            collapsed = false,
                            showDragHandle = false,
                            emptyText = "暂无标签",
                            displayColorMode = uiState.displayColorConfig.tagDisplayColorMode,
                            onItemSelected = { id ->
                                uiState.uncategorizedTags
                                    .firstOrNull { it.id == id }
                                    ?.let(viewModel::showEditTagDialog)
                            },
                            onItemLongClick = { id ->
                                uiState.uncategorizedTags
                                    .firstOrNull { it.id == id }
                                    ?.let { viewModel.showMoveTagDialog(it, null) }
                            },
                            onAddItem = { viewModel.showAddTagDialog(null) },
                        )
                    }

                    itemsIndexed(
                        items = reorderedCategories,
                        key = { _, category -> category.categoryName },
                    ) { index, category ->
                        val items = category.tags.map { tag ->
                            ManagementTagItem(tag)
                        }
                        CategoryGroupCard(
                            index = index,
                            groupName = category.categoryName,
                            items = items,
                            collapsed = category.categoryName !in uiState.expandedCategoryNames,
                            showDragHandle = true,
                            emptyText = "暂无标签",
                            displayColorMode = uiState.displayColorConfig.tagDisplayColorMode,
                            isDragging = draggedIndex == index,
                            dragOffsetY = if (draggedIndex == index) dragOffsetY else 0f,
                            shiftOffset = shiftOffsets[index] ?: 0f,
                            onToggleCollapsed = {
                                viewModel.toggleCategoryExpand(category.categoryName)
                            },
                            headerActions = {
                                TagCategoryActions(
                                    categoryName = category.categoryName,
                                    onRenameCategory = viewModel::showRenameCategoryDialog,
                                    onDeleteCategory = {
                                        viewModel.showDeleteCategoryDialog(
                                            category.categoryName,
                                            category.tags.size,
                                        )
                                    },
                                )
                            },
                            onItemSelected = { id ->
                                category.tags
                                    .firstOrNull { it.id == id }
                                    ?.let(viewModel::showEditTagDialog)
                            },
                            onItemLongClick = { id ->
                                category.tags
                                    .firstOrNull { it.id == id }
                                    ?.let { viewModel.showMoveTagDialog(it, category.categoryName) }
                            },
                            onAddItem = { viewModel.showAddTagDialog(category.categoryName) },
                            onDragStart = {
                                draggedIndex = index
                                targetIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { delta ->
                                dragOffsetY += delta
                                val nextTarget = computeTargetIndex(index, dragOffsetY)
                                if (nextTarget != targetIndex) {
                                    targetIndex = nextTarget
                                    updateShiftOffsets(index, nextTarget)
                                }
                            },
                            onDragEnd = {
                                if (draggedIndex in reorderedCategories.indices &&
                                    targetIndex in reorderedCategories.indices &&
                                    draggedIndex != targetIndex
                                ) {
                                    val item = reorderedCategories.removeAt(draggedIndex)
                                    reorderedCategories.add(targetIndex.coerceIn(0, reorderedCategories.size), item)
                                    viewModel.reorderCategories(reorderedCategories.map { it.categoryName })
                                }
                                draggedIndex = -1
                                targetIndex = -1
                                dragOffsetY = 0f
                                shiftOffsets.clear()
                            },
                            onDragCancel = {
                                draggedIndex = -1
                                targetIndex = -1
                                dragOffsetY = 0f
                                shiftOffsets.clear()
                            },
                            onPositioned = { idx, y, height ->
                                itemLayouts[idx] = y to height
                            },
                        )
                    }
                }
            }
        }

        val tagColorMode = uiState.displayColorConfig.tagDisplayColorMode
        val dragOptions = listOf(
            "添加分类",
            "添加标签",
            when (tagColorMode) {
                DisplayColorMode.BACKGROUND -> "✓ 标签：背景色模式"
                else -> "标签：背景色模式"
            },
            when (tagColorMode) {
                DisplayColorMode.TEXT -> "✓ 标签：文字色模式"
                else -> "标签：文字色模式"
            },
            when (tagColorMode) {
                DisplayColorMode.NORMAL -> "✓ 标签：正常模式"
                else -> "标签：正常模式"
            },
        )

        BottomBarDragFab(
            state = dragFabState,
            icon = Icons.Default.Add,
            dragOptions = dragOptions,
            onClick = { viewModel.showAddCategoryDialog() },
            onOptionSelected = { option ->
                when {
                    option.startsWith("添加分类") -> viewModel.showAddCategoryDialog()
                    option.startsWith("添加标签") -> viewModel.showAddTagDialog(null)
                    option.contains("背景色模式") -> viewModel.updateTagDisplayColorMode(DisplayColorMode.BACKGROUND)
                    option.contains("文字色模式") -> viewModel.updateTagDisplayColorMode(DisplayColorMode.TEXT)
                    option.contains("正常模式") -> viewModel.updateTagDisplayColorMode(DisplayColorMode.NORMAL)
                }
            },
        )

        FilledTonalIconButton(
            onClick = { viewModel.setAllCategoriesExpanded(!allExpanded) },
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

        TagManagementSheetRouter(
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

private data class ManagementTagItem(
    val tag: Tag,
) : CategorizableItem {
    override val itemId: Long = tag.id
    override val itemName: String = tag.name
    override val category: String? = tag.category
    override val usageCount: Int = tag.usageCount
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = tag.iconKey
    override val color: Long? = tag.color
}

@Composable
private fun TagCategoryActions(
    categoryName: String,
    onRenameCategory: (String) -> Unit,
    onDeleteCategory: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("重命名分类") },
                onClick = {
                    expanded = false
                    onRenameCategory(categoryName)
                },
            )
            DropdownMenuItem(
                text = { Text("删除分类", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDeleteCategory()
                },
            )
        }
    }
}
