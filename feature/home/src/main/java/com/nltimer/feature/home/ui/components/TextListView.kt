package com.nltimer.feature.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.TextListColumnMode
import com.nltimer.core.data.model.TextListFieldColorMode
import com.nltimer.core.data.model.TextListFieldConfig
import com.nltimer.core.data.model.TextListFieldType
import com.nltimer.core.data.model.TextListLayoutStyle
import com.nltimer.core.data.model.TagDisplayConfig
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.data.util.hhmmFormatter
import com.nltimer.core.designsystem.component.DayDividerRow
import com.nltimer.core.designsystem.component.LoadingMoreIndicator
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.HomeListItem
import com.nltimer.feature.home.model.TagUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun TextListView(
    items: List<HomeListItem>,
    onCellClick: (GridCellUiState) -> Unit = {},
    onCellLongClick: (GridCellUiState) -> Unit = {},
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasReachedEarliest: Boolean = false,
    textListStyle: TextListLayoutStyle = TextListLayoutStyle(),
    tagDisplayConfig: TagDisplayConfig = TagDisplayConfig(),
    header: @Composable (LazyItemScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val initialScrollDone = remember { mutableStateOf(false) }

    val displayItems = remember(items) { reverseGroupedItems(items) }
    val hasHeader = header != null

    LaunchedEffect(Unit) {
        initialScrollDone.value = true
    }

    val alphaState = animateFloatAsState(
        targetValue = if (initialScrollDone.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "TextListFadeIn"
    )

    val dateIndexMap = remember(displayItems, hasHeader) {
        val map = mutableMapOf<Int, String>()
        val headerOffset = if (hasHeader) 1 else 0
        displayItems.forEachIndexed { index, item ->
            if (item is HomeListItem.DayDivider) map[index + headerOffset] = item.label
        }
        map
    }

    val visibleDateLabelState = LocalVisibleDateLabel.current
    val currentLabel by remember(dateIndexMap) {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            dateIndexMap.entries
                .filter { it.key <= firstIndex }
                .maxByOrNull { it.key }
                ?.value
        }
    }

    LaunchedEffect(currentLabel) {
        visibleDateLabelState.value = currentLabel
    }

    LaunchedEffect(displayItems, hasReachedEarliest) {
        if (hasReachedEarliest) return@LaunchedEffect
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to lastVisible
        }.distinctUntilChanged()
            .filter { (total, last) -> total > 0 && last >= total - 5 }
            .collect { onLoadMore() }
    }

    val isTable = textListStyle.columnMode == TextListColumnMode.TABLE
    val visibleFields = remember(textListStyle.fieldConfigs) {
        textListStyle.fieldConfigs.filter {
            it.visible && it.field != TextListFieldType.TAGS && it.field != TextListFieldType.ICON
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alphaState.value },
            verticalArrangement = Arrangement.spacedBy(textListStyle.rowSpacing.dp),
            contentPadding = PaddingValues(
                start = textListStyle.paddingH.dp,
                top = textListStyle.paddingV.dp + LocalImmersiveTopPadding.current,
                end = textListStyle.paddingH.dp,
                bottom = 180.dp,
            ),
        ) {
            if (header != null) {
                item(key = "header", contentType = "header") {
                    header()
                }
            }
            if (displayItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无行为记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(
                    items = displayItems,
                    key = { it.key },
                    contentType = {
                        when (it) {
                            is HomeListItem.DayDivider -> "divider"
                            is HomeListItem.CellItem -> "behavior"
                        }
                    }
                ) { item ->
                    when (item) {
                        is HomeListItem.DayDivider -> DayDividerRow(label = item.label)
                        is HomeListItem.CellItem -> {
                            if (isTable) TextListTableRow(
                                cell = item.cell,
                                style = textListStyle,
                                visibleFields = visibleFields,
                                tagDisplayConfig = tagDisplayConfig,
                                onClick = { onCellClick(item.cell) },
                                onLongClick = { onCellLongClick(item.cell) },
                            ) else TextListFlowRow(
                                cell = item.cell,
                                style = textListStyle,
                                tagDisplayConfig = tagDisplayConfig,
                                onClick = { onCellClick(item.cell) },
                                onLongClick = { onCellLongClick(item.cell) },
                            )
                        }
                    }
                }
                if (isLoadingMore) item { LoadingMoreIndicator() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextListFlowRow(
    cell: GridCellUiState,
    style: TextListLayoutStyle,
    tagDisplayConfig: TagDisplayConfig,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val baseFontSize = MaterialTheme.typography.bodySmall.fontSize * style.globalFontScale
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    val visibleFields = remember(style.fieldConfigs) { style.fieldConfigs.filter { it.visible } }
    val iconField = remember(visibleFields) { visibleFields.find { it.field == TextListFieldType.ICON } }
    val textFields = remember(visibleFields) { visibleFields.filter { it.field != TextListFieldType.ICON } }
    val annotatedText = remember(cell, textFields, baseFontSize, onSurfaceColor, primaryColor, secondaryColor, tertiaryColor, errorColor, style.separator) {
        val colorMap = mapOf(
            TextListFieldColorMode.DEFAULT to onSurfaceColor,
            TextListFieldColorMode.PRIMARY to primaryColor,
            TextListFieldColorMode.SECONDARY to secondaryColor,
            TextListFieldColorMode.TERTIARY to tertiaryColor,
            TextListFieldColorMode.ERROR to errorColor,
        )
        buildFlowAnnotatedString(cell, textFields, baseFontSize, colorMap, style.separator)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL)))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = style.paddingV.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val firstField = visibleFields.firstOrNull()
            if (iconField != null && firstField?.field == TextListFieldType.ICON) {
                cell.activityIconKey?.let {
                    IconRenderer(
                        iconKey = it,
                        iconSize = (baseFontSize.value * 1.2f).dp,
                        tint = onSurfaceColor,
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
            Text(text = annotatedText)
            if (iconField != null && firstField?.field != TextListFieldType.ICON) {
                cell.activityIconKey?.let {
                    Spacer(Modifier.width(4.dp))
                    IconRenderer(
                        iconKey = it,
                        iconSize = (baseFontSize.value * 1.2f).dp,
                        tint = onSurfaceColor,
                    )
                }
            }
        }

        val showTags = style.fieldConfigs.any { it.field == TextListFieldType.TAGS && it.visible }
        if (showTags && cell.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            TextListTagRow(tags = cell.tags, tagDisplayConfig = tagDisplayConfig)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextListTableRow(
    cell: GridCellUiState,
    style: TextListLayoutStyle,
    visibleFields: List<TextListFieldConfig>,
    tagDisplayConfig: TagDisplayConfig,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val baseFontSize = MaterialTheme.typography.bodySmall.fontSize * style.globalFontScale
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val iconField = remember(style.fieldConfigs) {
        style.fieldConfigs.find { it.visible && it.field == TextListFieldType.ICON }
    }
    val iconFirst = remember(style.fieldConfigs) {
        style.fieldConfigs.firstOrNull { it.visible }?.field == TextListFieldType.ICON
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(styledCorner(ShapeTokens.CORNER_SMALL)))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = style.paddingV.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (iconField != null && iconFirst) {
                cell.activityIconKey?.let {
                    IconRenderer(
                        iconKey = it,
                        iconSize = (baseFontSize.value * 1.2f).dp,
                        tint = onSurfaceColor,
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
            for (fieldConfig in visibleFields) {
                val text = getFieldText(cell, fieldConfig.field) ?: ""
                if (text.isEmpty()) {
                    Box(modifier = Modifier.weight(1f))
                    continue
                }
                val color = resolveFieldColor(fieldConfig.colorMode, onSurfaceColor)
                val fontWeight = if (fieldConfig.bold) FontWeight.Bold else FontWeight.Normal
                val fontStyle = if (fieldConfig.italic) FontStyle.Italic else FontStyle.Normal
                val fontSize = baseFontSize * fieldConfig.fontScale

                Text(
                    text = text,
                    color = color,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    fontSize = fontSize,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
            if (iconField != null && !iconFirst) {
                cell.activityIconKey?.let {
                    Spacer(Modifier.width(4.dp))
                    IconRenderer(
                        iconKey = it,
                        iconSize = (baseFontSize.value * 1.2f).dp,
                        tint = onSurfaceColor,
                    )
                }
            }
        }

        val showTags = style.fieldConfigs.any { it.field == TextListFieldType.TAGS && it.visible }
        if (showTags && cell.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            TextListTagRow(tags = cell.tags, tagDisplayConfig = tagDisplayConfig)
        }
    }
}

private fun buildFlowAnnotatedString(
    cell: GridCellUiState,
    textFields: List<TextListFieldConfig>,
    baseFontSize: androidx.compose.ui.unit.TextUnit,
    colorMap: Map<TextListFieldColorMode, Color>,
    separator: String,
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var added = false
        for (fieldConfig in textFields) {
            val text = getFieldText(cell, fieldConfig.field) ?: continue
            if (added && separator.isNotEmpty()) {
                append(separator)
            }
            val color = colorMap[fieldConfig.colorMode] ?: Color.Unspecified
            val fontWeight = if (fieldConfig.bold) FontWeight.Bold else FontWeight.Normal
            val fontStyle = if (fieldConfig.italic) FontStyle.Italic else FontStyle.Normal
            val fontSize = baseFontSize * fieldConfig.fontScale

            withStyle(
                SpanStyle(
                    color = color,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    fontSize = fontSize,
                )
            ) {
                append(text)
            }
            added = true
        }
    }
}

@Composable
private fun resolveFieldColor(colorMode: TextListFieldColorMode, default: Color): Color = when (colorMode) {
    TextListFieldColorMode.DEFAULT -> default
    TextListFieldColorMode.PRIMARY -> MaterialTheme.colorScheme.primary
    TextListFieldColorMode.SECONDARY -> MaterialTheme.colorScheme.secondary
    TextListFieldColorMode.TERTIARY -> MaterialTheme.colorScheme.tertiary
    TextListFieldColorMode.ERROR -> MaterialTheme.colorScheme.error
}

private fun getFieldText(cell: GridCellUiState, field: TextListFieldType): String? = when (field) {
    TextListFieldType.NAME -> cell.activityName ?: "未知"
    TextListFieldType.TIME_RANGE -> {
        val start = cell.startTime?.format(hhmmFormatter) ?: "--:--"
        val end = cell.endTime?.format(hhmmFormatter) ?: "进行中"
        "$start-$end"
    }
    TextListFieldType.DURATION -> {
        val duration = cell.durationMs ?: cell.actualDuration
        if (duration != null && duration > 0) formatDuration(duration) else null
    }
    TextListFieldType.TAGS -> null
    TextListFieldType.STATUS -> when (cell.status) {
        BehaviorNature.ACTIVE -> "进行中"
        BehaviorNature.COMPLETED -> "已完成"
        BehaviorNature.PENDING -> "目标"
        null -> null
    }
    TextListFieldType.NOTE -> cell.note?.takeIf { it.isNotBlank() }?.let { "备注:$it" }
    TextListFieldType.POMODORO -> if (cell.pomodoroCount > 0) "番茄:${cell.pomodoroCount}" else null
    TextListFieldType.ESTIMATED -> cell.estimatedDuration?.let { "预估:${formatDuration(it)}" }
    TextListFieldType.ACHIEVEMENT -> cell.achievementLevel?.let { "完成度:$it" }
    TextListFieldType.PLANNED -> if (cell.wasPlanned) "计划内" else null
    TextListFieldType.ICON -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextListTagRow(tags: List<TagUiState>, tagDisplayConfig: TagDisplayConfig) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tags.forEach { tag ->
            TagChipSmall(tag.name, tagDisplayConfig = tagDisplayConfig)
        }
    }
}

private fun reverseGroupedItems(items: List<HomeListItem>): List<HomeListItem> {
    val groups = mutableListOf<Pair<HomeListItem.DayDivider, MutableList<HomeListItem.CellItem>>>()
    items.forEach { item ->
        when (item) {
            is HomeListItem.DayDivider -> groups.add(item to mutableListOf())
            is HomeListItem.CellItem -> groups.lastOrNull()?.second?.add(item)
        }
    }
    return groups.asReversed().flatMap { (divider, cells) ->
        listOf<HomeListItem>(divider) + cells.asReversed()
    }
}
