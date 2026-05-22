package com.nltimer.feature.management_activities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.behaviorui.sheet.CategoryGroupCard
import com.nltimer.core.behaviorui.sheet.CategorizableItem
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.designsystem.component.BottomBarDragFab
import com.nltimer.core.designsystem.component.EmptyStateView
import com.nltimer.core.designsystem.component.LoadingScreen
import com.nltimer.core.designsystem.component.rememberDragFabState
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.feature.management_activities.model.GroupWithActivities
import com.nltimer.feature.management_activities.viewmodel.ActivityManagementViewModel
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
fun ActivityManagementScreen(
    viewModel: ActivityManagementViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dragFabState = rememberDragFabState()
    val reorderedGroups = remember { mutableStateListOf<GroupWithActivities>() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    val itemLayouts = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val shiftOffsets = remember { mutableStateMapOf<Int, Float>() }
    val allExpanded = uiState.groups.isNotEmpty() &&
        uiState.groups.all { it.group.id in uiState.expandedGroupIds }

    LaunchedEffect(uiState.groups) {
        if (draggedIndex == -1 && reorderedGroups.toList() != uiState.groups) {
            reorderedGroups.clear()
            reorderedGroups.addAll(uiState.groups)
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
            } else if (
                uiState.uncategorizedActivities.isEmpty() &&
                uiState.groups.isEmpty()
            ) {
                EmptyStateView(
                    message = "暂无活动",
                    subtitle = "点击右下角按钮添加活动",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp + LocalImmersiveTopPadding.current, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        val items = remember(uiState.uncategorizedActivities) {
                            uiState.uncategorizedActivities.map { activity ->
                                ManagementActivityItem(activity)
                            }
                        }
                        CategoryGroupCard(
                            index = 0,
                            groupName = "未分类",
                            items = items,
                            collapsed = false,
                            showDragHandle = false,
                            emptyText = "暂无未分类活动",
                            displayColorMode = uiState.displayColorConfig.activityIconColorMode,
                            onItemSelected = { id ->
                                uiState.uncategorizedActivities
                                    .firstOrNull { it.id == id }
                                    ?.let(viewModel::showActivityDetail)
                            },
                            onItemLongClick = { id ->
                                uiState.uncategorizedActivities
                                    .firstOrNull { it.id == id }
                                    ?.let(viewModel::showMoveToGroupDialog)
                            },
                            onAddItem = { viewModel.showAddActivityDialog() },
                        )
                    }

                    itemsIndexed(
                        items = reorderedGroups,
                        key = { _, groupWithActivities -> groupWithActivities.group.id },
                    ) { index, groupWithActivities ->
                        val items = remember(groupWithActivities.activities) {
                            groupWithActivities.activities.map { activity ->
                                ManagementActivityItem(activity)
                            }
                        }
                        CategoryGroupCard(
                            index = index,
                            groupName = groupWithActivities.group.name,
                            items = items,
                            collapsed = !uiState.expandedGroupIds.contains(groupWithActivities.group.id),
                            showDragHandle = true,
                            emptyText = "暂无活动",
                            displayColorMode = uiState.displayColorConfig.activityIconColorMode,
                            isDragging = draggedIndex == index,
                            dragOffsetY = if (draggedIndex == index) dragOffsetY else 0f,
                            shiftOffset = shiftOffsets[index] ?: 0f,
                            onToggleCollapsed = {
                                viewModel.toggleGroupExpand(groupWithActivities.group.id)
                            },
                            headerActions = {
                                ActivityGroupActions(
                                    group = groupWithActivities.group,
                                    onAddActivity = viewModel::showAddActivityToGroupDialog,
                                    onRename = viewModel::showRenameGroupDialog,
                                    onDelete = viewModel::showDeleteGroupDialog,
                                )
                            },
                            onItemSelected = { id ->
                                groupWithActivities.activities
                                    .firstOrNull { it.id == id }
                                    ?.let(viewModel::showActivityDetail)
                            },
                            onItemLongClick = { id ->
                                groupWithActivities.activities
                                    .firstOrNull { it.id == id }
                                    ?.let(viewModel::showMoveToGroupDialog)
                            },
                            onAddItem = {
                                viewModel.showAddActivityToGroupDialog(groupWithActivities.group)
                            },
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
                                if (draggedIndex in reorderedGroups.indices &&
                                    targetIndex in reorderedGroups.indices &&
                                    draggedIndex != targetIndex
                                ) {
                                    val item = reorderedGroups.removeAt(draggedIndex)
                                    reorderedGroups.add(targetIndex.coerceIn(0, reorderedGroups.size), item)
                                    viewModel.reorderGroups(reorderedGroups.map { it.group.id })
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

        BottomBarDragFab(
            state = dragFabState,
            icon = Icons.Default.Add,
            dragOptions = listOf("添加活动", "添加分组"),
            onClick = { viewModel.showAddActivityDialog() },
            onOptionSelected = { option ->
                when (option) {
                    "添加活动" -> viewModel.showAddActivityDialog()
                    "添加分组" -> viewModel.showAddGroupDialog()
                }
            },
        )

        FilledTonalIconButton(
            onClick = { viewModel.setAllGroupsExpanded(!allExpanded) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 80.dp, bottom = 8.dp)
                .size(56.dp),
        ) {
            Icon(
                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                contentDescription = if (allExpanded) "一键收纳" else "一键展开",
            )
        }

        ActivityManagementSheetRouter(
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

private data class ManagementActivityItem(
    val activity: Activity,
) : CategorizableItem {
    override val itemId: Long = activity.id
    override val itemName: String = activity.name
    override val category: String? = null
    override val usageCount: Int = activity.usageCount
    override val lastUsedTimestamp: Long? = null
    override val iconKey: String? = activity.iconKey
    override val color: Long? = activity.color
}

@Composable
private fun ActivityGroupActions(
    group: ActivityGroup,
    onAddActivity: (ActivityGroup) -> Unit,
    onRename: (ActivityGroup) -> Unit,
    onDelete: (ActivityGroup) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("添加活动") },
                onClick = {
                    expanded = false
                    onAddActivity(group)
                },
            )
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = {
                    expanded = false
                    onRename(group)
                },
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete(group)
                },
            )
        }
    }
}
