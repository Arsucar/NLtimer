package com.nltimer.app.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nltimer.feature.categories.ui.CategoriesRoute
import com.nltimer.feature.home.ui.HomeRoute
import com.nltimer.feature.management_activities.ui.ActivityManagementRoute
import com.nltimer.feature.settings.ui.ColorPaletteRoute
import com.nltimer.feature.settings.ui.DialogConfigRoute
import com.nltimer.feature.settings.ui.HomeLayoutConfigRoute
import com.nltimer.feature.settings.ui.SettingsRoute
import com.nltimer.feature.settings.ui.ThemeSettingsRoute
import com.nltimer.app.experimental.ai_inter.AiInterRoute
import com.nltimer.app.experimental.ai_inter.AiProviderConfigRoute
import com.nltimer.app.experimental.ai_inter.AiToolsListRoute
import com.nltimer.app.experimental.ai_inter.AiCallLogsRoute
import com.nltimer.app.experimental.ai_inter.AiPromptConfigRoute
import com.nltimer.app.experimental.ai_inter.AiTestChatRoute
import com.nltimer.app.experimental.ai_inter.AiLogDetailRoute
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
) {
    NavHost(
        navController = navController,
        startDestination = NLtimerRoutes.HOME,
        modifier = modifier,
    ) {
        composable(NLtimerRoutes.HOME) { HomeRoute() }
        composable(NLtimerRoutes.AI_INTER) { AiInterRoute(navController) }
        composable(NLtimerRoutes.AI_PROVIDER_CONFIG) { AiProviderConfigRoute() }
        composable(NLtimerRoutes.AI_TOOLS_LIST) { AiToolsListRoute() }
        composable(NLtimerRoutes.AI_CALL_LOGS) {
            AiCallLogsRoute(
                onNavigateToLogDetail = { logId ->
                    navController.navigate(NLtimerRoutes.aiCallLogDetail(logId))
                }
            )
        }
        composable(NLtimerRoutes.AI_PROMPT_CONFIG) { AiPromptConfigRoute() }
        composable(NLtimerRoutes.AI_TEST_CHAT) { AiTestChatRoute() }
        composable(
            route = NLtimerRoutes.AI_CALL_LOG_DETAIL_PATTERN,
            arguments = listOf(navArgument("logId") { type = NavType.LongType })
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: 0L
            AiLogDetailRoute(
                logId = logId,
                onBackClick = { navController.popBackStack() }
            )
        }
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
        debugRoutes?.invoke(this)
    }
}

// debug 模块通过此变量动态注入路由，release 构建保持 null
internal var debugRoutes: (NavGraphBuilder.() -> Unit)? = null
