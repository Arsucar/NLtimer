package com.nltimer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nltimer.app.component.AppBottomNavigation
import com.nltimer.app.component.AppCollapsedTopAppBar
import com.nltimer.app.component.AppDrawer
import com.nltimer.app.component.AppCenterFabBottomBar
import com.nltimer.app.component.AppFloatingBottomBar
import com.nltimer.app.component.AppTopAppBar
import com.nltimer.app.component.MomentFilterOption
import com.nltimer.app.component.MomentSortOption
import com.nltimer.app.component.RouteSettingsPopup
import com.nltimer.app.navigation.NLtimerNavHost
import com.nltimer.app.navigation.NLtimerRoutes
import com.nltimer.app.viewmodel.DrawerViewModel
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.DisplayColorConfig
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.TagDisplayConfig
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.nltimer.core.designsystem.theme.DisplayColorMode
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.core.designsystem.theme.TopBarMode
import com.nltimer.core.designsystem.theme.toDisplayString
import com.nltimer.feature.ai.navigation.AiRoutes
import com.nltimer.feature.home.ui.components.LayoutConfigDialog
import com.nltimer.feature.home.ui.components.FocusCardConfigDialog
import com.nltimer.feature.home.ui.components.TagDisplayConfigDialog
import com.nltimer.feature.home.ui.components.LocalMomentFilterState
import com.nltimer.feature.home.ui.components.LocalVisibleDateLabel
import com.nltimer.feature.home.ui.components.MomentFilterState
import com.nltimer.feature.settings.ui.DialogConfigViewModel
import com.nltimer.feature.settings.ui.ThemeSettingsViewModel
import kotlinx.coroutines.launch

private val MomentFilterOptions = listOf(
    MomentFilterOption("乃大", "ALL"),
    MomentFilterOption("曾经", "COMPLETED"),
    MomentFilterOption("此后", "PENDING"),
)

private val MomentSortOptions = listOf(
    MomentSortOption("时间反", "TIME_DESC"),
    MomentSortOption("时间正", "TIME_ASC"),
    MomentSortOption("用时", "DURATION"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NLtimerScaffold(
    navController: NavHostController,
    settingsPrefs: SettingsPrefs,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()
    val isSecondaryPage = currentRoute in NLtimerRoutes.SETTINGS_FULLSCREEN_ROUTES
    val isAiInter = currentRoute == AiRoutes.AI_INTER
    val isAiAssistantChat = currentRoute == AiRoutes.AI_ASSISTANT_CHAT
    val isStats = currentRoute == NLtimerRoutes.STATS
    val visibleDateLabelState = remember { mutableStateOf<String?>(null) }
    // 离开主页后清除残留日期标签，避免管理页/子页标题显示过期日期
    LaunchedEffect(currentRoute) {
        if (currentRoute != NLtimerRoutes.HOME) {
            visibleDateLabelState.value = null
        }
    }
    val isHomePage = currentRoute !in NLtimerRoutes.SETTINGS_FULLSCREEN_ROUTES && currentRoute != NLtimerRoutes.SETTINGS && !isAiInter && !isStats && !isAiAssistantChat
    val isDateTitle = isHomePage && visibleDateLabelState.value != null
    val topBarTitle = when (currentRoute) {
        NLtimerRoutes.SETTINGS -> "设置"
        NLtimerRoutes.THEME_SETTINGS -> "主题配置"
        NLtimerRoutes.DIALOG_CONFIG -> "弹窗配置"
        NLtimerRoutes.BEHAVIOR_MANAGEMENT -> "行为管理"
        NLtimerRoutes.CATEGORIES -> "分类管理"
        NLtimerRoutes.DATA_MANAGEMENT -> "数据管理"
        NLtimerRoutes.HOME_LAYOUT_CONFIG -> "主页布局配置"
        NLtimerRoutes.COLOR_PALETTE -> "色板"
        NLtimerRoutes.ICON_MISS_LOG -> "图标库"
        NLtimerRoutes.ADVANCED_SETTINGS -> "高级"
        NLtimerRoutes.LOG_LIST -> "日志记录"
        AiRoutes.AI_INTER -> "AI Inter"
        NLtimerRoutes.STATS -> "统计"
        else -> visibleDateLabelState.value ?: "NLtimer"
    }
    var showLayoutPopup by remember { mutableStateOf(false) }
    var showLayoutConfigDialog by remember { mutableStateOf(false) }
    var showTagDisplayConfigDialog by remember { mutableStateOf(false) }
    var showFocusCardConfigDialog by remember { mutableStateOf(false) }
    var timeLabelSettingsRequestKey by remember { mutableStateOf(0) }
    var momentFilterKey by remember { mutableStateOf("ALL") }
    var momentSortKey by remember { mutableStateOf("TIME_DESC") }
    val theme = LocalTheme.current
    val displayColorConfig by settingsPrefs.getDisplayColorConfigFlow()
        .collectAsStateWithLifecycle(initialValue = DisplayColorConfig())
    val tagDisplayConfig by settingsPrefs.getTagDisplayConfigFlow()
        .collectAsStateWithLifecycle(initialValue = TagDisplayConfig())
    val focusCardConfig by settingsPrefs.getFocusCardConfigFlow()
        .collectAsStateWithLifecycle(initialValue = FocusCardConfig())
    val themeViewModel: ThemeSettingsViewModel = hiltViewModel()
    val dialogConfigViewModel: DialogConfigViewModel = hiltViewModel()
    val homeLayoutConfig by dialogConfigViewModel.homeLayoutConfig.collectAsStateWithLifecycle()
    val drawerViewModel: DrawerViewModel = hiltViewModel()
    val totalDurationMs by drawerViewModel.totalDurationMs.collectAsStateWithLifecycle()
    val topBarHazeState = rememberHazeState()
    val momentFilterLabel = if (isHomePage && theme.homeLayout == HomeLayout.MOMENT) {
        val filterLabel = MomentFilterOptions.firstOrNull { it.key == momentFilterKey }?.label ?: ""
        val sortLabel = MomentSortOptions.firstOrNull { it.key == momentSortKey }?.label ?: ""
        "$filterLabel · $sortLabel"
    } else null
    val layoutLabel = if (isHomePage) theme.homeLayout.toDisplayString() else null
    val useCollapsed = theme.topBarMode == TopBarMode.COLLAPSED && (!isSecondaryPage || isAiInter) && !isAiAssistantChat
    val isImmersive = theme.isImmersive && !isSecondaryPage
    val topBarScrollBehavior = if (useCollapsed) {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    } else {
        null
    }

    val momentFilterState = remember(momentFilterKey, momentSortKey) {
        MomentFilterState(
            filterKey = momentFilterKey,
            sortKey = momentSortKey,
            onFilterChange = { momentFilterKey = it },
            onSortChange = { momentSortKey = it },
        )
    }
    val layoutConfigLabel = when (theme.homeLayout) {
        HomeLayout.GRID -> "网格设置"
        HomeLayout.TIMELINE_REVERSE -> "时间轴设置"
        HomeLayout.LOG -> "日志设置"
        HomeLayout.MOMENT -> "当前时刻设置"
        HomeLayout.TEXT_LIST -> "纯文字列表设置"
    }
    val settingsDragOptions = remember(
        currentRoute,
        theme.homeLayout,
        theme.showTimeSideBar,
        displayColorConfig,
        tagDisplayConfig,
        layoutConfigLabel,
    ) {
        buildList {
            if (currentRoute == NLtimerRoutes.HOME) {
                add("更改布局")
                add(layoutConfigLabel)
                if (theme.homeLayout == HomeLayout.GRID) {
                    add(if (theme.showTimeSideBar) "关闭侧边时间轴" else "开启侧边时间轴")
                    add("时间标签设置")
                }
                add("标签配置")
                add("专注卡片配置")
            }
            if (currentRoute == NLtimerRoutes.MANAGEMENT_ACTIVITIES) {
                val mode = displayColorConfig.activityIconColorMode
                add(if (mode == DisplayColorMode.BACKGROUND) "✓ 图标：背景色" else "图标：背景色")
                add(if (mode == DisplayColorMode.TEXT) "✓ 图标：文字色" else "图标：文字色")
                add(if (mode == DisplayColorMode.NORMAL) "✓ 图标：正常" else "图标：正常")
            }
            if (currentRoute == NLtimerRoutes.TAG_MANAGEMENT) {
                val mode = displayColorConfig.tagDisplayColorMode
                add(if (mode == DisplayColorMode.BACKGROUND) "✓ 标签：背景色" else "标签：背景色")
                add(if (mode == DisplayColorMode.TEXT) "✓ 标签：文字色" else "标签：文字色")
                add(if (mode == DisplayColorMode.NORMAL) "✓ 标签：正常" else "标签：正常")
                add(if (displayColorConfig.showTagIcon) "✓ 显示图标" else "显示图标")
            }
        }
    }
    fun navigateToRoute(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun handleSettingsDragOption(option: String) {
        when (option) {
            "更改布局" -> showLayoutPopup = true
            "开启侧边时间轴" -> themeViewModel.onShowTimeSideBarToggle(true)
            "关闭侧边时间轴" -> themeViewModel.onShowTimeSideBarToggle(false)
            "时间标签设置" -> timeLabelSettingsRequestKey += 1
            layoutConfigLabel -> showLayoutConfigDialog = true
        }
        when {
            option == "图标：背景色" || option == "✓ 图标：背景色" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(activityIconColorMode = DisplayColorMode.BACKGROUND)
                )
            }
            option == "图标：文字色" || option == "✓ 图标：文字色" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(activityIconColorMode = DisplayColorMode.TEXT)
                )
            }
            option == "图标：正常" || option == "✓ 图标：正常" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(activityIconColorMode = DisplayColorMode.NORMAL)
                )
            }
            option == "标签：背景色" || option == "✓ 标签：背景色" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(tagDisplayColorMode = DisplayColorMode.BACKGROUND)
                )
            }
            option == "标签：文字色" || option == "✓ 标签：文字色" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(tagDisplayColorMode = DisplayColorMode.TEXT)
                )
            }
            option == "标签：正常" || option == "✓ 标签：正常" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(tagDisplayColorMode = DisplayColorMode.NORMAL)
                )
            }
            option == "显示图标" || option == "✓ 显示图标" -> scope.launch {
                settingsPrefs.updateDisplayColorConfig(
                    displayColorConfig.copy(showTagIcon = !displayColorConfig.showTagIcon)
                )
            }
            option == "标签配置" -> showTagDisplayConfigDialog = true
            option == "专注卡片配置" -> showFocusCardConfigDialog = true
        }
    }

    CompositionLocalProvider(
        LocalMomentFilterState provides momentFilterState,
        LocalVisibleDateLabel provides visibleDateLabelState,
    ) {
        ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController = navController,
                onClose = { scope.launch { drawerState.close() } },
                totalDurationMs = totalDurationMs,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val isFloating = theme.bottomBarMode == BottomBarMode.FLOATING && !isSecondaryPage
            val isCenterFab = theme.bottomBarMode == BottomBarMode.CENTER_FAB && !isSecondaryPage
            val isAnyFloating = isFloating || isCenterFab

            Scaffold(
                modifier = Modifier.then(
                    if (!isSecondaryPage && !isAnyFloating) Modifier.padding(bottom = 80.dp) else Modifier
                ).then(
                    if (topBarScrollBehavior != null)
                        Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                    else Modifier
                ),
                containerColor = Color.Transparent,
                topBar = {
                    if (isAiAssistantChat) {
                        // AiAssistantChatRoute has its own Scaffold + TopBar
                    } else if (isSecondaryPage && !isAiInter) {
                        TopAppBar(
                            title = { Text(topBarTitle) },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                    )
                                }
                            },
                        )
                    } else if (topBarScrollBehavior != null) {
                        AppCollapsedTopAppBar(
                            title = topBarTitle,
                            isDateTitle = isDateTitle,
                            scrollBehavior = topBarScrollBehavior,
                            hazeState = if (theme.topBarHaze) topBarHazeState else null,
                            navigationIcon = if (isAiInter) {
                                {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "返回",
                                        )
                                    }
                                }
                            } else { {} },
                            layoutLabel = layoutLabel,
                            onLayoutChange = if (isHomePage) { { themeViewModel.onHomeLayoutChange(it) } } else null,
                            momentFilterLabel = momentFilterLabel,
                            momentFilterOptions = MomentFilterOptions,
                            momentFilterKey = momentFilterKey,
                            onMomentFilterChange = { momentFilterKey = it },
                            momentSortOptions = MomentSortOptions,
                            momentSortKey = momentSortKey,
                            onMomentSortChange = { momentSortKey = it },
                        )
                    } else {
                        AppTopAppBar(
                            title = topBarTitle,
                            isDateTitle = isDateTitle,
                            hazeState = if (theme.topBarHaze) topBarHazeState else null,
                            navigationIcon = if (isAiInter) {
                                {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "返回",
                                        )
                                    }
                                }
                            } else { {} },
                            layoutLabel = layoutLabel,
                            onLayoutChange = if (isHomePage) { { themeViewModel.onHomeLayoutChange(it) } } else null,
                            momentFilterLabel = momentFilterLabel,
                            momentFilterOptions = MomentFilterOptions,
                            momentFilterKey = momentFilterKey,
                            onMomentFilterChange = { momentFilterKey = it },
                            momentSortOptions = MomentSortOptions,
                            momentSortKey = momentSortKey,
                            onMomentSortChange = { momentSortKey = it },
                        )
                    }
                },
            ) { padding ->
                val immersiveTopPadding = if (isImmersive || isStats) padding.calculateTopPadding() else 0.dp
                CompositionLocalProvider(LocalImmersiveTopPadding provides immersiveTopPadding) {
                    NLtimerNavHost(
                        navController = navController,
                        timeLabelSettingsRequestKey = timeLabelSettingsRequestKey,
                        onTimeLabelSettingsShown = { timeLabelSettingsRequestKey = 0 },
                        drawerState = drawerState,
                        modifier = Modifier
                            .then(if (theme.topBarHaze) Modifier.hazeSource(state = topBarHazeState) else Modifier)
                            .fillMaxSize()
                            .padding(
                                top = if (isImmersive || isStats) 0.dp else if (isAiAssistantChat) 0.dp else padding.calculateTopPadding(),
                                bottom = if (isAnyFloating) 0.dp else if (!isSecondaryPage) padding.calculateBottomPadding() else 0.dp,
                            ),
                    )
                }
            }

            if (!isAnyFloating && !isSecondaryPage) {
                AppBottomNavigation(
                    navController = navController,
                    onSettingsClick = { navigateToRoute(NLtimerRoutes.SETTINGS) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (isFloating) {
                AppFloatingBottomBar(
                    navController = navController,
                    onSettingsClick = { navigateToRoute(NLtimerRoutes.SETTINGS) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (isCenterFab) {
                AppCenterFabBottomBar(
                    navController = navController,
                    onSettingsClick = { navigateToRoute(NLtimerRoutes.SETTINGS) },
                    settingsDragOptions = settingsDragOptions,
                    onSettingsDragOptionSelected = { handleSettingsDragOption(it) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (showLayoutPopup) {
                RouteSettingsPopup(
                    currentRoute = currentRoute,
                    navController = navController,
                    onDismiss = {
                        showLayoutPopup = false
                    },
                    onHomeLayoutChange = { themeViewModel.onHomeLayoutChange(it) },
                    onShowTimeSideBarChange = { themeViewModel.onShowTimeSideBarToggle(it) },
                    popupOffsetY = if (isAnyFloating) -300 else -260,
                    initialShowLayoutOptions = showLayoutPopup,
                )
            }

            if (showLayoutConfigDialog) {
                LayoutConfigDialog(
                    layout = theme.homeLayout,
                    config = homeLayoutConfig,
                    onConfigChange = { dialogConfigViewModel.updateHomeLayoutConfig(it) },
                    onDismiss = { showLayoutConfigDialog = false },
                )
            }

            if (showTagDisplayConfigDialog) {
                TagDisplayConfigDialog(
                    config = tagDisplayConfig,
                    onConfigChange = { scope.launch { settingsPrefs.updateTagDisplayConfig(it) } },
                    onDismiss = { showTagDisplayConfigDialog = false },
                )
            }

            if (showFocusCardConfigDialog) {
                FocusCardConfigDialog(
                    config = focusCardConfig,
                    onConfigChange = { scope.launch { settingsPrefs.updateFocusCardConfig(it) } },
                    onDismiss = { showFocusCardConfigDialog = false },
                )
            }
        }
    }
    }
}
