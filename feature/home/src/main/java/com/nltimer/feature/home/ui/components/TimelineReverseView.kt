package com.nltimer.feature.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.data.util.formatDuration
import com.nltimer.core.data.util.hhmmFormatter
import com.nltimer.core.designsystem.icon.IconRenderer
import com.nltimer.core.designsystem.theme.BorderTokens
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.appBorder
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledBorder
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.HomeListItem
import com.nltimer.feature.home.model.TagUiState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun TimelineReverseView(
    items: List<HomeListItem>,
    onAddClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit = {},
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasReachedEarliest: Boolean = false,
    timelineStyle: TimelineLayoutStyle = TimelineLayoutStyle(),
    modifier: Modifier = Modifier,
    header: @Composable (LazyItemScope.() -> Unit)? = null,
) {
    val timeFormatter = hhmmFormatter
    val listState = rememberLazyListState()
    var detailCell by remember { mutableStateOf<GridCellUiState?>(null) }
    val initialScrollDone = remember { mutableStateOf(false) }

    val timelineItems = remember(items) { buildTimelineItemsReversed(items) }
    val hasHeader = header != null

    LaunchedEffect(timelineItems) {
        if (timelineItems.isNotEmpty() && !initialScrollDone.value) {
            initialScrollDone.value = true
        }
    }

    val alphaState = animateFloatAsState(
        targetValue = if (initialScrollDone.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "TimelineFadeIn"
    )

    val dateIndexMap = remember(timelineItems, hasHeader) {
        val map = mutableMapOf<Int, String>()
        val headerOffset = if (hasHeader) 1 else 0
        timelineItems.forEachIndexed { index, item ->
            if (item is TimelineDisplayItem.Divider) map[index + headerOffset] = item.label
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

    LaunchedEffect(timelineItems, hasReachedEarliest) {
        if (hasReachedEarliest) return@LaunchedEffect
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to lastVisible
        }.distinctUntilChanged()
            .filter { (total, last) -> total > 0 && last >= total - 5 }
            .collect { onLoadMore() }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .graphicsLayer { this.alpha = alphaState.value }) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(timelineStyle.itemSpacing.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp + LocalImmersiveTopPadding.current, end = 16.dp, bottom = 180.dp),
        ) {
            if (header != null) {
                item(key = "header", contentType = "header") {
                    header()
                }
            }
            items(
                items = timelineItems,
                key = { it.key },
                contentType = {
                    when (it) {
                        is TimelineDisplayItem.Divider -> "divider"
                        is TimelineDisplayItem.BehaviorRow -> "behavior"
                        is TimelineDisplayItem.Idle -> "idle"
                    }
                }
            ) { item ->
                when (item) {
                    is TimelineDisplayItem.Divider -> DayDividerRow(label = item.label)
                    is TimelineDisplayItem.BehaviorRow -> TimelineBehaviorItem(
                        behavior = item.cell,
                        timeFormatter = timeFormatter,
                        onClick = { detailCell = item.cell },
                        onLongClick = { onCellLongClick(item.cell) },
                    )
                    is TimelineDisplayItem.Idle -> TimelineIdleItem(
                        start = item.start,
                        end = item.end,
                        timeFormatter = timeFormatter,
                        onAddClick = { onAddClick(item.start.plusOneMillis(), item.end) },
                    )
                }
            }
            if (isLoadingMore) item { LoadingMoreIndicator() }
        }
    }

    detailCell?.let { cell ->
        BehaviorDetailDialog(cell = cell, onDismiss = { detailCell = null })
    }
}

private sealed class TimelineDisplayItem(val key: String) {
    class Divider(val date: LocalDate, val label: String) : TimelineDisplayItem("divider-$date")
    class BehaviorRow(val cell: GridCellUiState) : TimelineDisplayItem("behavior-${cell.behaviorId}")
    class Idle(val start: LocalDateTime, val end: LocalDateTime) : TimelineDisplayItem("idle-$start-$end")
}

private fun LocalDateTime.plusOneMillis(): LocalDateTime =
    plus(1, ChronoUnit.MILLIS)

private fun buildTimelineItemsReversed(items: List<HomeListItem>): List<TimelineDisplayItem> {
    data class DayBucket(val divider: HomeListItem.DayDivider, val cells: MutableList<GridCellUiState>)
    val buckets = mutableListOf<DayBucket>()
    items.forEach { item ->
        when (item) {
            is HomeListItem.DayDivider -> buckets.add(DayBucket(item, mutableListOf()))
            is HomeListItem.CellItem -> buckets.lastOrNull()?.cells?.add(item.cell)
        }
    }

    val result = mutableListOf<TimelineDisplayItem>()
    buckets.asReversed().forEach { bucket ->
        val sortedAsc = bucket.cells.sortedWith(compareBy(nullsFirst()) { it.startTime })
        if (sortedAsc.isEmpty()) return@forEach
        result.add(TimelineDisplayItem.Divider(bucket.divider.date, bucket.divider.label))
        for (i in sortedAsc.indices.reversed()) {
            val cell = sortedAsc[i]
            result.add(TimelineDisplayItem.BehaviorRow(cell))
            if (i > 0) {
                val prevEnd = sortedAsc[i - 1].endTime
                val currentStart = cell.startTime
                if (prevEnd != null && currentStart != null && currentStart.isAfter(prevEnd)) {
                    val gap = Duration.between(prevEnd, currentStart)
                    if (gap.toMinutes() >= 1) {
                        result.add(TimelineDisplayItem.Idle(prevEnd, currentStart))
                    }
                }
            }
        }
    }
    return result
}

@Composable
private fun DayDividerRow(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
private fun LoadingMoreIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun TimelineIdleItem(
    start: LocalDateTime,
    end: LocalDateTime,
    timeFormatter: DateTimeFormatter,
    onAddClick: () -> Unit
) {
    val duration = Duration.between(start, end)
    val durationText = formatDuration(duration.toMillis())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(50.dp)
        ) {
            Text(
                text = end.format(timeFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = start.format(timeFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = styledAlpha(0.6f))
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = styledAlpha(0.1f)))
                .appBorder(
                    borderProducer = {
                        BorderStroke(
                            styledBorder(BorderTokens.THIN),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = styledAlpha(0.3f))
                        )
                    },
                    shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM))
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "❓ 空闲 : $durationText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.5f)),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TimelineBehaviorItem(
    behavior: GridCellUiState,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(50.dp)
        ) {
            Text(
                text = behavior.startTime?.format(timeFormatter) ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            behavior.endTime?.let {
                Text(
                    text = it.format(timeFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val cardBackground = if (behavior.isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        }
        val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

        Column(
            modifier = Modifier
                .weight(1f)
                .behaviorCardStyle(cardBackground, borderColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconRenderer(
                        iconKey = behavior.activityIconKey,
                        defaultEmoji = "❓",
                        iconSize = 16.dp,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = behavior.activityName ?: "未知",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val duration = if (behavior.isCurrent && behavior.startEpochMs != null) {
                    LiveElapsedDuration(
                        startEpochMs = behavior.startEpochMs,
                        isCurrent = true,
                        fallbackDurationMs = behavior.durationMs ?: (behavior.actualDuration ?: 0L),
                    )
                } else {
                    behavior.durationMs ?: (behavior.actualDuration ?: 0L)
                }
                if (duration > 0) {
                    Text(
                        text = "⏱ ${formatDuration(duration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BehaviorTagRow(behavior.tags)

            behavior.note?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
