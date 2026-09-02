package com.nltimer.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.nltimer.feature.ai.navigation.AiRoutes

object NLtimerRoutes {
    const val HOME = "home"
    const val STATS = "stats"
    const val CATEGORIES = "categories"
    const val MANAGEMENT_ACTIVITIES = "management_activities"
    const val TAG_MANAGEMENT = "tag_management"
    const val ACTIVITY_ARCHIVE = "activity_archive"
    const val TAG_ARCHIVE = "tag_archive"
    const val SETTINGS = "settings"
    const val THEME_SETTINGS = "theme_settings"
    const val DIALOG_CONFIG = "dialog_config"
    const val BEHAVIOR_MANAGEMENT = "behavior_management"
    const val DATA_MANAGEMENT = "data_management"
    const val HOME_LAYOUT_CONFIG = "home_layout_config"
    const val COLOR_PALETTE = "color_palette"
    const val ICON_MISS_LOG = "icon_miss_log"
    const val ADVANCED_SETTINGS = "advanced_settings"
    const val LOG_LIST = "log_list"

    val PRIMARY_ROUTES = setOf(HOME, STATS, CATEGORIES, MANAGEMENT_ACTIVITIES, SETTINGS, AiRoutes.AI_ASSISTANT_CHAT)
    val SETTINGS_FULLSCREEN_ROUTES = setOf(
        THEME_SETTINGS, DIALOG_CONFIG, BEHAVIOR_MANAGEMENT, DATA_MANAGEMENT,
        HOME_LAYOUT_CONFIG, COLOR_PALETTE, ICON_MISS_LOG, ADVANCED_SETTINGS,
        LOG_LIST, CATEGORIES, ACTIVITY_ARCHIVE, TAG_ARCHIVE, AiRoutes.AI_INTER,
        AiRoutes.AI_PROVIDER_CONFIG, AiRoutes.AI_TOOLS_LIST, AiRoutes.AI_CALL_LOGS, AiRoutes.AI_PROMPT_CONFIG,
        AiRoutes.AI_TEST_CHAT, AiRoutes.AI_CALL_LOG_DETAIL_PATTERN
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
