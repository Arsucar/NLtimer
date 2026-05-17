package com.nltimer.feature.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.feature.home.model.GridCellUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.TreeMap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private sealed class MomentDisplayItem(val key: String) {
    class Divider(val label: String) : MomentDisplayItem("divider-$label")
    class Behavior(val cell: GridCellUiState) : MomentDisplayItem("behavior-${cell.behaviorId}")
}

private fun momentDayLabel(date: LocalDate, today: LocalDate): String {
    val datePart = "${date.monthValue}月${date.dayOfMonth}日"
    val days = ChronoUnit.DAYS.between(date, today)
    return when (days) {
        0L -> "今天 $datePart"
        1L -> "昨天 $datePart"
        else -> datePart
    }
}

private fun cellDate(cell: GridCellUiState): LocalDate? {
    val epoch = cell.startEpochMs ?: return null
    return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun buildMomentDisplayItems(
    behaviors: List<GridCellUiState>,
    today: LocalDate,
): List<MomentDisplayItem> {
    val pending = behaviors.filter { it.status == BehaviorNature.PENDING }
    val nonPending = behaviors.filter { it.status != BehaviorNature.PENDING }

    val byDate = nonPending.groupBy { cellDate(it) }
        .toSortedMap(compareBy { it })

    val result = mutableListOf<MomentDisplayItem>()

    if (pending.isNotEmpty()) {
        pending.forEach { result.add(MomentDisplayItem.Behavior(it)) }
    }

    byDate.entries.reversed().forEach { (date, cells) ->
        if (date != null && date != today) {
            result.add(MomentDisplayItem.Divider(momentDayLabel(date, today)))
        }
        cells.forEach { result.add(MomentDisplayItem.Behavior(it)) }
    }

    return result
}

enum class MomentFilterTab {
    ALL,
    COMPLETED,
    PENDING,
}

enum class MomentSortMode(val label: String) {
    TIME_DESC("时间反"),
    TIME_ASC("时间正"),
    DURATION("用时"),
}

@Composable
fun MomentView(
    cells: List<GridCellUiState>,
    hasActiveBehavior: Boolean,
    activeBehaviorId: Long?,
    onCompleteBehavior: (Long) -> Unit,
    onStartNextPending: () -> Unit,
    onStartBehavior: (Long) -> Unit,
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasReachedEarliest: Boolean = false,
    momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    modifier: Modifier = Modifier,
    header: @Composable (LazyItemScope.() -> Unit)? = null,
) {
    val momentFilterState = LocalMomentFilterState.current
    val filterTab = remember(momentFilterState.filterKey) {
        when (momentFilterState.filterKey) {
            "COMPLETED" -> MomentFilterTab.COMPLETED
            "PENDING" -> MomentFilterTab.PENDING
            else -> MomentFilterTab.ALL
        }
    }
    val sortMode = remember(momentFilterState.sortKey) {
        when (momentFilterState.sortKey) {
            "TIME_ASC" -> MomentSortMode.TIME_ASC
            "DURATION" -> MomentSortMode.DURATION
            else -> MomentSortMode.TIME_DESC
        }
    }

    val activeCell = remember(cells) {
        cells.firstOrNull {
            it.isCurrent && it.behaviorId != null && it.status == BehaviorNature.ACTIVE
        }
    }
    val nextPendingCell = remember(cells) {
        cells.firstOrNull { it.behaviorId != null && it.status == BehaviorNature.PENDING }
    }

    var detailCell by remember { mutableStateOf<GridCellUiState?>(null) }

    val listState = rememberLazyListState()
    val initialScrollDone = rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val behaviors = remember(cells, filterTab, sortMode) {
        val filtered = cells.filter { it.behaviorId != null }.let { list ->
            when (filterTab) {
                MomentFilterTab.ALL -> list
                MomentFilterTab.COMPLETED -> list.filter { it.status == BehaviorNature.COMPLETED || it.status == BehaviorNature.ACTIVE }
                MomentFilterTab.PENDING -> list.filter { it.status == BehaviorNature.PENDING }
            }
        }
        val pending = filtered.filter { it.status == BehaviorNature.PENDING }
            .sortedBy { it.startEpochMs }
        val nonPending = filtered.filter { it.status != BehaviorNature.PENDING }
        val sortedNonPending = when (sortMode) {
            MomentSortMode.TIME_DESC -> nonPending.sortedByDescending { it.startEpochMs ?: 0L }
            MomentSortMode.TIME_ASC -> nonPending.sortedBy { it.startEpochMs ?: 0L }
            MomentSortMode.DURATION -> nonPending.sortedByDescending { it.actualDuration ?: it.durationMs ?: 0L }
        }
        pending + sortedNonPending
    }

    val displayItems = remember(behaviors, today) {
        buildMomentDisplayItems(behaviors, today)
    }
    val hasHeader = header != null

    LaunchedEffect(displayItems) {
        if (displayItems.isNotEmpty() && !initialScrollDone.value) {
            initialScrollDone.value = true
        }
    }

    val alphaState = animateFloatAsState(
        targetValue = if (initialScrollDone.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "MomentFadeIn"
    )

    val dateIndexMap = remember(displayItems, hasHeader) {
        val map = TreeMap<Int, String>()
        val headerOffset = if (hasHeader) 1 else 0
        displayItems.forEachIndexed { index, item ->
            if (item is MomentDisplayItem.Divider) map[index + headerOffset] = item.label
        }
        map
    }

    val visibleDateLabelState = LocalVisibleDateLabel.current
    val currentLabel by remember(dateIndexMap) {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            dateIndexMap.floorEntry(firstIndex)?.value
        }
    }

    LaunchedEffect(currentLabel) {
        visibleDateLabelState.value = currentLabel
    }

    LaunchedEffect(displayItems, hasReachedEarliest) {
        if (hasReachedEarliest) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .filter { it <= 5 }
            .collect { onLoadMore() }
    }

    detailCell?.let { cell ->
        BehaviorDetailDialog(cell = cell, onDismiss = { detailCell = null })
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alphaState.value },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp + LocalImmersiveTopPadding.current, bottom = 180.dp
        ),
    ) {
        if (header != null) {
            item(key = "header", contentType = "header") {
                header()
            }
        }
        if (isLoadingMore) item("loading-top") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        items(
            items = displayItems,
            key = { it.key },
            contentType = {
                when (it) {
                    is MomentDisplayItem.Divider -> "divider"
                    is MomentDisplayItem.Behavior -> "behavior"
                }
            }
        ) { item ->
            when (item) {
                is MomentDisplayItem.Divider -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is MomentDisplayItem.Behavior -> MomentBehaviorItem(
                    behavior = item.cell,
                    onClick = { detailCell = item.cell },
                    onLongClick = { onCellLongClick(item.cell) },
                    logStyle = LogLayoutStyle(cardPadding = momentStyle.cardPadding),
                )
            }
        }
    }
}
