package com.nltimer.feature.stats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventsContentMode
import com.nltimer.core.data.model.EventsViewMode
import com.nltimer.core.data.util.formatTimestamp
import com.nltimer.core.data.util.summarizeEventValues
import com.nltimer.core.designsystem.component.AppTagChip
import com.nltimer.core.designsystem.component.AppTagChipStyle
import com.nltimer.feature.stats.model.EventPanelFilters
import com.nltimer.feature.stats.model.EventsPanelUiState

/** 面板是 LazyVerticalGrid 内的静态 item，禁止嵌套 LazyColumn：卡片/表格列表最多渲染这么多条 */
private const val MaxVisibleEvents = 10

/** 模板徽标兜底配色（按 templateId 稳定取模，混排视图中不同模板徽标颜色可区分） */
private val TemplateBadgePalette = listOf(
    0xFF6750A4, // 紫
    0xFF00696F, // 青
    0xFF984061, // 玫红
    0xFF815600, // 琥珀
    0xFF415F91, // 蓝
)

private fun badgeColorFor(templateId: Long?): Long? {
    if (templateId == null || templateId <= 0L) return null
    val size = TemplateBadgePalette.size
    val index = ((templateId % size) + size).toInt() % size
    return TemplateBadgePalette[index]
}

/**
 * "复盘事件" 面板内容（面板头双切换 + 筛选条 + 卡片/表格事件列表 + 空态）
 * - 混排（MIXED）或单模板聚焦（FOCUS + 模板下拉）
 * - 卡片/表格双视图切换（先例 behavior_management ViewMode SegmentedButton）
 * - 平铺渲染非 Lazy（外层仪表盘网格可滚动，内嵌 Lazy 禁令见 docs/agent/06-common-bug.md）
 */
@Composable
internal fun EventsPanelContent(
    state: EventsPanelUiState,
    onViewModeChange: (EventsViewMode) -> Unit = {},
    onFocusTemplateChange: (Long?) -> Unit = {},
    onFilterChange: (EventPanelFilters) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HeaderRow(
            state = state,
            onViewModeChange = onViewModeChange,
            onFocusTemplateChange = onFocusTemplateChange,
        )
        Spacer(modifier = Modifier.height(8.dp))
        EventFilterBar(
            state = state,
            onFilterChange = onFilterChange,
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            !state.hasPanel -> EmptyPanelMessage("添加「复盘事件」面板后在此展示")
            state.allEvents.isEmpty() -> EmptyPanelMessage("还没有事件，开始你的第一条复盘")
            state.events.isEmpty() -> EmptyPanelMessage("无匹配事件")
            else -> EventList(state = state)
        }
    }
}

@Composable
private fun HeaderRow(
    state: EventsPanelUiState,
    onViewModeChange: (EventsViewMode) -> Unit,
    onFocusTemplateChange: (Long?) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            SegmentedButton(
                selected = state.contentMode == EventsContentMode.MIXED,
                onClick = { onFocusTemplateChange(null) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("混排", style = MaterialTheme.typography.labelMedium)
            }
            SegmentedButton(
                selected = state.contentMode == EventsContentMode.FOCUS,
                onClick = { onFocusTemplateChange(state.focusTemplateId ?: state.templates.firstOrNull()?.id) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("聚焦", style = MaterialTheme.typography.labelMedium)
            }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            SegmentedButton(
                selected = state.viewMode == EventsViewMode.CARD,
                onClick = { onViewModeChange(EventsViewMode.CARD) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("卡片", style = MaterialTheme.typography.labelMedium)
            }
            SegmentedButton(
                selected = state.viewMode == EventsViewMode.TABLE,
                onClick = { onViewModeChange(EventsViewMode.TABLE) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("表格", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    // 聚焦模式下的具体模板选择（FilterChipDropdown 单选先例）
    if (state.contentMode == EventsContentMode.FOCUS) {
        Spacer(modifier = Modifier.height(6.dp))
        FocusTemplateSelector(
            templates = state.templates,
            selectedTemplateId = state.focusTemplateId,
            onSelect = onFocusTemplateChange,
        )
    }
}

@Composable
private fun FocusTemplateSelector(
    templates: List<EventTemplate>,
    selectedTemplateId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selectedTemplateId != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = templateDisplayName(selectedTemplateId, templates),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "选择聚焦模板",
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            templates.forEach { template ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (template.id == selectedTemplateId) "✓ ${template.name}" else template.name,
                        )
                    },
                    onClick = {
                        onSelect(template.id)
                        expanded = false
                    },
                )
            }
            if (templates.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("还没有打点模板，可在 设置 → 打点模板 中创建") },
                    onClick = { expanded = false },
                )
            }
        }
    }
}

private fun templateDisplayName(templateId: Long?, templates: List<EventTemplate>): String =
    templateId?.let { id -> templates.firstOrNull { it.id == id }?.name } ?: "选择模板"

@Composable
private fun EventList(state: EventsPanelUiState) {
    val templates = state.templates
    val displayed = state.events.take(MaxVisibleEvents)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state.viewMode) {
            EventsViewMode.CARD -> displayed.forEach { item ->
                EventCardRow(item = item, templates = templates)
            }
            EventsViewMode.TABLE -> displayed.forEachIndexed { index, item ->
                EventTableRow(event = item, templates = templates, isEvenItem = index % 2 == 1)
            }
        }
        if (state.events.size > MaxVisibleEvents) {
            Text(
                text = "…还有 ${state.events.size - MaxVisibleEvents} 条（仅显示最近 $MaxVisibleEvents 条）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventCardRow(
    item: BehaviorEventWithValues,
    templates: List<EventTemplate>,
    modifier: Modifier = Modifier,
) {
    val templateName = templates.firstOrNull { it.id == item.event.templateId }?.name
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppTagChip(
                label = templateName ?: "未知模板",
                color = badgeColorFor(item.event.templateId),
                style = AppTagChipStyle.Compact,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatTimestamp(item.event.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (item.event.behaviorId == null) {
                AppTagChip(
                    label = "独立",
                    color = null,
                    style = AppTagChipStyle.Compact,
                )
            }
        }
        val summary = item.values.summarizeEventValues()
        if (summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EventTableRow(
    event: BehaviorEventWithValues,
    templates: List<EventTemplate>,
    isEvenItem: Boolean,
    modifier: Modifier = Modifier,
) {
    val templateName = templates.firstOrNull { it.id == event.event.templateId }?.name
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isEvenItem) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatTimestamp(event.event.timestamp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.6f),
        )
        Text(
            text = templateName ?: "未知模板",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = event.values.summarizeEventValues().ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.8f),
        )
    }
}

@Composable
private fun EmptyPanelMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
