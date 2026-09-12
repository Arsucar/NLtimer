package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FocusCardConfig(
    val cardHeight: Int = DEFAULT_CARD_HEIGHT,
    val cardPadding: Int = 16,
    val enableCardStyle: Boolean = true,
    val themeColor: Long? = null,
    val cornerStyle: FocusCardCornerStyle = FocusCardCornerStyle.LARGE,
    val customCornerSize: Int = 32,
    val shadowStyle: FocusCardShadowStyle = FocusCardShadowStyle.STANDARD,
) {
    companion object {
        const val DEFAULT_CARD_HEIGHT = 280
        const val LEGACY_DEFAULT_CARD_HEIGHT = 260

        fun resolveCardHeight(stored: Int?): Int =
            if (stored == null || stored == LEGACY_DEFAULT_CARD_HEIGHT) DEFAULT_CARD_HEIGHT else stored
    }
}

enum class FocusCardCornerStyle {
    NONE,
    SMALL,
    LARGE,
    CUSTOM,
}

enum class FocusCardShadowStyle {
    NONE,
    LIGHT,
    STANDARD,
    HEAVY,
}
