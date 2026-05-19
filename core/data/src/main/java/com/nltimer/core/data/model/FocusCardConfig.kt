package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FocusCardConfig(
    val cardHeight: Int = 260,
    val cardPadding: Int = 16,
    val enableCardStyle: Boolean = true,
    val themeColor: Long? = null,
    val cornerStyle: FocusCardCornerStyle = FocusCardCornerStyle.LARGE,
    val customCornerSize: Int = 32,
    val shadowStyle: FocusCardShadowStyle = FocusCardShadowStyle.STANDARD,
)

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
