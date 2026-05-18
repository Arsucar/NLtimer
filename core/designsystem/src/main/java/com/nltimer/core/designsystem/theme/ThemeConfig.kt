package com.nltimer.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * NLtimer 全局主题配置
 * 控制亮暗模式、AMOLED 优化、调色板风格、种子颜色、字体、边框显示和主页布局
 */
@Immutable
data class Theme(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val isAmoled: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TONALSPOT,
    val isMaterialYou: Boolean = false,
    val seedColor: Color = Color(0xFF1565C0),
    val font: Fonts = Fonts.FIGTREE,
    val showBorders: Boolean = true,
    val homeLayout: HomeLayout = HomeLayout.GRID,
    val showTimeSideBar: Boolean = true,
    val topBarMode: TopBarMode = TopBarMode.PINNED,
    val bottomBarMode: BottomBarMode = BottomBarMode.STANDARD,
    val isImmersive: Boolean = false,
    val topBarHaze: Boolean = true,
    val style: StyleConfig = StyleConfig(),
)
