package com.nltimer.feature.stats.model

import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.model.EventsContentMode
import com.nltimer.core.data.model.EventsViewMode

/**
 * EVENTS 面板的字段值筛选状态（内存态，不做 DataStore 持久化）
 * - [optionSelection]：SELECT 字段多选等值（fieldId → 选中选项集合；空集合 = 未启用该字段筛选）
 * - [ranges]：NUMBER / RATING 字段值域（fieldId → 闭区间；等于全域 = 未启用）
 * - [searchText]：文本 LIKE（任一 valueText containsIgnoreCase；空 = 未启用）
 */
data class EventPanelFilters(
    val optionSelection: Map<Long, Set<String>> = emptyMap(),
    val ranges: Map<Long, ClosedFloatingPointRange<Float>> = emptyMap(),
    val searchText: String = "",
) {
    val hasActiveFilters: Boolean
        get() = optionSelection.values.any { it.isNotEmpty() } ||
            ranges.isNotEmpty() ||
            searchText.isNotBlank()
}

/**
 * EVENTS 面板 UI 状态
 * [allEvents] 为未过滤原始事件（用作筛选滑条值域计算），[events] 为应用筛选后的显示集合
 */
data class EventsPanelUiState(
    val hasPanel: Boolean = false,
    val viewMode: EventsViewMode = EventsViewMode.CARD,
    val contentMode: EventsContentMode = EventsContentMode.MIXED,
    val focusTemplateId: Long? = null,
    val templates: List<EventTemplate> = emptyList(),
    /** 聚焦模板的字段结构（驱动动态筛选器；混排模式为空） */
    val focusedFields: List<EventTemplateField> = emptyList(),
    /** 数字/星级字段的既有值域（滑条默认区间、判断「是否启用」的基准） */
    val numberDomains: Map<Long, ClosedFloatingPointRange<Float>> = emptyMap(),
    val allEvents: List<BehaviorEventWithValues> = emptyList(),
    val events: List<BehaviorEventWithValues> = emptyList(),
    val filters: EventPanelFilters = EventPanelFilters(),
)

/** 星级字段的固定值域（1~5 星，0 允许作为未评分边界） */
private val RATING_SLIDER_RANGE: ClosedFloatingPointRange<Float> = 0f..5f

/** 数值字段无数据时的兜底值域 */
private val NUMBER_FALLBACK_RANGE: ClosedFloatingPointRange<Float> = 0f..100f

/**
 * 每个数字/星级字段的既有值域（来自未过滤事件集，避免筛选反馈环路收缩滑条区间）
 * - RATING 固定 0..5；NUMBER 取数据 min/max（min==max 时 ±1 保持滑条可用）
 */
fun computeNumberDomains(
    events: List<BehaviorEventWithValues>,
    fields: List<EventTemplateField>,
): Map<Long, ClosedFloatingPointRange<Float>> {
    val numericFieldTypes = fields.associate { it.id to it.type }
        .filterValues { it == EventFieldType.NUMBER || it == EventFieldType.RATING }
    val numericFieldIds = numericFieldTypes.keys
    if (numericFieldIds.isEmpty()) return emptyMap()
    val byField = mutableMapOf<Long, MutableList<Double>>()
    for (item in events) {
        for (value in item.values) {
            val number = value.valueNumber ?: continue
            if (value.fieldId in numericFieldIds) {
                byField.getOrPut(value.fieldId) { mutableListOf() }.add(number)
            }
        }
    }
    return byField.mapValues { (fieldId, numbers) ->
        if (numericFieldTypes[fieldId] == EventFieldType.RATING) {
            RATING_SLIDER_RANGE
        } else if (numbers.isEmpty()) {
            NUMBER_FALLBACK_RANGE
        } else {
            val min = numbers.min()
            val max = numbers.max()
            if (min == max) {
                (min - 1.0).toFloat()..(max + 1.0).toFloat()
            } else {
                min.toFloat()..max.toFloat()
            }
        }
    }
}

/**
 * 把筛选状态应用到事件集合（客户端过滤；P1 仓库全量 observeEventsWithValues + 内存筛选）
 * - SELECT：事件含该字段且 valueText ∈ 选中集合；选中集合非空时字段无值 → 不匹配
 * - NUMBER / RATING range：valueNumber ∈ 区间；已启用该字段筛选但无值 → 不匹配
 * - searchText：任一 valueText containsIgnoreCase
 * 顺序：先全量后过滤，[allEvents] 供滑条值域计算使用
 */
fun applyEventFilters(
    events: List<BehaviorEventWithValues>,
    filters: EventPanelFilters,
): List<BehaviorEventWithValues> {
    if (!filters.hasActiveFilters) return events
    val query = filters.searchText.trim().lowercase()
    val activeOptions = filters.optionSelection.filterValues { it.isNotEmpty() }
    val ranges = filters.ranges
    return events.filter { item ->
        val values = item.values
        val matchedAllOptions = activeOptions.all { (fieldId, selected) ->
            values.any { v -> v.fieldId == fieldId && v.valueText != null && v.valueText!! in selected }
        }
        if (!matchedAllOptions) return@filter false

        val matchedRanges = ranges.all { (fieldId, range) ->
            values.any { v ->
                val n = v.valueNumber ?: return@any false
                n >= range.start && n <= range.endInclusive
            }
        }
        if (!matchedRanges) return@filter false

        query.isEmpty() || values.any { v -> v.valueText?.lowercase()?.contains(query) == true }
    }
}

// ---- 筛选状态构造 helpers（纯函数，UI 组件单回调 onFilterChange(newFilters) 使用）----

/** SELECT 选项多选切换（toggle fieldId 的 option） */
fun EventPanelFilters.toggledOption(fieldId: Long, option: String): EventPanelFilters {
    val current = optionSelection[fieldId].orEmpty()
    val next = if (option in current) current - option else current + option
    val updated = if (next.isEmpty()) optionSelection - fieldId else optionSelection + (fieldId to next)
    return copy(optionSelection = updated)
}

/**
 * 设置 NUMBER / RATING 值域筛选；range 为 null 或等于 [domain]（全域）时移除该字段筛选
 * （避免滑条拖到端点仍被判定为已启用筛选）
 */
fun EventPanelFilters.withRange(
    fieldId: Long,
    range: ClosedFloatingPointRange<Float>?,
    domain: ClosedFloatingPointRange<Float>?,
): EventPanelFilters {
    val selectedRange = range ?: return copy(ranges = ranges - fieldId)
    val isFullOrNull = domain != null &&
        selectedRange.start <= domain.start + FULL_RANGE_EPSILON &&
        selectedRange.endInclusive >= domain.endInclusive - FULL_RANGE_EPSILON
    return if (isFullOrNull) {
        copy(ranges = ranges - fieldId)
    } else {
        copy(ranges = ranges + (fieldId to selectedRange))
    }
}

private const val FULL_RANGE_EPSILON = 1e-6f

/** 文本 LIKE 搜索 */
fun EventPanelFilters.withSearchText(text: String): EventPanelFilters =
    copy(searchText = text)
