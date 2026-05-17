package com.nltimer.core.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.SecondsStrategy
import com.nltimer.core.designsystem.theme.AppTheme
import com.nltimer.core.designsystem.theme.AlphaPreset
import com.nltimer.core.designsystem.theme.BorderPreset
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.CornerPreset
import com.nltimer.core.designsystem.theme.Fonts
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.PaletteStyle
import com.nltimer.core.designsystem.theme.PathDrawMode
import com.nltimer.core.designsystem.theme.CardColorStrategy
import com.nltimer.core.designsystem.theme.ExpressivenessPreset
import com.nltimer.core.designsystem.theme.IconContainerSize
import com.nltimer.core.designsystem.theme.StyleConfig
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.designsystem.theme.TimeLabelFormat
import com.nltimer.core.designsystem.theme.TimeLabelStyle
import com.nltimer.core.designsystem.theme.TimerTypography
import com.nltimer.core.designsystem.theme.WavyProgressLevel
import com.nltimer.core.designsystem.theme.TopBarMode
import com.nltimer.core.designsystem.theme.BottomBarMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SettingsPrefsImpl 偏好设置实现类
 * 基于 DataStore Preferences 持久化存储主题、标签分类与弹窗配置
 *
 * @param dataStore DataStore 偏好存储实例
 */
class SettingsPrefsImpl(private val dataStore: DataStore<Preferences>) : SettingsPrefs {

    override fun getThemeFlow(): Flow<Theme> = dataStore.data.map { prefs ->
        val seed = prefs[seedColorKey] ?: DEFAULT_SEED_COLOR
        val appThemeName = prefs[appThemeKey] ?: AppTheme.SYSTEM.name
        val paletteStyleName = prefs[paletteStyleKey] ?: PaletteStyle.TONALSPOT.name
        val fontName = prefs[fontKey] ?: Fonts.FIGTREE.name
        val homeLayoutName = prefs[homeLayoutKey] ?: HomeLayout.GRID.name

        Theme(
            seedColor = Color(seed),
            appTheme = try { AppTheme.valueOf(appThemeName) } catch (_: IllegalArgumentException) { AppTheme.SYSTEM },
            isAmoled = prefs[isAmoledKey] == true,
            paletteStyle = try { PaletteStyle.valueOf(paletteStyleName) } catch (_: IllegalArgumentException) { PaletteStyle.TONALSPOT },
            isMaterialYou = prefs[isMaterialYouKey] == true,
            font = try { Fonts.valueOf(fontName) } catch (_: IllegalArgumentException) { Fonts.FIGTREE },
            showBorders = prefs[showBordersKey] != false,
            homeLayout = try { HomeLayout.valueOf(homeLayoutName) } catch (_: IllegalArgumentException) { HomeLayout.GRID },
            showTimeSideBar = prefs[showTimeSideBarKey] != false,
            topBarMode = try { TopBarMode.valueOf(prefs[topBarModeKey] ?: TopBarMode.PINNED.name) } catch (_: IllegalArgumentException) { TopBarMode.PINNED },
            bottomBarMode = try {
                val mode = BottomBarMode.valueOf(prefs[bottomBarModeKey] ?: BottomBarMode.STANDARD.name)
                if (mode == BottomBarMode.FLOATING) BottomBarMode.CENTER_FAB else mode
            } catch (_: IllegalArgumentException) { BottomBarMode.STANDARD },
            isImmersive = prefs[isImmersiveKey] == true,
            style = StyleConfig(
                cornerPreset = try { CornerPreset.valueOf(prefs[cornerPresetKey] ?: CornerPreset.STANDARD.name) } catch (_: IllegalArgumentException) { CornerPreset.STANDARD },
                borderPreset = try { BorderPreset.valueOf(prefs[borderPresetKey] ?: BorderPreset.STANDARD.name) } catch (_: IllegalArgumentException) { BorderPreset.STANDARD },
                alphaPreset = try { AlphaPreset.valueOf(prefs[alphaPresetKey] ?: AlphaPreset.STANDARD.name) } catch (_: IllegalArgumentException) { AlphaPreset.STANDARD },
                cornerScale = prefs[cornerScaleCustomKey],
                borderScale = prefs[borderScaleCustomKey],
                alphaScale = prefs[alphaScaleCustomKey],
                expressiveness = try { ExpressivenessPreset.valueOf(prefs[expressivenessKey] ?: ExpressivenessPreset.SUBDUED.name) } catch (_: IllegalArgumentException) { ExpressivenessPreset.SUBDUED },
                cardColorStrategy = try { CardColorStrategy.valueOf(prefs[cardColorStrategyKey] ?: CardColorStrategy.SURFACE.name) } catch (_: IllegalArgumentException) { CardColorStrategy.SURFACE },
                iconContainerSize = try { IconContainerSize.valueOf(prefs[iconContainerSizeKey] ?: IconContainerSize.NONE.name) } catch (_: IllegalArgumentException) { IconContainerSize.NONE },
                timerTypography = try { TimerTypography.valueOf(prefs[timerTypographyKey] ?: TimerTypography.HEADLINE.name) } catch (_: IllegalArgumentException) { TimerTypography.HEADLINE },
                wavyProgress = try { WavyProgressLevel.valueOf(prefs[wavyProgressKey] ?: WavyProgressLevel.OFF.name) } catch (_: IllegalArgumentException) { WavyProgressLevel.OFF },
            ),
        )
    }

    override suspend fun updateTheme(theme: Theme) {
        dataStore.edit { prefs ->
            prefs[seedColorKey] = theme.seedColor.toArgb()
            prefs[appThemeKey] = theme.appTheme.name
            prefs[isAmoledKey] = theme.isAmoled
            prefs[paletteStyleKey] = theme.paletteStyle.name
            prefs[isMaterialYouKey] = theme.isMaterialYou
            prefs[fontKey] = theme.font.name
            prefs[showBordersKey] = theme.showBorders
            prefs[homeLayoutKey] = theme.homeLayout.name
            prefs[showTimeSideBarKey] = theme.showTimeSideBar
            prefs[cornerPresetKey] = theme.style.cornerPreset.name
            prefs[borderPresetKey] = theme.style.borderPreset.name
            prefs[alphaPresetKey] = theme.style.alphaPreset.name
            val cornerScale = theme.style.cornerScale; if (cornerScale != null) prefs[cornerScaleCustomKey] = cornerScale else prefs.remove(cornerScaleCustomKey)
            val borderScale = theme.style.borderScale; if (borderScale != null) prefs[borderScaleCustomKey] = borderScale else prefs.remove(borderScaleCustomKey)
            val alphaScale = theme.style.alphaScale; if (alphaScale != null) prefs[alphaScaleCustomKey] = alphaScale else prefs.remove(alphaScaleCustomKey)
            prefs[expressivenessKey] = theme.style.expressiveness.name
            prefs[cardColorStrategyKey] = theme.style.cardColorStrategy.name
            prefs[iconContainerSizeKey] = theme.style.iconContainerSize.name
            prefs[timerTypographyKey] = theme.style.timerTypography.name
            prefs[wavyProgressKey] = theme.style.wavyProgress.name
            prefs[topBarModeKey] = theme.topBarMode.name
            prefs[bottomBarModeKey] = theme.bottomBarMode.name
            prefs[isImmersiveKey] = theme.isImmersive
        }
    }

    override fun getSavedTagCategories(): Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[savedTagCategoriesKey] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    override fun getSavedTagCategoriesOrder(): Flow<List<String>> = dataStore.data.map { prefs ->
        val raw = prefs[savedTagCategoriesKey] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    override suspend fun saveTagCategories(categories: Set<String>) {
        dataStore.edit { prefs ->
            prefs[savedTagCategoriesKey] = categories.joinToString(",")
        }
    }

    override suspend fun saveTagCategoriesOrder(categories: List<String>) {
        dataStore.edit { prefs ->
            prefs[savedTagCategoriesKey] = categories.distinct().joinToString(",")
        }
    }

    override fun getDialogConfigFlow(): Flow<DialogGridConfig> = dataStore.data.map { prefs ->
        DialogGridConfig(
            activityDisplayMode = try { ChipDisplayMode.valueOf(prefs[actDisplayModeKey] ?: ChipDisplayMode.Filled.name) } catch (_: IllegalArgumentException) { ChipDisplayMode.Filled },
            activityLayoutMode = try { GridLayoutMode.valueOf(prefs[actLayoutModeKey] ?: GridLayoutMode.Horizontal.name) } catch (_: IllegalArgumentException) { GridLayoutMode.Horizontal },
            activityColumnLines = prefs[actColumnLinesKey] ?: 2,
            activityHorizontalLines = prefs[actHorizontalLinesKey] ?: 2,
            activityUseColorForText = prefs[actUseColorKey] ?: true,
            tagDisplayMode = try { ChipDisplayMode.valueOf(prefs[tagDisplayModeKey] ?: ChipDisplayMode.Filled.name) } catch (_: IllegalArgumentException) { ChipDisplayMode.Filled },
            tagLayoutMode = try { GridLayoutMode.valueOf(prefs[tagLayoutModeKey] ?: GridLayoutMode.Horizontal.name) } catch (_: IllegalArgumentException) { GridLayoutMode.Horizontal },
            tagColumnLines = prefs[tagColumnLinesKey] ?: 2,
            tagHorizontalLines = prefs[tagHorizontalLinesKey] ?: 2,
            tagUseColorForText = prefs[tagUseColorKey] ?: true,
            showBehaviorNature = prefs[showNatureKey] ?: true,
            pathDrawMode = try { PathDrawMode.valueOf(prefs[pathDrawModeKey] ?: PathDrawMode.StartToEnd.name) } catch (_: IllegalArgumentException) { PathDrawMode.StartToEnd },
            secondsStrategy = try { SecondsStrategy.valueOf(prefs[secondsStrategyKey] ?: SecondsStrategy.OPEN_TIME.name) } catch (_: IllegalArgumentException) { SecondsStrategy.OPEN_TIME },
            autoMatchNote = prefs[autoMatchNoteKey] ?: false,

        )
    }

    override suspend fun updateDialogConfig(config: DialogGridConfig) {
        dataStore.edit { prefs ->
            prefs[actDisplayModeKey] = config.activityDisplayMode.name
            prefs[actLayoutModeKey] = config.activityLayoutMode.name
            prefs[actColumnLinesKey] = config.activityColumnLines
            prefs[actHorizontalLinesKey] = config.activityHorizontalLines
            prefs[actUseColorKey] = config.activityUseColorForText
            prefs[tagDisplayModeKey] = config.tagDisplayMode.name
            prefs[tagLayoutModeKey] = config.tagLayoutMode.name
            prefs[tagColumnLinesKey] = config.tagColumnLines
            prefs[tagHorizontalLinesKey] = config.tagHorizontalLines
            prefs[tagUseColorKey] = config.tagUseColorForText
            prefs[showNatureKey] = config.showBehaviorNature
            prefs[pathDrawModeKey] = config.pathDrawMode.name
            prefs[secondsStrategyKey] = config.secondsStrategy.name
            prefs[autoMatchNoteKey] = config.autoMatchNote

        }
    }

    override fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig> = dataStore.data.map { prefs ->
        val raw = prefs[timeLabelConfigKey]
        if (raw.isNullOrBlank()) {
            TimeLabelConfig()
        } else {
            parseTimeLabelConfig(raw)
        }
    }

    override suspend fun updateTimeLabelConfig(config: TimeLabelConfig) {
        dataStore.edit { prefs ->
            prefs[timeLabelConfigKey] = serializeTimeLabelConfig(config)
        }
    }

    override fun getHasSeenIntroFlow(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[hasSeenIntroKey] == true
    }

    override suspend fun setHasSeenIntro(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[hasSeenIntroKey] = seen
        }
    }

    override fun getHomeLayoutConfigFlow(): Flow<HomeLayoutConfig> = dataStore.data.map { prefs ->
        HomeLayoutConfig(
            grid = GridLayoutStyle(
                columns = prefs[gridColumnsKey] ?: 4,
                minRowHeight = prefs[gridMinRowHeightKey] ?: 100,
                maxCellHeight = prefs[gridMaxCellHeightKey] ?: 140,
                columnSpacing = prefs[gridColumnSpacingKey] ?: 5,
                cellPadding = prefs[gridCellPaddingKey] ?: 4,
                iconSize = prefs[gridIconSizeKey] ?: 14,
                tagScale = prefs[gridTagScaleKey] ?: 0.8f,
                tagSpacing = prefs[gridTagSpacingKey] ?: 2,
                activeBgAlpha = prefs[gridActiveBgAlphaKey] ?: 0.3f,
            ),
            log = LogLayoutStyle(
                cardPadding = prefs[logCardPaddingKey] ?: 12,
                iconSize = prefs[logIconSizeKey] ?: 18,
                iconSpacing = prefs[logIconSpacingKey] ?: 6,
                tagRowSpacing = prefs[logTagRowSpacingKey] ?: 6,
                statusBadgePaddingH = prefs[logBadgePaddingHKey] ?: 8,
                statusBadgePaddingV = prefs[logBadgePaddingVKey] ?: 2,
            ),
            timeline = TimelineLayoutStyle(
                itemSpacing = prefs[timelineItemSpacingKey] ?: 8,
            ),
            moment = MomentLayoutStyle(
                cardPadding = prefs[momentCardPaddingKey] ?: 16,
            ),
        )
    }

    override suspend fun updateHomeLayoutConfig(config: HomeLayoutConfig) {
        dataStore.edit { prefs ->
            prefs[gridColumnsKey] = config.grid.columns
            prefs[gridMinRowHeightKey] = config.grid.minRowHeight
            prefs[gridMaxCellHeightKey] = config.grid.maxCellHeight
            prefs[gridColumnSpacingKey] = config.grid.columnSpacing
            prefs[gridCellPaddingKey] = config.grid.cellPadding
            prefs[gridIconSizeKey] = config.grid.iconSize
            prefs[gridTagScaleKey] = config.grid.tagScale
            prefs[gridTagSpacingKey] = config.grid.tagSpacing
            prefs[gridActiveBgAlphaKey] = config.grid.activeBgAlpha
            prefs[logCardPaddingKey] = config.log.cardPadding
            prefs[logIconSizeKey] = config.log.iconSize
            prefs[logIconSpacingKey] = config.log.iconSpacing
            prefs[logTagRowSpacingKey] = config.log.tagRowSpacing
            prefs[logBadgePaddingHKey] = config.log.statusBadgePaddingH
            prefs[logBadgePaddingVKey] = config.log.statusBadgePaddingV
            prefs[timelineItemSpacingKey] = config.timeline.itemSpacing
            prefs[momentCardPaddingKey] = config.moment.cardPadding
        }
    }

    override fun getDisplayColorConfigFlow(): Flow<com.nltimer.core.data.model.DisplayColorConfig> = dataStore.data.map { prefs ->
        com.nltimer.core.data.model.DisplayColorConfig(
            activityIconColorMode = try { com.nltimer.core.designsystem.theme.DisplayColorMode.valueOf(prefs[activityIconColorModeKey] ?: com.nltimer.core.designsystem.theme.DisplayColorMode.NORMAL.name) } catch (_: IllegalArgumentException) { com.nltimer.core.designsystem.theme.DisplayColorMode.NORMAL },
            tagDisplayColorMode = try { com.nltimer.core.designsystem.theme.DisplayColorMode.valueOf(prefs[tagDisplayColorModeKey] ?: com.nltimer.core.designsystem.theme.DisplayColorMode.NORMAL.name) } catch (_: IllegalArgumentException) { com.nltimer.core.designsystem.theme.DisplayColorMode.NORMAL },
        )
    }

    override suspend fun updateDisplayColorConfig(config: com.nltimer.core.data.model.DisplayColorConfig) {
        dataStore.edit { prefs ->
            prefs[activityIconColorModeKey] = config.activityIconColorMode.name
            prefs[tagDisplayColorModeKey] = config.tagDisplayColorMode.name
        }
    }

    private fun serializeTimeLabelConfig(config: TimeLabelConfig): String {
        return "${config.visible}|${config.style.name}|${config.format.name}"
    }

    private fun parseTimeLabelConfig(raw: String): TimeLabelConfig {
        val parts = raw.split("|")
        if (parts.size != 3) return TimeLabelConfig()
        return TimeLabelConfig(
            visible = parts[0].toBooleanStrictOrNull() ?: true,
            style = try { TimeLabelStyle.valueOf(parts[1]) } catch (_: IllegalArgumentException) { TimeLabelStyle.PILL },
            format = try { TimeLabelFormat.valueOf(parts[2]) } catch (_: IllegalArgumentException) { TimeLabelFormat.HH_MM },
        )
    }

    companion object {
        private const val DEFAULT_SEED_COLOR = 0xFF1565C0.toInt()
        private val seedColorKey = intPreferencesKey("seed_color")
        private val appThemeKey = stringPreferencesKey("app_theme")
        private val isAmoledKey = booleanPreferencesKey("is_amoled")
        private val paletteStyleKey = stringPreferencesKey("palette_style")
        private val isMaterialYouKey = booleanPreferencesKey("is_material_you")
        private val fontKey = stringPreferencesKey("font")
        private val showBordersKey = booleanPreferencesKey("show_borders")
        private val homeLayoutKey = stringPreferencesKey("home_layout")
        private val showTimeSideBarKey = booleanPreferencesKey("show_time_side_bar")
        private val savedTagCategoriesKey = stringPreferencesKey("saved_tag_categories")

        private val actDisplayModeKey = stringPreferencesKey("act_display_mode")
        private val actLayoutModeKey = stringPreferencesKey("act_layout_mode")
        private val actColumnLinesKey = intPreferencesKey("act_column_lines")
        private val actHorizontalLinesKey = intPreferencesKey("act_horizontal_lines")
        private val actUseColorKey = booleanPreferencesKey("act_use_color")
        private val tagDisplayModeKey = stringPreferencesKey("tag_display_mode")
        private val tagLayoutModeKey = stringPreferencesKey("tag_layout_mode")
        private val tagColumnLinesKey = intPreferencesKey("tag_column_lines")
        private val tagHorizontalLinesKey = intPreferencesKey("tag_horizontal_lines")
        private val tagUseColorKey = booleanPreferencesKey("tag_use_color")
        private val showNatureKey = booleanPreferencesKey("show_nature_selector")
        private val pathDrawModeKey = stringPreferencesKey("path_draw_mode")
        private val secondsStrategyKey = stringPreferencesKey("seconds_strategy")
        private val autoMatchNoteKey = booleanPreferencesKey("auto_match_note")
        private val timeLabelConfigKey = stringPreferencesKey("time_label_config")

        private val cornerPresetKey = stringPreferencesKey("corner_preset")
        private val borderPresetKey = stringPreferencesKey("border_preset")
        private val alphaPresetKey = stringPreferencesKey("alpha_preset")
        private val cornerScaleCustomKey = floatPreferencesKey("corner_scale_custom")
        private val borderScaleCustomKey = floatPreferencesKey("border_scale_custom")
        private val alphaScaleCustomKey = floatPreferencesKey("alpha_scale_custom")
        private val expressivenessKey = stringPreferencesKey("expressiveness_key")
        private val cardColorStrategyKey = stringPreferencesKey("card_color_strategy_key")
        private val iconContainerSizeKey = stringPreferencesKey("icon_container_size_key")
        private val timerTypographyKey = stringPreferencesKey("timer_typography_key")
        private val wavyProgressKey = stringPreferencesKey("wavy_progress_key")
        private val hasSeenIntroKey = booleanPreferencesKey("has_seen_intro")
        private val topBarModeKey = stringPreferencesKey("top_bar_mode")
        private val bottomBarModeKey = stringPreferencesKey("bottom_bar_mode")
        private val isImmersiveKey = booleanPreferencesKey("is_immersive")

        private val gridColumnsKey = intPreferencesKey("home_grid_columns")
        private val gridMinRowHeightKey = intPreferencesKey("home_grid_min_row_height")
        private val gridMaxCellHeightKey = intPreferencesKey("home_grid_max_cell_height")
        private val gridColumnSpacingKey = intPreferencesKey("home_grid_column_spacing")
        private val gridCellPaddingKey = intPreferencesKey("home_grid_cell_padding")
        private val gridIconSizeKey = intPreferencesKey("home_grid_icon_size")
        private val gridTagScaleKey = floatPreferencesKey("home_grid_tag_scale")
        private val gridTagSpacingKey = intPreferencesKey("home_grid_tag_spacing")
        private val gridActiveBgAlphaKey = floatPreferencesKey("home_grid_active_bg_alpha")
        private val logCardPaddingKey = intPreferencesKey("home_log_card_padding")
        private val logIconSizeKey = intPreferencesKey("home_log_icon_size")
        private val logIconSpacingKey = intPreferencesKey("home_log_icon_spacing")
        private val logTagRowSpacingKey = intPreferencesKey("home_log_tag_row_spacing")
        private val logBadgePaddingHKey = intPreferencesKey("home_log_badge_padding_h")
        private val logBadgePaddingVKey = intPreferencesKey("home_log_badge_padding_v")
        private val timelineItemSpacingKey = intPreferencesKey("home_timeline_item_spacing")
        private val momentCardPaddingKey = intPreferencesKey("home_moment_card_padding")
        private val activityIconColorModeKey = stringPreferencesKey("activity_icon_color_mode")
        private val tagDisplayColorModeKey = stringPreferencesKey("tag_display_color_mode")
    }
}
