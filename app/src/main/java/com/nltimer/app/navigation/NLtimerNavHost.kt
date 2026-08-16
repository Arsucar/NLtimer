package com.nltimer.app.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nltimer.feature.ai.navigation.aiNavGraph
import com.nltimer.feature.categories.ui.CategoriesRoute
import com.nltimer.feature.home.ui.HomeRoute
import com.nltimer.feature.management_activities.ui.ActivityManagementRoute
import com.nltimer.feature.settings.ui.ColorPaletteRoute
import com.nltimer.feature.settings.ui.DialogConfigRoute
import com.nltimer.feature.settings.ui.HomeLayoutConfigRoute
import com.nltimer.feature.settings.ui.IconMissLogRoute
import com.nltimer.feature.settings.ui.LogListRoute
import com.nltimer.feature.settings.ui.AdvancedSettingsRoute
import com.nltimer.feature.settings.ui.SettingsRoute
import com.nltimer.feature.settings.ui.ThemeSettingsRoute
import com.nltimer.feature.stats.ui.StatsRoute
import com.nltimer.feature.behavior_management.ui.BehaviorManagementRoute
import com.nltimer.feature.settings.ui.DataManagementRoute
import com.nltimer.feature.tag_management.ui.TagManagementRoute

/**
 * 导航宿主 Composable
 * 注册所有应用页面的路由，包括 feature 模块的页面路由和 debug 模块的动态路由
 *
 * @param navController 导航控制器
 * @param modifier Modifier 修饰符
 */
@Composable
fun NLtimerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    timeLabelSettingsRequestKey: Int = 0,
    onTimeLabelSettingsShown: () -> Unit = {},
    drawerState: DrawerState? = null,
) {
    NavHost(
        navController = navController,
        startDestination = NLtimerRoutes.HOME,
        modifier = modifier,
    ) {
        composable(NLtimerRoutes.HOME) {
            HomeRoute(
                timeLabelSettingsRequestKey = timeLabelSettingsRequestKey,
                onTimeLabelSettingsShown = onTimeLabelSettingsShown,
            )
        }
        aiNavGraph(navController)
        composable(NLtimerRoutes.STATS) { StatsRoute() }
        composable(NLtimerRoutes.CATEGORIES) { CategoriesRoute() }
        composable(NLtimerRoutes.MANAGEMENT_ACTIVITIES) { ActivityManagementRoute() }
        composable(NLtimerRoutes.TAG_MANAGEMENT) {
            TagManagementRoute(
                _onNavigateBack = { navController.popBackStack() },
            )
        }
        slideComposable(NLtimerRoutes.BEHAVIOR_MANAGEMENT) {
            BehaviorManagementRoute(
                _onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NLtimerRoutes.SETTINGS) {
            SettingsRoute(
                onNavigateToThemeSettings = { navController.navigate(NLtimerRoutes.THEME_SETTINGS) },
                onNavigateToDialogConfig = { navController.navigate(NLtimerRoutes.DIALOG_CONFIG) },
                onNavigateToDataManagement = { navController.navigate(NLtimerRoutes.DATA_MANAGEMENT) },
                onNavigateToHomeLayoutConfig = { navController.navigate(NLtimerRoutes.HOME_LAYOUT_CONFIG) },
                onNavigateToColorPalette = { navController.navigate(NLtimerRoutes.COLOR_PALETTE) },
                onNavigateToAdvancedSettings = { navController.navigate(NLtimerRoutes.ADVANCED_SETTINGS) },
            )
        }
        slideComposable(NLtimerRoutes.THEME_SETTINGS) {
            ThemeSettingsRoute()
        }
        slideComposable(NLtimerRoutes.DIALOG_CONFIG) {
            DialogConfigRoute()
        }
        slideComposable(NLtimerRoutes.DATA_MANAGEMENT) {
            DataManagementRoute(
                _onNavigateBack = { navController.popBackStack() },
                onNavigateToBehaviorManagement = { navController.navigate(NLtimerRoutes.BEHAVIOR_MANAGEMENT) },
            )
        }
        slideComposable(NLtimerRoutes.HOME_LAYOUT_CONFIG) {
            HomeLayoutConfigRoute()
        }
        slideComposable(NLtimerRoutes.COLOR_PALETTE) {
            ColorPaletteRoute()
        }
        slideComposable(NLtimerRoutes.ADVANCED_SETTINGS) {
            AdvancedSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogList = { navController.navigate(NLtimerRoutes.LOG_LIST) },
            )
        }
        slideComposable(NLtimerRoutes.LOG_LIST) {
            LogListRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIconMissLog = { navController.navigate(NLtimerRoutes.ICON_MISS_LOG) },
            )
        }
        slideComposable(NLtimerRoutes.ICON_MISS_LOG) {
            IconMissLogRoute(_onNavigateBack = { navController.popBackStack() })
        }
        debugRoutes?.invoke(this)
    }
}

/**
 * 注册带水平滑入/滑出过渡的路由
 * 设置类子页面统一使用此过渡，避免重复编写 4 个 transition lambda
 */
private fun NavGraphBuilder.slideComposable(
    route: String,
    content: @Composable () -> Unit,
) {
    composable(
        route,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        content()
    }
}

// debug 模块通过此变量动态注入路由，release 构建保持 null
internal var debugRoutes: (NavGraphBuilder.() -> Unit)? = null
