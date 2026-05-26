package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeLayoutConfig(
    val grid: GridLayoutStyle = GridLayoutStyle(),
    val log: LogLayoutStyle = LogLayoutStyle(),
    val timeline: TimelineLayoutStyle = TimelineLayoutStyle(),
    val moment: MomentLayoutStyle = MomentLayoutStyle(),
    val textList: TextListLayoutStyle = TextListLayoutStyle(),
)

@Immutable
data class GridLayoutStyle(
    val columns: Int = 3,
    val minRowHeight: Int = 120,
    val maxCellHeight: Int = 160,
    val columnSpacing: Int = 5,
    val cellPadding: Int = 6,
    val iconSize: Int = 14,
    val tagScale: Float = 0.8f,
    val tagSpacing: Int = 2,
    val activeBgAlpha: Float = 0.3f,
)

@Immutable
data class LogLayoutStyle(
    val cardPadding: Int = 12,
    val iconSize: Int = 18,
    val iconSpacing: Int = 6,
    val tagRowSpacing: Int = 6,
    val statusBadgePaddingH: Int = 8,
    val statusBadgePaddingV: Int = 2,
)

@Immutable
data class TimelineLayoutStyle(
    val itemSpacing: Int = 8,
)

@Immutable
data class MomentLayoutStyle(
    val cardPadding: Int = 16,
)

enum class TextListFieldType {
    NAME,
    TIME_RANGE,
    DURATION,
    TAGS,
    STATUS,
    NOTE,
    POMODORO,
    ESTIMATED,
    ACHIEVEMENT,
    PLANNED,
    ICON,
}

enum class TextListFieldColorMode {
    DEFAULT,
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
}

enum class TextListColumnMode {
    FLOW,
    TABLE,
}

@Immutable
data class TextListFieldConfig(
    val field: TextListFieldType,
    val visible: Boolean = true,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontScale: Float = 1f,
    val colorMode: TextListFieldColorMode = TextListFieldColorMode.DEFAULT,
)

@Immutable
data class TextListLayoutStyle(
    val rowSpacing: Int = 4,
    val fieldSpacing: Int = 8,
    val paddingH: Int = 12,
    val paddingV: Int = 2,
    val globalFontScale: Float = 1f,
    val fieldConfigs: List<TextListFieldConfig> = defaultTextListFieldConfigs(),
    val separator: String = ",",
    val columnMode: TextListColumnMode = TextListColumnMode.FLOW,
)

fun defaultTextListFieldConfigs(): List<TextListFieldConfig> = listOf(
    TextListFieldConfig(TextListFieldType.ICON, visible = true),
    TextListFieldConfig(TextListFieldType.NAME, visible = true, bold = true, fontScale = 1.1f),
    TextListFieldConfig(TextListFieldType.TIME_RANGE, visible = true),
    TextListFieldConfig(TextListFieldType.DURATION, visible = true),
    TextListFieldConfig(TextListFieldType.TAGS, visible = true),
    TextListFieldConfig(TextListFieldType.STATUS, visible = false),
    TextListFieldConfig(TextListFieldType.NOTE, visible = true, italic = true, fontScale = 0.9f),
    TextListFieldConfig(TextListFieldType.POMODORO, visible = false),
    TextListFieldConfig(TextListFieldType.ESTIMATED, visible = false),
    TextListFieldConfig(TextListFieldType.ACHIEVEMENT, visible = false),
    TextListFieldConfig(TextListFieldType.PLANNED, visible = false),
)
