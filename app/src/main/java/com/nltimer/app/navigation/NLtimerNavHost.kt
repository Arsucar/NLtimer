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
        composable(NLtimerRoutes.HOME) { HomeRoute(timeLabelSettingsRequestKey = timeLabelSettingsRequestKey, onTimeLabelSettingsShown = onTimeLabelSettingsShown) }
        aiNavGraph(navController)
        composable(NLtimerRoutes.STATS) { StatsRoute() }
        composable(NLtimerRoutes.CATEGORIES) { CategoriesRoute() }
        composable(NLtimerRoutes.MANAGEMENT_ACTIVITIES) { ActivityManagementRoute() }
        composable(NLtimerRoutes.TAG_MANAGEMENT) {
            TagManagementRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            NLtimerRoutes.BEHAVIOR_MANAGEMENT,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            BehaviorManagementRoute(
                onNavigateBack = { navController.popBackStack() },
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
        composable(
            NLtimerRoutes.THEME_SETTINGS,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            ThemeSettingsRoute()
        }
        composable(
            NLtimerRoutes.DIALOG_CONFIG,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            DialogConfigRoute()
        }
        composable(
            NLtimerRoutes.DATA_MANAGEMENT,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            DataManagementRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBehaviorManagement = { navController.navigate(NLtimerRoutes.BEHAVIOR_MANAGEMENT) },
            )
        }
        composable(
            NLtimerRoutes.HOME_LAYOUT_CONFIG,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            HomeLayoutConfigRoute()
        }
        composable(
            NLtimerRoutes.COLOR_PALETTE,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            ColorPaletteRoute()
        }
        composable(
            NLtimerRoutes.ADVANCED_SETTINGS,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            AdvancedSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogList = { navController.navigate(NLtimerRoutes.LOG_LIST) },
            )
        }
        composable(
            NLtimerRoutes.LOG_LIST,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            LogListRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIconMissLog = { navController.navigate(NLtimerRoutes.ICON_MISS_LOG) },
            )
        }
        composable(
            NLtimerRoutes.ICON_MISS_LOG,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            IconMissLogRoute(onNavigateBack = { navController.popBackStack() })
        }
        debugRoutes?.invoke(this)
    }
}

// debug 模块通过此变量动态注入路由，release 构建保持 null
internal var debugRoutes: (NavGraphBuilder.() -> Unit)? = null
