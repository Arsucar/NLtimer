package com.nltimer.core.designsystem.theme

import com.materialkolor.PaletteStyle as MPaletteStyle
import com.nltimer.core.designsystem.R

/**
 * 将本地字体枚举映射为资源 ID，系统默认字体返回 null
 */
fun Fonts.toFontRes(): Int? = when (this) {
    Fonts.FIGTREE -> R.font.figtree
    Fonts.SYSTEM_DEFAULT -> null
    Fonts.DM_SERIF_TEXT -> R.font.dm_serif_text
    Fonts.GOOGLE_SANS_FLEX -> R.font.google_sans_flex
}

/**
 * 返回字体的可读显示名称
 */
fun Fonts.toDisplayString(): String = when (this) {
    Fonts.FIGTREE -> "Figtree"
    Fonts.SYSTEM_DEFAULT -> "System Default"
    Fonts.DM_SERIF_TEXT -> "DM Serif Text"
    Fonts.GOOGLE_SANS_FLEX -> "Google Sans Flex"
}

/**
 * 将本地 PaletteStyle 枚举映射为 materialkolor 库的 PaletteStyle
 */
fun PaletteStyle.toMPaletteStyle(): MPaletteStyle = when (this) {
    PaletteStyle.TONALSPOT -> MPaletteStyle.TonalSpot
    PaletteStyle.NEUTRAL -> MPaletteStyle.Neutral
    PaletteStyle.VIBRANT -> MPaletteStyle.Vibrant
    PaletteStyle.EXPRESSIVE -> MPaletteStyle.Expressive
    PaletteStyle.RAINBOW -> MPaletteStyle.Rainbow
    PaletteStyle.FRUITSALAD -> MPaletteStyle.FruitSalad
    PaletteStyle.MONOCHROME -> MPaletteStyle.Monochrome
    PaletteStyle.FIDELITY -> MPaletteStyle.Fidelity
    PaletteStyle.CONTENT -> MPaletteStyle.Content
}

/**
 * 返回主题模式的可读显示文本
 */
fun AppTheme.toDisplayString(): String = when (this) {
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
    AppTheme.SYSTEM -> "Follow System"
}

/**
 * 返回首页布局的可读显示文本
 */
fun HomeLayout.toDisplayString(): String = when (this) {
    HomeLayout.GRID -> "网格时间"
    HomeLayout.TIMELINE_REVERSE -> "时间轴(反)"
    HomeLayout.LOG -> "行为日志"
    HomeLayout.MOMENT -> "当前时刻"
    HomeLayout.TEXT_LIST -> "纯文字列表"
}

fun CornerPreset.toDisplayString(): String = when (this) {
    CornerPreset.COMPACT -> "紧凑"
    CornerPreset.STANDARD -> "标准"
    CornerPreset.ROUNDED -> "圆润"
    CornerPreset.SOFT -> "超圆"
}

fun BorderPreset.toDisplayString(): String = when (this) {
    BorderPreset.NONE -> "无边"
    BorderPreset.THIN -> "纤细"
    BorderPreset.STANDARD -> "标准"
    BorderPreset.THICK -> "粗厚"
}

fun AlphaPreset.toDisplayString(): String = when (this) {
    AlphaPreset.SUBTLE -> "淡雅"
    AlphaPreset.STANDARD -> "标准"
    AlphaPreset.VIVID -> "鲜明"
    AlphaPreset.SOLID -> "实心"
}

fun ExpressivenessPreset.toDisplayString(): String = when (this) {
    ExpressivenessPreset.SUBDUED -> "克制"
    ExpressivenessPreset.STANDARD -> "标准"
    ExpressivenessPreset.EXPRESSIVE -> "表达🔥"
}

fun CardColorStrategy.toDisplayString(): String = when (this) {
    CardColorStrategy.SURFACE -> "素色"
    CardColorStrategy.TINTED_PRIMARY -> "主色浸染"
    CardColorStrategy.MULTI_CONTAINER -> "多彩容器"
}

fun IconContainerSize.toDisplayString(): String = when (this) {
    IconContainerSize.NONE -> "无"
    IconContainerSize.CIRCLE_SMALL -> "小圆"
    IconContainerSize.CIRCLE_LARGE -> "大圆"
}

fun TimerTypography.toDisplayString(): String = when (this) {
    TimerTypography.HEADLINE -> "标题"
    TimerTypography.DISPLAY_SMALL -> "展示 S"
    TimerTypography.DISPLAY_LARGE_SERIF -> "衬线展示"
}

fun WavyProgressLevel.toDisplayString(): String = when (this) {
    WavyProgressLevel.OFF -> "关闭"
    WavyProgressLevel.ON -> "波浪"
    WavyProgressLevel.PROMINENT -> "醒目波浪"
}

fun TopBarMode.toDisplayString(): String = when (this) {
    TopBarMode.PINNED -> "固定"
    TopBarMode.COLLAPSED -> "折叠"
}

fun BottomBarMode.toDisplayString(): String = when (this) {
    BottomBarMode.STANDARD -> "标准"
    BottomBarMode.FLOATING -> "悬浮"
    BottomBarMode.CENTER_FAB -> "悬浮居中"
}
