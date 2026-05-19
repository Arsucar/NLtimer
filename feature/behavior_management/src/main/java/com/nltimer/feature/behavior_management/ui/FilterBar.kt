package com.nltimer.feature.behavior_management.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.theme.appAssistChipBorder
import com.nltimer.core.designsystem.theme.appInputChipBorder
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.feature.behavior_management.model.ViewMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    activityGroups: List<String>,
    tagCategories: List<String>,
    selectedActivityGroup: String?,
    selectedTagCategory: String?,
    selectedStatus: BehaviorNature?,
    searchQuery: String,
    viewMode: ViewMode,
    onActivityGroupChange: (String?) -> Unit,
    onTagCategoryChange: (String?) -> Unit,
    onStatusChange: (BehaviorNature?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activityExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChipDropdown(
            label = selectedActivityGroup ?: "活动分组",
            selected = selectedActivityGroup != null,
            expanded = activityExpanded,
            onExpandChange = { activityExpanded = it },
        ) {
            DropdownMenuItem(
                text = { Text("全部") },
                onClick = {
                    onActivityGroupChange(null)
                    activityExpanded = false
                },
            )
            activityGroups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group) },
                    onClick = {
                        onActivityGroupChange(group)
                        activityExpanded = false
                    },
                )
            }
        }

        FilterChipDropdown(
            label = selectedTagCategory ?: "标签分类",
            selected = selectedTagCategory != null,
            expanded = tagExpanded,
            onExpandChange = { tagExpanded = it },
        ) {
            DropdownMenuItem(
                text = { Text("全部") },
                onClick = {
                    onTagCategoryChange(null)
                    tagExpanded = false
                },
            )
            tagCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onTagCategoryChange(category)
                        tagExpanded = false
                    },
                )
            }
        }

        FilterChipDropdown(
            label = selectedStatus?.let { statusLabel(it) } ?: "状态",
            selected = selectedStatus != null,
            expanded = statusExpanded,
            onExpandChange = { statusExpanded = it },
        ) {
            DropdownMenuItem(
                text = { Text("全部") },
                onClick = {
                    onStatusChange(null)
                    statusExpanded = false
                },
            )
            BehaviorNature.entries.forEach { nature ->
                DropdownMenuItem(
                    text = { Text(statusLabel(nature)) },
                    onClick = {
                        onStatusChange(nature)
                        statusExpanded = false
                    },
                )
            }
        }

        SearchChip(
            searchQuery = searchQuery,
            onClick = { showSearchDialog = true },
            onClear = { onSearchQueryChange("") },
        )

        Spacer(Modifier.width(4.dp))

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = viewMode == ViewMode.LIST,
                onClick = { onViewModeChange(ViewMode.LIST) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("列表", style = MaterialTheme.typography.labelMedium)
            }
            SegmentedButton(
                selected = viewMode == ViewMode.TIMELINE,
                onClick = { onViewModeChange(ViewMode.TIMELINE) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("时间轴", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onDismiss = { showSearchDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipDropdown(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box {
        FilterChip(
            selected = selected,
            onClick = { onExpandChange(true) },
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "展开筛选",
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchChip(
    searchQuery: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    if (searchQuery.isBlank()) {
        AssistChip(
            onClick = onClick,
            label = { Text("搜索", style = MaterialTheme.typography.labelMedium) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            border = appAssistChipBorder(),
        )
    } else {
        InputChip(
            selected = true,
            onClick = onClick,
            label = {
                Text(
                    text = searchQuery,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(InputChipDefaults.IconSize),
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清除搜索",
                    modifier = Modifier
                        .size(InputChipDefaults.IconSize)
                        .clickable(onClick = onClear),
                )
            },
            border = appInputChipBorder(selected = true),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索行为", style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("输入关键词", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清空",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else null,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
    )
}

private fun statusLabel(nature: BehaviorNature): String = when (nature) {
    BehaviorNature.COMPLETED -> "已完成"
    BehaviorNature.ACTIVE -> "进行中"
    BehaviorNature.PENDING -> "待定"
}
