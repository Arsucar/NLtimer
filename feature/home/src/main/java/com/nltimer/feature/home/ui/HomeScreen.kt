package com.nltimer.feature.home.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.AddActivityCallback
import com.nltimer.core.data.model.AddTagCallback
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.data.model.Tag
import com.nltimer.core.designsystem.component.BottomBarDragFab
import com.nltimer.core.designsystem.component.LoadingScreen
import com.nltimer.core.designsystem.component.rememberDragFabState
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.core.designsystem.theme.NLtimerTheme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.tools.match.NoteProcessOutcome
import com.nltimer.core.tools.match.NoteScanResult
import com.nltimer.feature.home.model.AddSheetMode
import com.nltimer.feature.home.model.GridCellUiState
import com.nltimer.feature.home.model.GridDaySection
import com.nltimer.feature.home.model.GridRowUiState
import com.nltimer.feature.home.model.HomeUiState
import com.nltimer.feature.home.model.TagUiState
import com.nltimer.feature.home.ui.components.BehaviorLogView
import com.nltimer.feature.home.ui.components.MomentFocusCard
import com.nltimer.feature.home.ui.components.MomentView
import com.nltimer.feature.home.ui.components.TimeAxisGrid
import com.nltimer.feature.home.ui.components.TimeLabelSettingsDialog
import com.nltimer.feature.home.ui.components.TimeSideBar
import com.nltimer.feature.home.ui.components.TimelineReverseView
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.collections.immutable.persistentListOf

private val DragOptionsWithActive = listOf("完成", "放弃", "特记", "+自定义")
private val DragOptionsWithoutActive = listOf("完成", "目标", "当前", "+自定义")

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    dialogConfig: DialogGridConfig = DialogGridConfig(),
    homeLayoutConfig: HomeLayoutConfig = HomeLayoutConfig(),
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onShowAddSheet: (AddSheetMode) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit,
    onAddBehavior: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onDismissSheet: () -> Unit,
    onCompleteBehavior: (Long) -> Unit,
    onToggleIdleMode: () -> Unit,
    onStartNextPending: () -> Unit,
    onStartBehavior: (Long) -> Unit,
    onAddActivity: AddActivityCallback,
    onAddTag: AddTagCallback,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onHourClick: (Int) -> Unit,
    onLoadMore: () -> Unit = {},
    timeLabelConfig: TimeLabelConfig = TimeLabelConfig(),
    onTimeLabelConfigChange: (TimeLabelConfig) -> Unit = {},
    onHomeLayoutConfigChange: (HomeLayoutConfig) -> Unit = {},
    onHomeLayoutChange: (HomeLayout) -> Unit = {},
    onMatchNote: (String) -> NoteScanResult = { NoteScanResult(null, emptySet()) },
    onProcessNote: suspend (String) -> NoteProcessOutcome = { NoteProcessOutcome.Empty },
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    val layout = theme.homeLayout
    var showTimeLabelSettings by remember { mutableStateOf(false) }

    val activeCell by remember(uiState.momentCells) {
        derivedStateOf {
            uiState.momentCells.firstOrNull {
                it.isCurrent && it.behaviorId != null && it.status == BehaviorNature.ACTIVE
            }
        }
    }
    val nextPendingCell by remember(uiState.momentCells) {
        derivedStateOf {
            uiState.momentCells.firstOrNull { it.behaviorId != null && it.status == BehaviorNature.PENDING }
        }
    }
    val activeBehaviorId = activeCell?.behaviorId

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    val dragFabState = rememberDragFabState()
    val dragOptions = if (uiState.hasActiveBehavior) DragOptionsWithActive else DragOptionsWithoutActive

    Box(
        modifier = modifier.onGloballyPositioned { dragFabState.boxPositionInWindow = it.positionInWindow() }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (uiState.isLoading) {
                LoadingScreen()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    HomeLayoutContent(
                        layout = layout,
                        uiState = uiState,
                        activeCell = activeCell,
                        nextPendingCell = nextPendingCell,
                        onEmptyCellClick = onEmptyCellClick,
                        onCellLongClick = onCellLongClick,
                        onHourClick = onHourClick,
                        onCompleteBehavior = onCompleteBehavior,
                        onStartNextPending = onStartNextPending,
                        onStartBehavior = onStartBehavior,
                        onLoadMore = onLoadMore,
                        timeLabelConfig = timeLabelConfig,
                        onTimeLabelSettingsClick = { showTimeLabelSettings = true },
                        homeLayoutConfig = homeLayoutConfig,
                        onHomeLayoutChange = onHomeLayoutChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HomeSheetRouter(
                uiState = uiState,
                activities = activities,
                activityGroups = activityGroups,
                allTags = allTags,
                dialogConfig = dialogConfig,
                activityLastUsedMap = activityLastUsedMap,
                tagLastUsedMap = tagLastUsedMap,
                tagCategoryOrder = tagCategoryOrder,
                onDismissSheet = onDismissSheet,
                onAddBehavior = onAddBehavior,
                onAddActivity = onAddActivity,
                onAddTag = onAddTag,
                onActivityGroupsReordered = onActivityGroupsReordered,
                onTagCategoriesReordered = onTagCategoriesReordered,
                onMatchNote = onMatchNote,
                onProcessNote = onProcessNote,
            )
        }

        BottomBarDragFab(
            state = dragFabState,
            icon = if (uiState.hasActiveBehavior) Icons.Default.Check else Icons.Default.Add,
            dragOptions = dragOptions,
            label = if (uiState.hasActiveBehavior) "完成行为" else null,
            containerColor = if (uiState.hasActiveBehavior) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (uiState.hasActiveBehavior) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            cornerRadius = if (uiState.hasActiveBehavior) 16.dp else 28.dp,
            onClick = if (uiState.hasActiveBehavior) {
                { activeBehaviorId?.let { onCompleteBehavior(it) } }
            } else {
                { onEmptyCellClick(null, null) }
            },
            onOptionSelected = { option ->
                when (option) {
                    "完成" -> {
                        if (uiState.hasActiveBehavior) {
                            activeBehaviorId?.let { onCompleteBehavior(it) }
                        } else {
                            onShowAddSheet(AddSheetMode.COMPLETED)
                        }
                    }
                    "当前" -> onShowAddSheet(AddSheetMode.CURRENT)
                    "目标" -> onShowAddSheet(AddSheetMode.TARGET)
                    else -> Toast.makeText(context, "触发功能: $option", Toast.LENGTH_SHORT).show()
                }
            },
        )

        if (showTimeLabelSettings) {
            TimeLabelSettingsDialog(
                config = timeLabelConfig,
                onConfigChange = onTimeLabelConfigChange,
                onDismiss = { showTimeLabelSettings = false },
            )
        }
    }
}

@Composable
private fun HomeLayoutContent(
    layout: HomeLayout,
    uiState: HomeUiState,
    activeCell: GridCellUiState?,
    nextPendingCell: GridCellUiState?,
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit,
    onHourClick: (Int) -> Unit,
    onCompleteBehavior: (Long) -> Unit,
    onStartNextPending: () -> Unit,
    onStartBehavior: (Long) -> Unit,
    onLoadMore: () -> Unit,
    timeLabelConfig: TimeLabelConfig,
    onTimeLabelSettingsClick: () -> Unit,
    homeLayoutConfig: HomeLayoutConfig = HomeLayoutConfig(),
    onHomeLayoutChange: (HomeLayout) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var swipeDirection by remember { mutableStateOf(0) } // 1 for next (left swipe), -1 for prev (right swipe)
    val animOffsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val swipeThreshold = remember { with(density) { 56.dp.toPx() } }

    val handleSwipe = { direction: Int ->
        swipeDirection = direction
        val nextLayout = if (direction > 0) {
            when (layout) {
                HomeLayout.GRID -> HomeLayout.TIMELINE_REVERSE
                HomeLayout.TIMELINE_REVERSE -> HomeLayout.LOG
                HomeLayout.LOG -> HomeLayout.MOMENT
                HomeLayout.MOMENT -> HomeLayout.GRID
            }
        } else {
            when (layout) {
                HomeLayout.GRID -> HomeLayout.MOMENT
                HomeLayout.MOMENT -> HomeLayout.LOG
                HomeLayout.LOG -> HomeLayout.TIMELINE_REVERSE
                HomeLayout.TIMELINE_REVERSE -> HomeLayout.GRID
            }
        }
        onHomeLayoutChange(nextLayout)
        // 切换后立即重置动画位移，避免影响新视图
        scope.launch { animOffsetX.snapTo(0f) }
    }

    val focusCard = @Composable {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .graphicsLayer {
                    // 应用阻尼效果的位移 (x * 0.4 产生阻力感)
                    translationX = animOffsetX.value * 0.4f
                }
                .pointerInput(layout) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val finalOffset = animOffsetX.value
                            scope.launch {
                                if (finalOffset < -swipeThreshold) {
                                    handleSwipe(1)
                                } else if (finalOffset > swipeThreshold) {
                                    handleSwipe(-1)
                                }
                                // 无论是否切换，都执行弹性回弹
                                animOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.7f,
                                        stiffness = 500f
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animOffsetX.animateTo(0f, spring(0.7f, 500f))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                animOffsetX.snapTo(animOffsetX.value + dragAmount)
                            }
                        }
                    )
                }
        ) {
            MomentFocusCard(
                activeCell = activeCell,
                nextPendingCell = nextPendingCell,
                onCompleteBehavior = onCompleteBehavior,
                onStartNextPending = onStartNextPending,
                onStartBehavior = onStartBehavior,
                onEmptyCellClick = { onEmptyCellClick(null, null) },
                momentStyle = homeLayoutConfig.moment,
            )
        }
    }

    AnimatedContent(
        targetState = layout,
        transitionSpec = {
            if (swipeDirection > 0) {
                // 左滑 -> 内容从右侧滑入
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else if (swipeDirection < 0) {
                // 右滑 -> 内容从左侧滑入
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            } else {
                // 默认/点击切换
                val initialIndex = initialState.ordinal
                val targetIndex = targetState.ordinal
                val isForwardCycle = (targetIndex == 0 && initialIndex == HomeLayout.entries.size - 1)
                if (targetIndex > initialIndex || isForwardCycle) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            }.using(SizeTransform(clip = false))
        },
        label = "HomeLayoutSwitch",
        modifier = modifier,
    ) { currentLayout ->
        when (currentLayout) {
            HomeLayout.GRID -> GridContent(
                uiState = uiState,
                onEmptyCellClick = onEmptyCellClick,
                onCellLongClick = onCellLongClick,
                onHourClick = onHourClick,
                onLoadMore = onLoadMore,
                timeLabelConfig = timeLabelConfig,
                onTimeLabelSettingsClick = onTimeLabelSettingsClick,
                gridStyle = homeLayoutConfig.grid,
                header = focusCard,
                modifier = Modifier.fillMaxSize(),
            )
            HomeLayout.TIMELINE_REVERSE -> TimelineReverseContent(
                uiState = uiState,
                onEmptyCellClick = onEmptyCellClick,
                onCellLongClick = onCellLongClick,
                onLoadMore = onLoadMore,
                timelineStyle = homeLayoutConfig.timeline,
                header = focusCard,
                modifier = Modifier.fillMaxSize(),
            )
            HomeLayout.LOG -> LogContent(
                uiState = uiState,
                onCellLongClick = onCellLongClick,
                onLoadMore = onLoadMore,
                logStyle = homeLayoutConfig.log,
                header = focusCard,
                modifier = Modifier.fillMaxSize(),
            )
            HomeLayout.MOMENT -> MomentContent(
                uiState = uiState,
                activeCell = activeCell,
                nextPendingCell = nextPendingCell,
                onEmptyCellClick = onEmptyCellClick,
                onCellLongClick = onCellLongClick,
                onCompleteBehavior = onCompleteBehavior,
                onStartNextPending = onStartNextPending,
                onStartBehavior = onStartBehavior,
                onLoadMore = onLoadMore,
                isLoadingMore = uiState.isLoadingMore,
                hasReachedEarliest = uiState.hasReachedEarliest,
                momentStyle = homeLayoutConfig.moment,
                header = focusCard,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun GridContent(
    uiState: HomeUiState,
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit,
    onHourClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    timeLabelConfig: TimeLabelConfig,
    onTimeLabelSettingsClick: () -> Unit,
    gridStyle: GridLayoutStyle = GridLayoutStyle(),
    header: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val showSideBar = LocalTheme.current.showTimeSideBar
    Row(modifier = modifier) {
        TimeAxisGrid(
            sections = uiState.gridSections,
            onEmptyCellClick = onEmptyCellClick,
            onCellLongClick = onCellLongClick,
            onLoadMore = onLoadMore,
            isLoadingMore = uiState.isLoadingMore,
            hasReachedEarliest = uiState.hasReachedEarliest,
            currentHour = uiState.selectedTimeHour,
            showTimeSideBar = showSideBar,
            timeLabelConfig = timeLabelConfig,
            onTimeLabelSettingsClick = onTimeLabelSettingsClick,
            gridStyle = gridStyle,
            header = header?.let { { it() } },
            modifier = Modifier.weight(1f),
        )
        if (showSideBar) {
            val activeHours by remember {
                derivedStateOf {
                    uiState.gridSections.firstOrNull()?.rows.orEmpty()
                        .filter { it.cells.any { cell -> cell.behaviorId != null } || it.isCurrentRow }
                        .map { it.startTime.hour }
                        .toSet()
                }
            }
            TimeSideBar(
                activeHours = activeHours,
                currentHour = uiState.selectedTimeHour,
                onHourClick = onHourClick,
            )
        }
    }
}

@Composable
private fun TimelineReverseContent(
    uiState: HomeUiState,
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit,
    onLoadMore: () -> Unit,
    timelineStyle: TimelineLayoutStyle = TimelineLayoutStyle(),
    header: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    TimelineReverseView(
        items = uiState.items,
        onAddClick = onEmptyCellClick,
        onCellLongClick = onCellLongClick,
        onLoadMore = onLoadMore,
        isLoadingMore = uiState.isLoadingMore,
        hasReachedEarliest = uiState.hasReachedEarliest,
        timelineStyle = timelineStyle,
        header = header?.let { { it() } },
        modifier = modifier,
    )
}

@Composable
private fun LogContent(
    uiState: HomeUiState,
    onCellLongClick: (GridCellUiState) -> Unit,
    onLoadMore: () -> Unit,
    logStyle: LogLayoutStyle = LogLayoutStyle(),
    header: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BehaviorLogView(
        items = uiState.items,
        onCellLongClick = onCellLongClick,
        onLoadMore = onLoadMore,
        isLoadingMore = uiState.isLoadingMore,
        hasReachedEarliest = uiState.hasReachedEarliest,
        logStyle = logStyle,
        header = header?.let { { it() } },
        modifier = modifier,
    )
}

@Composable
private fun MomentContent(
    uiState: HomeUiState,
    activeCell: GridCellUiState?,
    nextPendingCell: GridCellUiState?,
    onEmptyCellClick: (idleStart: LocalDateTime?, idleEnd: LocalDateTime?) -> Unit,
    onCellLongClick: (GridCellUiState) -> Unit,
    onCompleteBehavior: (Long) -> Unit,
    onStartNextPending: () -> Unit,
    onStartBehavior: (Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean = false,
    hasReachedEarliest: Boolean = false,
    momentStyle: MomentLayoutStyle = MomentLayoutStyle(),
    header: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    MomentView(
        cells = uiState.momentCells,
        hasActiveBehavior = uiState.hasActiveBehavior,
        activeBehaviorId = activeCell?.behaviorId,
        onCompleteBehavior = onCompleteBehavior,
        onStartNextPending = onStartNextPending,
        onStartBehavior = onStartBehavior,
        onEmptyCellClick = onEmptyCellClick,
        onCellLongClick = onCellLongClick,
        onLoadMore = onLoadMore,
        isLoadingMore = isLoadingMore,
        hasReachedEarliest = hasReachedEarliest,
        momentStyle = momentStyle,
        header = header?.let { { it() } },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val sampleTags = listOf(
        Tag(1, "Tag 1", null, null, null, 0, 0, 0, null, false),
        Tag(2, "Tag 2", null, null, null, 0, 0, 0, null, false)
    )
    val sampleActivities = listOf(
        Activity(1, "Activity 1", "😊"),
        Activity(2, "Activity 2", "🚀")
    )
    val sampleSection = GridDaySection(
        date = java.time.LocalDate.of(2026, 5, 13),
        label = "今天 5/13",
        rows = persistentListOf(
            GridRowUiState(
                rowId = "1",
                startTime = LocalTime.of(9, 0),
                isCurrentRow = true,
                isLocked = false,
                cells = persistentListOf(
                    GridCellUiState(
                        behaviorId = 1L,
                        activityIconKey = "😊",
                        activityName = "Activity 1",
                        tags = persistentListOf(TagUiState(1, "Tag 1", null)),
                        status = BehaviorNature.ACTIVE,
                        isCurrent = true,
                    )
                )
            )
        ),
    )
    val sampleUiState = HomeUiState(
        isLoading = false,
        gridSections = persistentListOf(sampleSection),
        selectedTimeHour = 9,
    )

    NLtimerTheme {
        HomeScreen(
            uiState = sampleUiState,
            activities = sampleActivities,
            activityGroups = emptyList(),
            allTags = sampleTags,
            onEmptyCellClick = { _, _ -> },
            onShowAddSheet = {},
            onCellLongClick = {},
            onAddBehavior = { _, _, _, _, _, _, _ -> },
            onDismissSheet = {},
            onCompleteBehavior = {},
            onToggleIdleMode = {},
            onStartNextPending = {},
            onStartBehavior = {},
            onAddActivity = { _, _, _, _, _, _ -> },
            onAddTag = { _, _, _, _, _, _, _ -> },
            onHourClick = {},
        )
    }
}
