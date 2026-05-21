package com.nltimer.feature.ai.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nltimer.feature.ai.AiInterRoute
import com.nltimer.feature.ai.AiProviderConfigRoute
import com.nltimer.feature.ai.AiToolsListRoute
import com.nltimer.feature.ai.AiCallLogsRoute
import com.nltimer.feature.ai.AiPromptConfigRoute
import com.nltimer.feature.ai.AiTestChatRoute
import com.nltimer.feature.ai.AiLogDetailRoute
import com.nltimer.feature.ai.chat.AiAssistantChatRoute

fun NavGraphBuilder.aiNavGraph(navController: NavHostController) {
    composable(AiRoutes.AI_INTER) { AiInterRoute(navController) }
    composable(AiRoutes.AI_PROVIDER_CONFIG) { AiProviderConfigRoute() }
    composable(AiRoutes.AI_TOOLS_LIST) { AiToolsListRoute() }
    composable(AiRoutes.AI_CALL_LOGS) {
        AiCallLogsRoute(
            onNavigateToLogDetail = { logId ->
                navController.navigate(AiRoutes.aiCallLogDetail(logId))
            }
        )
    }
    composable(AiRoutes.AI_PROMPT_CONFIG) { AiPromptConfigRoute() }
    composable(AiRoutes.AI_TEST_CHAT) { AiTestChatRoute() }
    composable(
        AiRoutes.AI_ASSISTANT_CHAT,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it } },
        popEnterTransition = { slideInHorizontally { -it } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        AiAssistantChatRoute(navController = navController)
    }
    composable(
        route = AiRoutes.AI_CALL_LOG_DETAIL_PATTERN,
        arguments = listOf(navArgument("logId") { type = NavType.LongType })
    ) { backStackEntry ->
        val logId = backStackEntry.arguments?.getLong("logId") ?: 0L
        AiLogDetailRoute(
            logId = logId,
            onBackClick = { navController.popBackStack() }
        )
    }
}
