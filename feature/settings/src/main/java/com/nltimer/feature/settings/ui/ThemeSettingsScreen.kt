package com.nltimer.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.dynamicColorScheme
import com.nltimer.core.designsystem.component.ColorPickerDialog
import com.nltimer.core.designsystem.theme.AppTheme
import com.nltimer.core.designsystem.theme.Fonts
import com.nltimer.core.designsystem.theme.PaletteStyle
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.AlphaPreset
import com.nltimer.core.designsystem.theme.BorderPreset
import com.nltimer.core.designsystem.theme.BorderTokens
import com.nltimer.core.designsystem.theme.CardColorStrategy
import com.nltimer.core.designsystem.theme.CornerPreset
import com.nltimer.core.designsystem.theme.ExpressivenessPreset
import com.nltimer.core.designsystem.theme.IconContainerSize
import com.nltimer.core.designsystem.theme.ShapeTokens
import com.nltimer.core.designsystem.theme.StyleConfig
import com.nltimer.core.designsystem.theme.TimerTypography
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.TopBarMode
import com.nltimer.core.designsystem.theme.WavyProgressLevel
import com.nltimer.core.designsystem.theme.appBorder
import com.nltimer.core.designsystem.theme.effectiveAlphaScale
import com.nltimer.core.designsystem.theme.effectiveBorderScale
import com.nltimer.core.designsystem.theme.effectiveCornerScale
import com.nltimer.core.designsystem.theme.styledAlpha
import com.nltimer.core.designsystem.theme.styledBorder
import com.nltimer.core.designsystem.theme.styledCorner
import com.nltimer.core.designsystem.theme.endItemShape
import com.nltimer.core.designsystem.theme.leadingItemShape
import com.nltimer.core.designsystem.theme.listItemColors
import com.nltimer.core.designsystem.theme.middleItemShape
import com.nltimer.core.designsystem.theme.toDisplayString
import com.nltimer.core.designsystem.theme.toFontRes
import com.nltimer.core.designsystem.theme.toMPaletteStyle
import com.nltimer.core.designsystem.theme.toStyleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 主题设置页路由入口，负责初始化ViewModel并将状态与回调绑定到UI层。
 * @param viewModel 主题设置ViewModel，通过Hilt自动注入
 * @param onNavigateBack 返回上一级页面的导航回调
 */
@Composable
fun ThemeSettingsRoute(
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
) {
    // 从ViewModel中收集主题状态，重组时自动订阅
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    // 将ViewModel的方法指针传入UI层，分离逻辑与视图
    ThemeSettingsScreen(
        theme = theme,
        onSeedColorChange = viewModel::onSeedColorChange,
        onThemeSwitch = viewModel::onThemeSwitch,
        onAmoledSwitch = viewModel::onAmoledSwitch,
        onPaletteChange = viewModel::onPaletteChange,
        onMaterialYouToggle = viewModel::onMaterialYouToggle,
        onFontChange = viewModel::onFontChange,
        onShowBordersToggle = viewModel::onShowBordersToggle,
        onCornerPresetChange = viewModel::onCornerPresetChange,
        onBorderPresetChange = viewModel::onBorderPresetChange,
        onAlphaPresetChange = viewModel::onAlphaPresetChange,
        onCustomCornerScale = viewModel::onCustomCornerScale,
        onCustomBorderScale = viewModel::onCustomBorderScale,
        onCustomAlphaScale = viewModel::onCustomAlphaScale,
        onExpressivenessChange = viewModel::onExpressivenessChange,
        onCardColorStrategyChange = viewModel::onCardColorStrategyChange,
        onIconContainerSizeChange = viewModel::onIconContainerSizeChange,
        onTimerTypographyChange = viewModel::onTimerTypographyChange,
        onWavyProgressChange = viewModel::onWavyProgressChange,
        onResetStyleConfig = viewModel::onResetStyleConfig,
        onTopBarModeChange = viewModel::onTopBarModeChange,
        onBottomBarModeChange = viewModel::onBottomBarModeChange,
        onImmersiveToggle = viewModel::onImmersiveToggle,
    )
}

/**
 * 主题设置页面主界面，提供主题模式、Material You、字体、AMOLED纯黑、
 * 种子颜色、配色方案、边框显示等设置项。
 * @param theme 当前主题配置
 * @param onSeedColorChange 种子颜色变更回调
 * @param onThemeSwitch 主题模式（亮/暗/跟随系统）切换回调
 * @param onAmoledSwitch AMOLED纯黑模式开关回调
 * @param onPaletteChange 配色方案样式变更回调
 * @param onMaterialYouToggle Material You动态取色开关回调
 * @param onFontChange 字体选择变更回调
 * @param onShowBordersToggle 网格卡片边框显示开关回调
 * @param onNavigateBack 导航返回回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    theme: Theme,
    onSeedColorChange: (Color) -> Unit,
    onThemeSwitch: (AppTheme) -> Unit,
    onAmoledSwitch: (Boolean) -> Unit,
    onPaletteChange: (PaletteStyle) -> Unit,
    onMaterialYouToggle: (Boolean) -> Unit,
    onFontChange: (Fonts) -> Unit,
    onShowBordersToggle: (Boolean) -> Unit,
    onCornerPresetChange: (CornerPreset) -> Unit,
    onBorderPresetChange: (BorderPreset) -> Unit,
    onAlphaPresetChange: (AlphaPreset) -> Unit,
    onCustomCornerScale: (Float?) -> Unit,
    onCustomBorderScale: (Float?) -> Unit,
    onCustomAlphaScale: (Float?) -> Unit,
    onExpressivenessChange: (ExpressivenessPreset) -> Unit,
    onCardColorStrategyChange: (CardColorStrategy) -> Unit,
    onIconContainerSizeChange: (IconContainerSize) -> Unit,
    onTimerTypographyChange: (TimerTypography) -> Unit,
    onWavyProgressChange: (WavyProgressLevel) -> Unit,
    onResetStyleConfig: () -> Unit,
    onTopBarModeChange: (TopBarMode) -> Unit,
    onBottomBarModeChange: (BottomBarMode) -> Unit,
    onImmersiveToggle: (Boolean) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier,
) {
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = theme.seedColor,
            onSelect = onSeedColorChange,
            onDismiss = { showColorPicker = false }
        )
    }

    SettingsSubpageContainer(
        containerColor = containerColor,
        modifier = modifier,
    ) {
        ThemeSettingsContent(
            theme = theme,
            onSeedColorChange = onSeedColorChange,
            onThemeSwitch = onThemeSwitch,
            onAmoledSwitch = onAmoledSwitch,
            onPaletteChange = onPaletteChange,
            onMaterialYouToggle = onMaterialYouToggle,
            onFontChange = onFontChange,
            onShowBordersToggle = onShowBordersToggle,
            onCornerPresetChange = onCornerPresetChange,
            onBorderPresetChange = onBorderPresetChange,
            onAlphaPresetChange = onAlphaPresetChange,
            onCustomCornerScale = onCustomCornerScale,
            onCustomBorderScale = onCustomBorderScale,
            onCustomAlphaScale = onCustomAlphaScale,
            onExpressivenessChange = onExpressivenessChange,
            onCardColorStrategyChange = onCardColorStrategyChange,
            onIconContainerSizeChange = onIconContainerSizeChange,
            onTimerTypographyChange = onTimerTypographyChange,
            onWavyProgressChange = onWavyProgressChange,
            onResetStyleConfig = onResetStyleConfig,
            onTopBarModeChange = onTopBarModeChange,
            onBottomBarModeChange = onBottomBarModeChange,
            onImmersiveToggle = onImmersiveToggle,
            showColorPicker = showColorPicker,
            onShowColorPicker = { showColorPicker = it },
        )
    }
}

private fun LazyListScope.ThemeSettingsContent(
    theme: Theme,
    onSeedColorChange: (Color) -> Unit,
    onThemeSwitch: (AppTheme) -> Unit,
    onAmoledSwitch: (Boolean) -> Unit,
    onPaletteChange: (PaletteStyle) -> Unit,
    onMaterialYouToggle: (Boolean) -> Unit,
    onFontChange: (Fonts) -> Unit,
    onShowBordersToggle: (Boolean) -> Unit,
    onCornerPresetChange: (CornerPreset) -> Unit,
    onBorderPresetChange: (BorderPreset) -> Unit,
    onAlphaPresetChange: (AlphaPreset) -> Unit,
    onCustomCornerScale: (Float?) -> Unit,
    onCustomBorderScale: (Float?) -> Unit,
    onCustomAlphaScale: (Float?) -> Unit,
    onExpressivenessChange: (ExpressivenessPreset) -> Unit,
    onCardColorStrategyChange: (CardColorStrategy) -> Unit,
    onIconContainerSizeChange: (IconContainerSize) -> Unit,
    onTimerTypographyChange: (TimerTypography) -> Unit,
    onWavyProgressChange: (WavyProgressLevel) -> Unit,
    onResetStyleConfig: () -> Unit,
    onTopBarModeChange: (TopBarMode) -> Unit,
    onBottomBarModeChange: (BottomBarMode) -> Unit,
    onImmersiveToggle: (Boolean) -> Unit,
    showColorPicker: Boolean,
    onShowColorPicker: (Boolean) -> Unit,
) {
    item {
        val isDark = when (theme.appTheme) {
            AppTheme.SYSTEM -> isSystemInDarkTheme()
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
        }

        val schemes by produceState<Map<PaletteStyle, ColorScheme>>(
            initialValue = emptyMap(),
            key1 = theme.seedColor,
            key2 = isDark,
            key3 = theme.isAmoled,
        ) {
            value = withContext(Dispatchers.Default) {
                PaletteStyle.entries.associateWith { style ->
                    dynamicColorScheme(
                        seedColor = theme.seedColor,
                        isDark = isDark,
                        isAmoled = theme.isAmoled,
                        style = style.toMPaletteStyle(),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // 主题模式切换区：使用FilterChip选择亮/暗/跟随系统
                    Column(modifier = Modifier.clip(leadingItemShape())) {
                        ListItem(
                            headlineContent = { Text(text = "主题模式") },
                            colors = listItemColors(),
                            leadingContent = {
                                Icon(
                                    imageVector = when (theme.appTheme) {
                                        AppTheme.SYSTEM -> if (isSystemInDarkTheme())
                                            Icons.Default.DarkMode
                                        else
                                            Icons.Default.LightMode
                                        AppTheme.DARK -> Icons.Default.DarkMode
                                        AppTheme.LIGHT -> Icons.Default.LightMode
                                    },
                                    contentDescription = null,
                                )
                            },
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(listItemColors().containerColor)
                                .padding(start = 52.dp, end = 16.dp, bottom = 8.dp),
                        ) {
                            AppTheme.entries.toList().forEach { appTheme ->
                                FilterChip(
                                    selected = appTheme == theme.appTheme,
                                    onClick = { onThemeSwitch(appTheme) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    label = {
                                        Text(
                                            text = appTheme.toDisplayString(),
                                            modifier = Modifier.basicMarquee(),
                                            maxLines = 1,
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Material You 开关：开启后使用系统壁纸动态取色（需要API 31+）
                    ListItem(
                        headlineContent = { Text(text = "Material You") },
                        supportingContent = { Text(text = "使用系统壁纸颜色") },
                        trailingContent = {
                            Switch(
                                checked = theme.isMaterialYou,
                                onCheckedChange = { onMaterialYouToggle(it) },
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier.clip(middleItemShape()),
                    )

                    // 字体选择区：列出所有可用字体，选中项高亮，预览时应用实际字体
                    Column(modifier = Modifier.clip(middleItemShape())) {
                        ListItem(
                            headlineContent = { Text(text = "字体") },
                            colors = listItemColors(),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.FontDownload,
                                    contentDescription = null,
                                )
                            },
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(listItemColors().containerColor)
                                .padding(start = 52.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Fonts.entries.toList().forEach { font ->
                                FilterChip(
                                    selected = theme.font == font,
                                    onClick = { onFontChange(font) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    label = {
                                        Text(
                                            text = font.toDisplayString(),
                                            fontFamily = font.toFontRes()?.let { FontFamily(Font(it)) }
                                                ?: FontFamily.Default,
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // AMOLED纯黑开关：深色模式下将背景替换为纯黑以节省屏幕功耗
                    ListItem(
                        headlineContent = { Text(text = "AMOLED 纯黑") },
                        supportingContent = { Text(text = "在深色模式下使用纯黑背景") },
                        trailingContent = {
                            Switch(
                                checked = theme.isAmoled,
                                onCheckedChange = { onAmoledSwitch(it) },
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier.clip(middleItemShape()),
                    )

                    // 种子颜色选择器：仅当Material You关闭时可见，点击弹出颜色选择对话框
                    AnimatedVisibility(visible = !theme.isMaterialYou) {
                        ListItem(
                            headlineContent = { Text(text = "主题色") },
                            supportingContent = { Text(text = "选择应用的主色调") },
                            trailingContent = {
                                IconButton(
                                    onClick = { onShowColorPicker(true) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = theme.seedColor,
                                        contentColor = contentColorFor(theme.seedColor),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Colorize,
                                        contentDescription = "选择颜色",
                                    )
                                }
                            },
                            colors = listItemColors(),
                            modifier = Modifier.clip(middleItemShape()),
                        )
                    }

                    // Palette style picker
                    PaletteStyleSection(
                        schemes = schemes,
                        selectedStyle = theme.paletteStyle,
                        onStyleClick = onPaletteChange,
                    )

                    // Border toggle
                    ListItem(
                        headlineContent = { Text(text = "显示边框") },
                        supportingContent = { Text(text = "在网格卡片上显示轮廓边框") },
                        trailingContent = {
                            Switch(
                                checked = theme.showBorders,
                                onCheckedChange = { onShowBordersToggle(it) },
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier.clip(middleItemShape()),
                     )

                    Column(modifier = Modifier.clip(middleItemShape())) {
                        ListItem(
                            headlineContent = { Text(text = "栏位模式") },
                            colors = listItemColors(),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(listItemColors().containerColor)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                TopBarMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = TopBarMode.entries.size
                                        ),
                                        onClick = { onTopBarModeChange(mode) },
                                        selected = theme.topBarMode == mode,
                                    ) {
                                        Text(text = mode.toDisplayString())
                                    }
                                }
                            }

                            val bottomBarModes = remember { listOf(BottomBarMode.STANDARD, BottomBarMode.CENTER_FAB) }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                bottomBarModes.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = bottomBarModes.size
                                        ),
                                        onClick = { onBottomBarModeChange(mode) },
                                        selected = theme.bottomBarMode == mode,
                                    ) {
                                        Text(text = mode.toDisplayString())
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    ListItem(
                        headlineContent = { Text(text = "沉浸式页面") },
                        supportingContent = { Text(text = "内容延伸到状态栏下方") },
                        trailingContent = {
                            Switch(
                                checked = theme.isImmersive,
                                onCheckedChange = { onImmersiveToggle(it) },
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier.clip(middleItemShape()),
                    )

                    ExpressivenessSection(
                        styleConfig = theme.style,
                        onExpressivenessChange = onExpressivenessChange,
                        onCardColorStrategyChange = onCardColorStrategyChange,
                        onIconContainerSizeChange = onIconContainerSizeChange,
                        onTimerTypographyChange = onTimerTypographyChange,
                        onWavyProgressChange = onWavyProgressChange,
                    )

                    StyleConfigSection(
                        styleConfig = theme.style,
                        onCornerPresetChange = onCornerPresetChange,
                        onBorderPresetChange = onBorderPresetChange,
                        onAlphaPresetChange = onAlphaPresetChange,
                        onCustomCornerScale = onCustomCornerScale,
                        onCustomBorderScale = onCustomBorderScale,
                        onCustomAlphaScale = onCustomAlphaScale,
                        onResetStyleConfig = onResetStyleConfig,
                    )
        }
    }
}

@Composable
private fun PaletteStyleSection(
    schemes: Map<PaletteStyle, ColorScheme>,
    selectedStyle: PaletteStyle,
    onStyleClick: (PaletteStyle) -> Unit,
) {
    Column(modifier = Modifier.clip(middleItemShape())) {
        ListItem(
            headlineContent = { Text(text = "配色方案") },
            colors = listItemColors(),
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                )
            },
        )
        if (schemes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(listItemColors().containerColor)
                    .padding(start = 52.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaletteStyle.entries.forEach { style ->
                    key(style) {
                        val scheme = schemes[style]
                        if (scheme != null) {
                            val selected = selectedStyle == style
                            val colors = remember(scheme) {
                                listOf(
                                    scheme.primary,
                                    scheme.primaryContainer,
                                    scheme.secondary,
                                    scheme.secondaryContainer,
                                    scheme.tertiary,
                                    scheme.tertiaryContainer,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .clickable { onStyleClick(style) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val sweepAngle = 360f / colors.size
                                    colors.forEachIndexed { index, color ->
                                        drawArc(
                                            color = color,
                                            startAngle = index * sweepAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = true,
                                        )
                                    }
                                }

                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(scheme.primary.copy(alpha = 0.7f))
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = scheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StylePreviewRow() {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .background(
                    colorScheme.primaryContainer.copy(alpha = styledAlpha(0.3f)),
                    RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)),
                )
                .appBorder(
                    borderProducer = { BorderStroke(styledBorder(BorderTokens.STANDARD), colorScheme.outlineVariant) },
                    shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_MEDIUM)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "卡片",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurface.copy(alpha = styledAlpha(0.8f)),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .background(
                    colorScheme.primaryContainer.copy(alpha = styledAlpha(0.15f)),
                    RoundedCornerShape(styledCorner(ShapeTokens.CORNER_LARGE)),
                )
                .appBorder(
                    borderProducer = { BorderStroke(styledBorder(BorderTokens.THIN), colorScheme.outlineVariant.copy(alpha = styledAlpha(0.5f))) },
                    shape = RoundedCornerShape(styledCorner(ShapeTokens.CORNER_LARGE)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "容器",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurface.copy(alpha = styledAlpha(0.6f)),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .background(
                    colorScheme.primary.copy(alpha = styledAlpha(0.3f)),
                    RoundedCornerShape(styledCorner(ShapeTokens.CORNER_PILL)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "胶囊",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onPrimaryContainer.copy(alpha = styledAlpha(0.7f)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressivenessSection(
    styleConfig: StyleConfig,
    onExpressivenessChange: (ExpressivenessPreset) -> Unit,
    onCardColorStrategyChange: (CardColorStrategy) -> Unit,
    onIconContainerSizeChange: (IconContainerSize) -> Unit,
    onTimerTypographyChange: (TimerTypography) -> Unit,
    onWavyProgressChange: (WavyProgressLevel) -> Unit,
) {
    val currentPreset = styleConfig.expressiveness
    val presetConfig = currentPreset.toStyleConfig()
    val isCustom = styleConfig.cardColorStrategy != presetConfig.cardColorStrategy
            || styleConfig.iconContainerSize != presetConfig.iconContainerSize
            || styleConfig.timerTypography != presetConfig.timerTypography
            || styleConfig.wavyProgress != presetConfig.wavyProgress

    Column(modifier = Modifier.clip(middleItemShape())) {
        ListItem(
            headlineContent = { Text(text = "表达力") },
            colors = listItemColors(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(listItemColors().containerColor)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ExpressivenessPreset.entries.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ExpressivenessPreset.entries.size),
                        onClick = { onExpressivenessChange(preset) },
                        selected = !isCustom && currentPreset == preset,
                    ) {
                        Text(text = preset.toDisplayString())
                    }
                }
            }

            if (isCustom) {
                Text(
                    text = "自定义",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CardColorStrategy.entries.forEachIndexed { index, strategy ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CardColorStrategy.entries.size),
                        onClick = { onCardColorStrategyChange(strategy) },
                        selected = styleConfig.cardColorStrategy == strategy,
                    ) {
                        Text(text = strategy.toDisplayString())
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                IconContainerSize.entries.forEachIndexed { index, size ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = IconContainerSize.entries.size),
                        onClick = { onIconContainerSizeChange(size) },
                        selected = styleConfig.iconContainerSize == size,
                    ) {
                        Text(text = size.toDisplayString())
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TimerTypography.entries.forEachIndexed { index, typography ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TimerTypography.entries.size),
                        onClick = { onTimerTypographyChange(typography) },
                        selected = styleConfig.timerTypography == typography,
                    ) {
                        Text(text = typography.toDisplayString())
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                WavyProgressLevel.entries.forEachIndexed { index, level ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = WavyProgressLevel.entries.size),
                        onClick = { onWavyProgressChange(level) },
                        selected = styleConfig.wavyProgress == level,
                    ) {
                        Text(text = level.toDisplayString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleConfigSection(
    styleConfig: StyleConfig,
    onCornerPresetChange: (CornerPreset) -> Unit,
    onBorderPresetChange: (BorderPreset) -> Unit,
    onAlphaPresetChange: (AlphaPreset) -> Unit,
    onCustomCornerScale: (Float?) -> Unit,
    onCustomBorderScale: (Float?) -> Unit,
    onCustomAlphaScale: (Float?) -> Unit,
    onResetStyleConfig: () -> Unit,
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Column(modifier = Modifier.clip(endItemShape())) {
        ListItem(
            headlineContent = { Text(text = "样式风格") },
            colors = listItemColors(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(listItemColors().containerColor)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StylePreviewRow()

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CornerPreset.entries.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CornerPreset.entries.size),
                        onClick = { onCornerPresetChange(preset) },
                        selected = styleConfig.cornerPreset == preset,
                    ) {
                        Text(text = preset.toDisplayString())
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                BorderPreset.entries.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = BorderPreset.entries.size),
                        onClick = { onBorderPresetChange(preset) },
                        selected = styleConfig.borderPreset == preset,
                    ) {
                        Text(text = preset.toDisplayString())
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AlphaPreset.entries.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AlphaPreset.entries.size),
                        onClick = { onAlphaPresetChange(preset) },
                        selected = styleConfig.alphaPreset == preset,
                    ) {
                        Text(text = preset.toDisplayString())
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "高级自定义")
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                val cornerScale = styleConfig.effectiveCornerScale()
                val borderScale = styleConfig.effectiveBorderScale()
                val alphaScale = styleConfig.effectiveAlphaScale()
                var localCorner by remember { mutableStateOf(cornerScale) }
                var localBorder by remember { mutableStateOf(borderScale) }
                var localAlpha by remember { mutableStateOf(alphaScale) }
                LaunchedEffect(cornerScale) { localCorner = cornerScale }
                LaunchedEffect(borderScale) { localBorder = borderScale }
                LaunchedEffect(alphaScale) { localAlpha = alphaScale }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "圆角比例: ${"%.2f".format(localCorner)}")
                    Slider(
                        value = localCorner,
                        onValueChange = { localCorner = it },
                        onValueChangeFinished = { onCustomCornerScale(localCorner) },
                        valueRange = 0f..4f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(text = "边框比例: ${"%.2f".format(localBorder)}")
                    Slider(
                        value = localBorder,
                        onValueChange = { localBorder = it },
                        onValueChangeFinished = { onCustomBorderScale(localBorder) },
                        valueRange = 0f..3f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(text = "透明度比例: ${"%.2f".format(localAlpha)}")
                    Slider(
                        value = localAlpha,
                        onValueChange = { localAlpha = it },
                        onValueChangeFinished = { onCustomAlphaScale(localAlpha) },
                        valueRange = 0f..3f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onResetStyleConfig,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "重置为默认")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
