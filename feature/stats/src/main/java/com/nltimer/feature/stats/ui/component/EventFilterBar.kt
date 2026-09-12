package com.nltimer.feature.stats.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.model.EventsContentMode
import com.nltimer.core.designsystem.theme.appAssistChipBorder
import com.nltimer.core.designsystem.theme.appInputChipBorder
import com.nltimer.feature.stats.model.EventPanelFilters
import com.nltimer.feature.stats.model.EventsPanelUiState
import com.nltimer.feature.stats.model.toggledOption
import com.nltimer.feature.stats.model.withRange
import com.nltimer.feature.stats.model.withSearchText

/**
 * 「复盘事件」面板动态筛选条
 * - 文本搜索：SearchChip + SearchDialog（先例 behavior_management FilterBar.kt:217/269），
 *   LIKE 匹配任一 valueText（applyEventFilters 内存实现）
 * - 字段级筛选仅聚焦模式展示：按模板字段动态生成——SELECT 选项多选 FilterChip
 *   （单选 chip 先例扩多选 Set）；NUMBER / RATING 值域 M3 RangeSlider（仓内零引用，用 material3 API）
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EventFilterBar(
    state: EventsPanelUiState,
    onFilterChange: (EventPanelFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSearchDialog by remember { mutableStateOf(false) }
    val fields = state.focusedFields

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SearchChip(
                searchQuery = state.filters.searchText,
                onClick = { showSearchDialog = true },
                onClear = { onFilterChange(state.filters.withSearchText("")) },
            )
            if (state.filters.hasActiveFilters) {
                AssistChip(
                    onClick = { onFilterChange(EventPanelFilters()) },
                    label = { Text("清除筛选", style = MaterialTheme.typography.labelMedium) },
                    border = appAssistChipBorder(),
                )
            }
        }

        // 字段级筛选仅聚焦模式有意义（混排时 fieldId 跨模板易撞名，R4 决策：聚焦才生成）
        if (state.contentMode == EventsContentMode.FOCUS) {
            val selectableFields = fields.filter { it.type == EventFieldType.SELECT && it.options.isNotEmpty() }
            val numericFields = fields.filter { it.type == EventFieldType.NUMBER || it.type == EventFieldType.RATING }
            selectableFields.forEach { field ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = field.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    field.options.forEach { option ->
                        val selected = state.filters.optionSelection[field.id].orEmpty().contains(option)
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(state.filters.toggledOption(field.id, option)) },
                            label = { Text(text = option, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
            numericFields.forEach { field ->
                val domain = state.numberDomains[field.id] ?: return@forEach
                ValueRangeRow(
                    field = field,
                    domain = domain,
                    currentFilterRange = state.filters.ranges[field.id],
                    onRangeChange = { range ->
                        onFilterChange(state.filters.withRange(field.id, range, domain))
                    },
                )
            }
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            query = state.filters.searchText,
            onQueryChange = { onFilterChange(state.filters.withSearchText(it)) },
            onDismiss = { showSearchDialog = false },
        )
    }
}

/** NUMBER / RATING 字段值域行（M3 RangeSlider；域端点即关闭筛选——withRange 判定全域移除） */
@Composable
private fun ValueRangeRow(
    field: EventTemplateField,
    domain: ClosedFloatingPointRange<Float>,
    currentFilterRange: ClosedFloatingPointRange<Float>?,
    onRangeChange: (ClosedFloatingPointRange<Float>?) -> Unit,
) {
    var sliderValue by remember(field.id, domain, currentFilterRange) {
        mutableStateOf(currentFilterRange ?: domain)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${formatRangeEndpoint(sliderValue.start)} – ${formatRangeEndpoint(sliderValue.endInclusive)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        RangeSlider(
            value = sliderValue,
            onValueChange = { newValue -> sliderValue = newValue },
            onValueChangeFinished = { onRangeChange(sliderValue) },
            valueRange = domain,
        )
    }
}

private fun formatRangeEndpoint(value: Float): String =
    if (value == value.toLong().toFloat()) value.toLong().toString() else "%.1f".format(value)

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
        title = { Text("搜索事件", style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("输入关键词（匹配字段值）", style = MaterialTheme.typography.bodyMedium) },
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
