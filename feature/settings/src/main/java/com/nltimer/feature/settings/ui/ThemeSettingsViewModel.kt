package com.nltimer.feature.settings.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.designsystem.theme.AlphaPreset
import com.nltimer.core.designsystem.theme.AppTheme
import com.nltimer.core.designsystem.theme.BorderPreset
import com.nltimer.core.designsystem.theme.CardColorStrategy
import com.nltimer.core.designsystem.theme.CornerPreset
import com.nltimer.core.designsystem.theme.ExpressivenessPreset
import com.nltimer.core.designsystem.theme.Fonts
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.IconContainerSize
import com.nltimer.core.designsystem.theme.PaletteStyle
import com.nltimer.core.designsystem.theme.StyleConfig
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimerTypography
import com.nltimer.core.designsystem.theme.TopBarMode
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.WavyProgressLevel
import com.nltimer.core.designsystem.theme.toStyleConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题设置页的 ViewModel
 * 管理主题配置的读取与持久化，暴露可观察的 [theme] 状态流供 UI 层订阅
 */
@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val settingsPrefs: SettingsPrefs,
) : ViewModel() {

    /** 当前主题配置的状态流，通过 DataStore 持久化并实时响应变更 */
    val theme: StateFlow<Theme> = settingsPrefs.getThemeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Theme())

    private fun updateTheme(transform: Theme.() -> Theme) {
        viewModelScope.launch {
            settingsPrefs.updateTheme(settingsPrefs.getThemeFlow().first().transform())
        }
    }

    /** 更新种子颜色 */
    fun onSeedColorChange(color: Color) = updateTheme { copy(seedColor = color) }

    /** 切换亮暗模式 */
    fun onThemeSwitch(appTheme: AppTheme) = updateTheme { copy(appTheme = appTheme) }

    /** 切换 AMOLED 纯黑模式 */
    fun onAmoledSwitch(isAmoled: Boolean) = updateTheme { copy(isAmoled = isAmoled) }

    /** 更改调色板风格 */
    fun onPaletteChange(style: PaletteStyle) = updateTheme { copy(paletteStyle = style) }

    /** 切换 Material You 动态取色 */
    fun onMaterialYouToggle(enabled: Boolean) = updateTheme { copy(isMaterialYou = enabled) }

    /** 切换全局字体 */
    fun onFontChange(font: Fonts) = updateTheme { copy(font = font) }

    /** 切换组件边框显示 */
    fun onShowBordersToggle(enabled: Boolean) = updateTheme {
        val newBorderPreset = if (enabled) {
            if (style.borderPreset == BorderPreset.NONE) BorderPreset.STANDARD else style.borderPreset
        } else {
            BorderPreset.NONE
        }
        copy(showBorders = enabled, style = style.copy(borderPreset = newBorderPreset))
    }

    /** 切换主页布局（网格/时间轴） */
    fun onHomeLayoutChange(layout: HomeLayout) = updateTheme { copy(homeLayout = layout) }

    /** 切换侧边滑动时间轴显示 */
    fun onShowTimeSideBarToggle(enabled: Boolean) = updateTheme { copy(showTimeSideBar = enabled) }

    fun onCornerPresetChange(preset: CornerPreset) = updateTheme { copy(style = style.copy(cornerPreset = preset, cornerScale = null)) }

    fun onBorderPresetChange(preset: BorderPreset) = updateTheme {
        val newShowBorders = preset != BorderPreset.NONE
        copy(showBorders = newShowBorders, style = style.copy(borderPreset = preset, borderScale = null))
    }

    fun onAlphaPresetChange(preset: AlphaPreset) = updateTheme { copy(style = style.copy(alphaPreset = preset, alphaScale = null)) }

    fun onCustomCornerScale(scale: Float?) = updateTheme { copy(style = style.copy(cornerScale = scale)) }

    fun onCustomBorderScale(scale: Float?) = updateTheme { copy(style = style.copy(borderScale = scale)) }

    fun onCustomAlphaScale(scale: Float?) = updateTheme { copy(style = style.copy(alphaScale = scale)) }

    fun onExpressivenessChange(preset: ExpressivenessPreset) = updateTheme {
        copy(showBorders = preset.toStyleConfig().borderPreset != BorderPreset.NONE, style = preset.toStyleConfig())
    }

    fun onCardColorStrategyChange(strategy: CardColorStrategy) = updateTheme { copy(style = style.copy(cardColorStrategy = strategy)) }

    fun onIconContainerSizeChange(size: IconContainerSize) = updateTheme { copy(style = style.copy(iconContainerSize = size)) }

    fun onTimerTypographyChange(typography: TimerTypography) = updateTheme { copy(style = style.copy(timerTypography = typography)) }

    fun onWavyProgressChange(level: WavyProgressLevel) = updateTheme { copy(style = style.copy(wavyProgress = level)) }

    fun onResetStyleConfig() = updateTheme { copy(showBorders = true, style = StyleConfig()) }

    fun onTopBarModeChange(mode: TopBarMode) = updateTheme { copy(topBarMode = mode) }

    fun onBottomBarModeChange(mode: BottomBarMode) = updateTheme {
        copy(bottomBarMode = if (mode == BottomBarMode.FLOATING) BottomBarMode.CENTER_FAB else mode)
    }

    fun onImmersiveToggle(enabled: Boolean) = updateTheme { copy(isImmersive = enabled) }

    fun onTopBarHazeToggle(enabled: Boolean) = updateTheme { copy(topBarHaze = enabled) }
}
