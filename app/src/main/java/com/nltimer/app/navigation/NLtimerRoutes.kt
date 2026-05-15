package com.nltimer.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector

object NLtimerRoutes {
    const val HOME = "home"
    const val STATS = "stats"
    const val CATEGORIES = "categories"
    const val MANAGEMENT_ACTIVITIES = "management_activities"
    const val TAG_MANAGEMENT = "tag_management"
    const val SETTINGS = "settings"
    const val THEME_SETTINGS = "theme_settings"
    const val DIALOG_CONFIG = "dialog_config"
    const val BEHAVIOR_MANAGEMENT = "behavior_management"
    const val DATA_MANAGEMENT = "data_management"
    const val HOME_LAYOUT_CONFIG = "home_layout_config"
    const val COLOR_PALETTE = "color_palette"
    const val AI_INTER = "ai_inter"
    const val AI_PROVIDER_CONFIG = "ai_provider_config"
    const val AI_TOOLS_LIST = "ai_tools_list"
    const val AI_CALL_LOGS = "ai_call_logs"
    const val AI_PROMPT_CONFIG = "ai_prompt_config"
    const val AI_TEST_CHAT = "ai_test_chat"

    val PRIMARY_ROUTES = setOf(HOME, STATS, CATEGORIES, MANAGEMENT_ACTIVITIES, SETTINGS)
    val SETTINGS_FULLSCREEN_ROUTES = setOf(
        THEME_SETTINGS, DIALOG_CONFIG, BEHAVIOR_MANAGEMENT, DATA_MANAGEMENT, 
        HOME_LAYOUT_CONFIG, COLOR_PALETTE, CATEGORIES, AI_INTER,
        AI_PROVIDER_CONFIG, AI_TOOLS_LIST, AI_CALL_LOGS, AI_PROMPT_CONFIG,
        AI_TEST_CHAT
    )
}

data class RouteConfig(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val showBottomNav: Boolean = true,
    val isFullscreen: Boolean = false,
    val topBarTitle: String? = null,
)
