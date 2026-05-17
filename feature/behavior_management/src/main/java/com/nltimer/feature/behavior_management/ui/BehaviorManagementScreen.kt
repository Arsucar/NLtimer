package com.nltimer.feature.behavior_management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.behaviorui.sheet.AddBehaviorSheet
import com.nltimer.core.designsystem.component.EmptyStateView
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.util.epochToLocalDate
import com.nltimer.core.data.util.epochToLocalDateTime
import com.nltimer.core.data.util.formatDurationCompactHm
import com.nltimer.feature.behavior_management.model.DuplicateHandling
import com.nltimer.feature.behavior_management.model.ViewMode
import com.nltimer.feature.behavior_management.viewmodel.BehaviorManagementViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorManagementScreen(
    viewModel: BehaviorManagementViewModel,
    onNavigateBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activityGroups by viewModel.activityGroups.collectAsStateWithLifecycle()
    val tagCategories by viewModel.tagCategories.collectAsStateWithLifecycle()
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val activityLastUsedMap by viewModel.activityLastUsedMap.collectAsStateWithLifecycle()
    val tagLastUsedMap by viewModel.tagLastUsedMap.collectAsStateWithLifecycle()
    val tagCategoryOrder by viewModel.tagCategoryOrder.collectAsStateWithLifecycle()

    var selectedHandling by remember { mutableStateOf(DuplicateHandling.SKIP) }

    val editBehavior = remember(uiState.editBehaviorId, uiState.behaviors) {
        uiState.behaviors.find { it.behavior.id == uiState.editBehaviorId }
    }
    val displayItems = remember(uiState.behaviors) {
        buildDisplayItems(uiState.behaviors)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.isMultiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "已选 ${uiState.selectedBehaviorIds.size} 项",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::deleteSelectedBehaviors) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::exitMultiSelect) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "取消",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TimeRangeSelector(
                        currentPreset = uiState.timeRange,
                        currentDate = uiState.rangeStartDate,
                        currentHour = uiState.rangeStartHour,
                        onPresetChange = viewModel::setTimeRange,
                        onDateChange = viewModel::setRangeStartDate,
                        onHourChange = viewModel::setRangeStartHour,
                        onNavigate = viewModel::navigateRange,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onImport) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "导入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onExport) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = "导出",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            FilterBar(
                activityGroups = activityGroups.map { it.name },
                tagCategories = tagCategories,
                selectedActivityGroup = uiState.selectedActivityGroup,
                selectedTagCategory = uiState.selectedTagCategory,
                selectedStatus = uiState.selectedStatus,
                searchQuery = uiState.searchQuery,
                viewMode = uiState.viewMode,
                onActivityGroupChange = viewModel::setActivityGroupFilter,
                onTagCategoryChange = viewModel::setTagCategoryFilter,
                onStatusChange = viewModel::setStatusFilter,
                onSearchQueryChange = viewModel::setSearchQuery,
                onViewModeChange = viewModel::setViewMode,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            if (uiState.behaviors.isEmpty()) {
                EmptyStateView(
                    message = "暂无行为记录",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    if (uiState.viewMode == ViewMode.LIST) {
                        items(
                            items = displayItems,
                            key = { it.key },
                            contentType = { if (it is BehaviorManagementDisplayItem.DayDivider) "day" else "behavior" },
                        ) { displayItem ->
                            when (displayItem) {
                                is BehaviorManagementDisplayItem.DayDivider -> DayDividerRow(displayItem.label)
                                is BehaviorManagementDisplayItem.BehaviorRow -> {
                                    val item = displayItem.behaviorWithDetails
                                    BehaviorListItem(
                                        behaviorWithDetails = item,
                                        isSelected = item.behavior.id in uiState.selectedBehaviorIds,
                                        onClick = {
                                            if (uiState.isMultiSelectMode) {
                                                viewModel.toggleMultiSelect(item.behavior.id)
                                            } else {
                                                viewModel.startEditBehavior(item.behavior.id)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleMultiSelect(item.behavior.id) },
                                        isEvenItem = displayItem.behaviorIndex % 2 == 0,
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = displayItems,
                            key = { it.key },
                            contentType = { if (it is BehaviorManagementDisplayItem.DayDivider) "day" else "behavior" },
                        ) { displayItem ->
                            when (displayItem) {
                                is BehaviorManagementDisplayItem.DayDivider -> DayDividerRow(displayItem.label)
                                is BehaviorManagementDisplayItem.BehaviorRow -> {
                                    val item = displayItem.behaviorWithDetails
                                    BehaviorTimelineItem(
                                        behaviorWithDetails = item,
                                        isLast = displayItem.isLastInDay,
                                        onClick = {
                                            if (uiState.isMultiSelectMode) {
                                                viewModel.toggleMultiSelect(item.behavior.id)
                                            } else {
                                                viewModel.startEditBehavior(item.behavior.id)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleMultiSelect(item.behavior.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                SummaryBar(
                    behaviors = uiState.behaviors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    editBehavior?.let { bwd ->
        val initialStartTime = bwd.behavior.startTime.epochToLocalDateTime()
        val initialEndTime = bwd.behavior.endTime?.epochToLocalDateTime()

        AddBehaviorSheet(
            activities = activities,
            activityGroups = activityGroups,
            allTags = allTags,
            initialStartTime = initialStartTime,
            initialEndTime = initialEndTime,
            initialActivityId = bwd.activity.id,
            initialTagIds = bwd.tags.map { it.id },
            initialNote = bwd.behavior.note,
            editBehaviorId = bwd.behavior.id,
            existingBehaviors = uiState.behaviors.map { it.behavior },
            activityLastUsedMap = activityLastUsedMap,
            tagLastUsedMap = tagLastUsedMap,
            tagCategoryOrder = tagCategoryOrder,
            onDismiss = viewModel::finishEditBehavior,
            onConfirm = { activityId, tagIds, startTime, endTime, nature, note, _ ->
                viewModel.updateBehavior(
                    bwd.behavior.id, activityId, tagIds, startTime, endTime, nature, note,
                )
            },
            onActivityGroupsReordered = viewModel::reorderActivityGroups,
            onTagCategoriesReordered = viewModel::reorderTagCategories,
        )
    }

    uiState.importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            onDuplicateHandlingChange = { selectedHandling = it },
            selectedHandling = selectedHandling,
            onConfirm = {
                uiState.importData?.let { data ->
                    viewModel.executeImport(data, selectedHandling)
                }
            },
            onDismiss = viewModel::dismissImportPreview,
        )
    }
}

private sealed interface BehaviorManagementDisplayItem {
    val key: String

    data class DayDivider(
        val date: LocalDate,
        val label: String,
    ) : BehaviorManagementDisplayItem {
        override val key: String = "day-$date"
    }

    data class BehaviorRow(
        val behaviorWithDetails: BehaviorWithDetails,
        val behaviorIndex: Int,
        val isLastInDay: Boolean,
    ) : BehaviorManagementDisplayItem {
        override val key: String = "behavior-${behaviorWithDetails.behavior.id}"
    }
}

private val dayDividerFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/M/d E", Locale.CHINA)

private fun buildDisplayItems(
    behaviors: List<BehaviorWithDetails>,
): List<BehaviorManagementDisplayItem> {
    val result = mutableListOf<BehaviorManagementDisplayItem>()
    var currentDate: LocalDate? = null
    behaviors.forEachIndexed { index, item ->
        val date = item.behavior.startTime.epochToLocalDate()
        if (date != currentDate) {
            currentDate = date
            result.add(
                BehaviorManagementDisplayItem.DayDivider(
                    date = date,
                    label = date.format(dayDividerFormatter),
                )
            )
        }
        val nextDate = behaviors.getOrNull(index + 1)?.behavior?.startTime?.epochToLocalDate()
        result.add(
            BehaviorManagementDisplayItem.BehaviorRow(
                behaviorWithDetails = item,
                behaviorIndex = index,
                isLastInDay = nextDate != date,
            )
        )
    }
    return result
}

@Composable
private fun DayDividerRow(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryBar(
    behaviors: List<BehaviorWithDetails>,
    modifier: Modifier = Modifier,
) {
    val totalCount = behaviors.size
    val completedCount = behaviors.count { it.behavior.status == BehaviorNature.COMPLETED }
    val totalDurationMinutes = behaviors
        .filter { it.behavior.endTime != null }
        .sumOf { bwd ->
            (bwd.behavior.endTime!! - bwd.behavior.startTime) / 60_000
        }
    val durationText = formatDurationCompactHm(totalDurationMinutes * 60_000)

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "共 $totalCount 条",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Text(
            text = "已完成 $completedCount",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Text(
            text = "总时长 $durationText",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
