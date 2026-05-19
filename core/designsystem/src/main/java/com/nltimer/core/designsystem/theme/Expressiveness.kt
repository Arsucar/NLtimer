package com.nltimer.core.designsystem.theme

enum class ExpressivenessPreset {
    SUBDUED,
    STANDARD,
    EXPRESSIVE;
    companion object { val DEFAULT = STANDARD }
}

enum class CardColorStrategy {
    SURFACE,
    TINTED_PRIMARY,
    MULTI_CONTAINER;
    companion object { val DEFAULT = SURFACE }
}

enum class IconContainerSize {
    NONE,
    CIRCLE_SMALL,
    CIRCLE_LARGE;
    companion object { val DEFAULT = NONE }
}

enum class TimerTypography {
    HEADLINE,
    DISPLAY_SMALL,
    DISPLAY_LARGE_SERIF;
    companion object { val DEFAULT = HEADLINE }
}

enum class WavyProgressLevel {
    OFF,
    ON,
    PROMINENT;
    companion object { val DEFAULT = OFF }
}

fun ExpressivenessPreset.toStyleConfig(): StyleConfig = when (this) {
    ExpressivenessPreset.SUBDUED -> StyleConfig(
        cornerPreset = CornerPreset.COMPACT,
        borderPreset = BorderPreset.STANDARD,
        alphaPreset = AlphaPreset.SUBTLE,
        expressiveness = ExpressivenessPreset.SUBDUED,
        cardColorStrategy = CardColorStrategy.SURFACE,
        iconContainerSize = IconContainerSize.NONE,
        timerTypography = TimerTypography.HEADLINE,
    )
    ExpressivenessPreset.STANDARD -> StyleConfig(
        cornerPreset = CornerPreset.STANDARD,
        borderPreset = BorderPreset.THIN,
        alphaPreset = AlphaPreset.STANDARD,
        expressiveness = ExpressivenessPreset.STANDARD,
        cardColorStrategy = CardColorStrategy.TINTED_PRIMARY,
        iconContainerSize = IconContainerSize.CIRCLE_SMALL,
        timerTypography = TimerTypography.DISPLAY_SMALL,
        wavyProgress = WavyProgressLevel.ON,
    )
    ExpressivenessPreset.EXPRESSIVE -> StyleConfig(
        cornerPreset = CornerPreset.SOFT,
        borderPreset = BorderPreset.NONE,
        alphaPreset = AlphaPreset.VIVID,
        expressiveness = ExpressivenessPreset.EXPRESSIVE,
        cardColorStrategy = CardColorStrategy.MULTI_CONTAINER,
        iconContainerSize = IconContainerSize.CIRCLE_LARGE,
        timerTypography = TimerTypography.DISPLAY_LARGE_SERIF,
        wavyProgress = WavyProgressLevel.PROMINENT,
    )
}
