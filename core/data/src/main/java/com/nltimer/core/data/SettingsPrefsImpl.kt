package com.nltimer.core.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.FocusCardConfig
import com.nltimer.core.data.model.FocusCardCornerStyle
import com.nltimer.core.data.model.FocusCardShadowStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.StatsDashboardConfig
import com.nltimer.core.data.model.TextListFieldConfig
import com.nltimer.core.data.model.TextListFieldType
import com.nltimer.core.data.model.TextListFieldColorMode
import com.nltimer.core.data.model.TextListColumnMode
import com.nltimer.core.data.model.TextListLayoutStyle
import com.nltimer.core.data.model.defaultTextListFieldConfigs
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.SecondsStrategy
import com.nltimer.core.data.model.DisplayColorConfig
import com.nltimer.core.data.model.defaultStatsDashboardConfig
import com.nltimer.core.data.util.safeValueOf
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
import com.nltimer.core.designsystem.theme.DisplayColorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

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
        val paletteStyleName = prefs[paletteStyleKey] ?: PaletteStyle.CONTENT.name
        val fontName = prefs[fontKey] ?: Fonts.SYSTEM_DEFAULT.name
        val homeLayoutName = prefs[homeLayoutKey] ?: HomeLayout.GRID.name

        Theme(
            seedColor = Color(seed),
            appTheme = safeValueOf(appThemeName, AppTheme.SYSTEM),
            isAmoled = prefs[isAmoledKey] == true,
            paletteStyle = safeValueOf(paletteStyleName, PaletteStyle.CONTENT),
            isMaterialYou = prefs[isMaterialYouKey] == true,
            font = safeValueOf(fontName, Fonts.SYSTEM_DEFAULT),
            showBorders = prefs[showBordersKey] != false,
            homeLayout = safeValueOf(homeLayoutName, HomeLayout.GRID),
            showTimeSideBar = prefs[showTimeSideBarKey] != false,
            topBarMode = safeValueOf(prefs[topBarModeKey] ?: TopBarMode.COLLAPSED.name, TopBarMode.COLLAPSED),
            bottomBarMode = run {
                val mode = safeValueOf(prefs[bottomBarModeKey] ?: BottomBarMode.CENTER_FAB.name, BottomBarMode.CENTER_FAB)
                if (mode == BottomBarMode.FLOATING) BottomBarMode.CENTER_FAB else mode
            },
            isImmersive = prefs[isImmersiveKey] != false,
            topBarHaze = prefs[topBarHazeKey] != false,
            style = StyleConfig(
                cornerPreset = safeValueOf(prefs[cornerPresetKey] ?: CornerPreset.STANDARD.name, CornerPreset.STANDARD),
                borderPreset = safeValueOf(prefs[borderPresetKey] ?: BorderPreset.STANDARD.name, BorderPreset.STANDARD),
                alphaPreset = safeValueOf(prefs[alphaPresetKey] ?: AlphaPreset.STANDARD.name, AlphaPreset.STANDARD),
                cornerScale = prefs[cornerScaleCustomKey],
                borderScale = prefs[borderScaleCustomKey],
                alphaScale = prefs[alphaScaleCustomKey],
                expressiveness = safeValueOf(prefs[expressivenessKey] ?: ExpressivenessPreset.STANDARD.name, ExpressivenessPreset.STANDARD),
                cardColorStrategy = safeValueOf(prefs[cardColorStrategyKey] ?: CardColorStrategy.SURFACE.name, CardColorStrategy.SURFACE),
                iconContainerSize = safeValueOf(prefs[iconContainerSizeKey] ?: IconContainerSize.NONE.name, IconContainerSize.NONE),
                timerTypography = safeValueOf(prefs[timerTypographyKey] ?: TimerTypography.HEADLINE.name, TimerTypography.HEADLINE),
                wavyProgress = safeValueOf(prefs[wavyProgressKey] ?: WavyProgressLevel.OFF.name, WavyProgressLevel.OFF),
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
            prefs[topBarHazeKey] = theme.topBarHaze
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
        val useGlobalTagConfig = prefs[useGlobalTagConfigKey] ?: false
        val tagDisplayConfig = if (useGlobalTagConfig) {
            // 从全局配置读取
            com.nltimer.core.data.model.TagDisplayConfig(
                displayMode = safeValueOf(prefs[globalTagDisplayModeKey] ?: ChipDisplayMode.Filled.name, ChipDisplayMode.Filled),
                layoutMode = safeValueOf(prefs[globalTagLayoutModeKey] ?: GridLayoutMode.Horizontal.name, GridLayoutMode.Horizontal),
                columnLines = prefs[globalTagColumnLinesKey] ?: 2,
                horizontalLines = prefs[globalTagHorizontalLinesKey] ?: 2,
                useColorForText = prefs[globalTagUseColorForTextKey] ?: true,
            )
        } else {
            // 从弹窗配置读取
            com.nltimer.core.data.model.TagDisplayConfig(
                displayMode = safeValueOf(prefs[tagDisplayModeKey] ?: ChipDisplayMode.Filled.name, ChipDisplayMode.Filled),
                layoutMode = safeValueOf(prefs[tagLayoutModeKey] ?: GridLayoutMode.Horizontal.name, GridLayoutMode.Horizontal),
                columnLines = prefs[tagColumnLinesKey] ?: 2,
                horizontalLines = prefs[tagHorizontalLinesKey] ?: 2,
                useColorForText = prefs[tagUseColorKey] ?: true,
            )
        }

        DialogGridConfig(
            activityDisplayMode = safeValueOf(prefs[actDisplayModeKey] ?: ChipDisplayMode.Filled.name, ChipDisplayMode.Filled),
            activityLayoutMode = safeValueOf(prefs[actLayoutModeKey] ?: GridLayoutMode.Horizontal.name, GridLayoutMode.Horizontal),
            activityColumnLines = prefs[actColumnLinesKey] ?: 2,
            activityHorizontalLines = prefs[actHorizontalLinesKey] ?: 2,
            activityUseColorForText = prefs[actUseColorKey] ?: true,
            tagDisplayMode = safeValueOf(prefs[tagDisplayModeKey] ?: ChipDisplayMode.Filled.name, ChipDisplayMode.Filled),
            tagLayoutMode = safeValueOf(prefs[tagLayoutModeKey] ?: GridLayoutMode.Horizontal.name, GridLayoutMode.Horizontal),
            tagColumnLines = prefs[tagColumnLinesKey] ?: 2,
            tagHorizontalLines = prefs[tagHorizontalLinesKey] ?: 2,
            tagUseColorForText = prefs[tagUseColorKey] ?: true,
            showBehaviorNature = prefs[showNatureKey] ?: true,
            pathDrawMode = safeValueOf(prefs[pathDrawModeKey] ?: PathDrawMode.StartToEnd.name, PathDrawMode.StartToEnd),
            secondsStrategy = safeValueOf(prefs[secondsStrategyKey] ?: SecondsStrategy.OPEN_TIME.name, SecondsStrategy.OPEN_TIME),
            autoMatchNote = prefs[autoMatchNoteKey] ?: false,
            useGlobalTagConfig = useGlobalTagConfig,
            tagDisplayConfig = tagDisplayConfig,
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
            prefs[useGlobalTagConfigKey] = config.useGlobalTagConfig

            // 如果使用全局配置，更新全局配置
            if (config.useGlobalTagConfig) {
                prefs[globalTagDisplayModeKey] = config.tagDisplayConfig.displayMode.name
                prefs[globalTagLayoutModeKey] = config.tagDisplayConfig.layoutMode.name
                prefs[globalTagColumnLinesKey] = config.tagDisplayConfig.columnLines
                prefs[globalTagHorizontalLinesKey] = config.tagDisplayConfig.horizontalLines
                prefs[globalTagUseColorForTextKey] = config.tagDisplayConfig.useColorForText
            }
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
            textList = TextListLayoutStyle(
                rowSpacing = prefs[textListRowSpacingKey] ?: 4,
                fieldSpacing = prefs[textListFieldSpacingKey] ?: 8,
                paddingH = prefs[textListPaddingHKey] ?: 12,
                paddingV = prefs[textListPaddingVKey] ?: 2,
                globalFontScale = prefs[textListGlobalFontScaleKey] ?: 1f,
                fieldConfigs = prefs[textListFieldConfigsKey]?.let { parseTextListFieldConfigs(it) }
                    ?: defaultTextListFieldConfigs(),
                separator = prefs[textListSeparatorKey] ?: " · ",
                columnMode = safeValueOf(prefs[textListColumnModeKey] ?: TextListColumnMode.FLOW.name, TextListColumnMode.FLOW),
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
            prefs[textListRowSpacingKey] = config.textList.rowSpacing
            prefs[textListFieldSpacingKey] = config.textList.fieldSpacing
            prefs[textListPaddingHKey] = config.textList.paddingH
            prefs[textListPaddingVKey] = config.textList.paddingV
            prefs[textListGlobalFontScaleKey] = config.textList.globalFontScale
            prefs[textListFieldConfigsKey] = serializeTextListFieldConfigs(config.textList.fieldConfigs)
            prefs[textListSeparatorKey] = config.textList.separator
            prefs[textListColumnModeKey] = config.textList.columnMode.name
        }
    }

    override fun getDisplayColorConfigFlow(): Flow<DisplayColorConfig> = dataStore.data.map { prefs ->
        DisplayColorConfig(
            activityIconColorMode = safeValueOf(prefs[activityIconColorModeKey] ?: DisplayColorMode.NORMAL.name, DisplayColorMode.NORMAL),
            tagDisplayColorMode = safeValueOf(prefs[tagDisplayColorModeKey] ?: DisplayColorMode.NORMAL.name, DisplayColorMode.NORMAL),
            showTagIcon = prefs[showTagIconKey] != false,
        )
    }

    override suspend fun updateDisplayColorConfig(config: DisplayColorConfig) {
        dataStore.edit { prefs ->
            prefs[activityIconColorModeKey] = config.activityIconColorMode.name
            prefs[tagDisplayColorModeKey] = config.tagDisplayColorMode.name
            prefs[showTagIconKey] = config.showTagIcon
        }
    }

    override fun getTagDisplayConfigFlow(): Flow<com.nltimer.core.data.model.TagDisplayConfig> = dataStore.data.map { prefs ->
        com.nltimer.core.data.model.TagDisplayConfig(
            displayMode = safeValueOf(prefs[globalTagDisplayModeKey] ?: ChipDisplayMode.Filled.name, ChipDisplayMode.Filled),
            layoutMode = safeValueOf(prefs[globalTagLayoutModeKey] ?: GridLayoutMode.Horizontal.name, GridLayoutMode.Horizontal),
            columnLines = prefs[globalTagColumnLinesKey] ?: 2,
            horizontalLines = prefs[globalTagHorizontalLinesKey] ?: 2,
            useColorForText = prefs[globalTagUseColorForTextKey] ?: true,
        )
    }

    override suspend fun updateTagDisplayConfig(config: com.nltimer.core.data.model.TagDisplayConfig) {
        dataStore.edit { prefs ->
            prefs[globalTagDisplayModeKey] = config.displayMode.name
            prefs[globalTagLayoutModeKey] = config.layoutMode.name
            prefs[globalTagColumnLinesKey] = config.columnLines
            prefs[globalTagHorizontalLinesKey] = config.horizontalLines
            prefs[globalTagUseColorForTextKey] = config.useColorForText
        }
    }

    override fun getFocusCardConfigFlow(): Flow<FocusCardConfig> = dataStore.data.map { prefs ->
        val themeColor = prefs[focusCardThemeColorKey]
        FocusCardConfig(
            cardHeight = prefs[focusCardHeightKey] ?: 260,
            cardPadding = prefs[focusCardPaddingKey] ?: 16,
            enableCardStyle = prefs[focusCardEnableCardStyleKey] != false,
            themeColor = themeColor,
            cornerStyle = safeValueOf(prefs[focusCardCornerStyleKey] ?: FocusCardCornerStyle.LARGE.name, FocusCardCornerStyle.LARGE),
            customCornerSize = prefs[focusCardCustomCornerKey] ?: 32,
            shadowStyle = safeValueOf(prefs[focusCardShadowStyleKey] ?: FocusCardShadowStyle.STANDARD.name, FocusCardShadowStyle.STANDARD),
        )
    }

    override suspend fun updateFocusCardConfig(config: FocusCardConfig) {
        dataStore.edit { prefs ->
            prefs[focusCardHeightKey] = config.cardHeight
            prefs[focusCardPaddingKey] = config.cardPadding
            prefs[focusCardEnableCardStyleKey] = config.enableCardStyle
            if (config.themeColor != null) prefs[focusCardThemeColorKey] = config.themeColor else prefs.remove(focusCardThemeColorKey)
            prefs[focusCardCornerStyleKey] = config.cornerStyle.name
            prefs[focusCardCustomCornerKey] = config.customCornerSize
            prefs[focusCardShadowStyleKey] = config.shadowStyle.name
        }
    }

    override fun getStatsDashboardConfigFlow(): Flow<StatsDashboardConfig> = dataStore.data.map { prefs ->
        val raw = prefs[statsDashboardConfigKey]
        if (raw.isNullOrBlank()) {
            defaultStatsDashboardConfig()
        } else {
            try {
                json.decodeFromString<StatsDashboardConfig>(raw)
            } catch (_: Exception) {
                defaultStatsDashboardConfig()
            }
        }
    }

    override suspend fun updateStatsDashboardConfig(config: StatsDashboardConfig) {
        dataStore.edit { prefs ->
            prefs[statsDashboardConfigKey] = json.encodeToString(StatsDashboardConfig.serializer(), config)
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
            style = safeValueOf(parts[1], TimeLabelStyle.PILL),
            format = safeValueOf(parts[2], TimeLabelFormat.HH_MM),
        )
    }

    private fun serializeTextListFieldConfigs(configs: List<TextListFieldConfig>): String {
        return configs.joinToString(",") { cfg ->
            "${cfg.field.name}:${cfg.visible}:${cfg.bold}:${cfg.italic}:${cfg.fontScale}:${cfg.colorMode.name}"
        }
    }

    private fun parseTextListFieldConfigs(raw: String): List<TextListFieldConfig> {
        if (raw.isBlank()) return defaultTextListFieldConfigs()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size < 5) return@mapNotNull null
            val field = try {
                enumValueOf<TextListFieldType>(parts[0])
            } catch (_: IllegalArgumentException) {
                return@mapNotNull null
            }
            val colorMode = if (parts.size >= 6) {
                try { enumValueOf<TextListFieldColorMode>(parts[5]) } catch (_: IllegalArgumentException) { TextListFieldColorMode.DEFAULT }
            } else TextListFieldColorMode.DEFAULT
            TextListFieldConfig(
                field = field,
                visible = parts[1].toBooleanStrictOrNull() ?: true,
                bold = parts[2].toBooleanStrictOrNull() ?: false,
                italic = parts[3].toBooleanStrictOrNull() ?: false,
                fontScale = parts[4].toFloatOrNull() ?: 1f,
                colorMode = colorMode,
            )
        }.ifEmpty { defaultTextListFieldConfigs() }
    }

    companion object {
        private const val DEFAULT_SEED_COLOR = 0xFF539E44.toInt()
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
        private val topBarHazeKey = booleanPreferencesKey("top_bar_haze")

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
        private val textListRowSpacingKey = intPreferencesKey("home_text_row_spacing")
        private val textListFieldSpacingKey = intPreferencesKey("home_text_field_spacing")
        private val textListPaddingHKey = intPreferencesKey("home_text_padding_h")
        private val textListPaddingVKey = intPreferencesKey("home_text_padding_v")
        private val textListGlobalFontScaleKey = floatPreferencesKey("home_text_global_font_scale")
        private val textListFieldConfigsKey = stringPreferencesKey("home_text_field_configs")
        private val textListSeparatorKey = stringPreferencesKey("home_text_separator")
        private val textListColumnModeKey = stringPreferencesKey("home_text_column_mode")
        private val activityIconColorModeKey = stringPreferencesKey("activity_icon_color_mode")
        private val tagDisplayColorModeKey = stringPreferencesKey("tag_display_color_mode")
        private val showTagIconKey = booleanPreferencesKey("show_tag_icon")
        private val globalTagDisplayModeKey = stringPreferencesKey("global_tag_display_mode")
        private val globalTagLayoutModeKey = stringPreferencesKey("global_tag_layout_mode")
        private val globalTagColumnLinesKey = intPreferencesKey("global_tag_column_lines")
        private val globalTagHorizontalLinesKey = intPreferencesKey("global_tag_horizontal_lines")
        private val globalTagUseColorForTextKey = booleanPreferencesKey("global_tag_use_color_for_text")
        private val useGlobalTagConfigKey = booleanPreferencesKey("use_global_tag_config")

        private val focusCardHeightKey = intPreferencesKey("focus_card_height")
        private val focusCardPaddingKey = intPreferencesKey("focus_card_padding")
        private val focusCardEnableCardStyleKey = booleanPreferencesKey("focus_card_enable_card_style")
        private val focusCardThemeColorKey = longPreferencesKey("focus_card_theme_color")
        private val focusCardCornerStyleKey = stringPreferencesKey("focus_card_corner_style")
        private val focusCardCustomCornerKey = intPreferencesKey("focus_card_custom_corner")
        private val focusCardShadowStyleKey = stringPreferencesKey("focus_card_shadow_style")

        private val statsDashboardConfigKey = stringPreferencesKey("stats_dashboard_config")

        private val json = Json { ignoreUnknownKeys = true }
    }
}
