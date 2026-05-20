package com.nltimer.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding

@Composable
fun SettingsRoute(
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToDialogConfig: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToHomeLayoutConfig: () -> Unit = {},
    onNavigateToColorPalette: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit = {},
) {
    SettingsScreen(
        onNavigateToThemeSettings = onNavigateToThemeSettings,
        onNavigateToDialogConfig = onNavigateToDialogConfig,
        onNavigateToDataManagement = onNavigateToDataManagement,
        onNavigateToHomeLayoutConfig = onNavigateToHomeLayoutConfig,
        onNavigateToColorPalette = onNavigateToColorPalette,
        onNavigateToAdvancedSettings = onNavigateToAdvancedSettings,
    )
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToDialogConfig: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToHomeLayoutConfig: () -> Unit = {},
    onNavigateToColorPalette: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp + LocalImmersiveTopPadding.current, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsEntryCard(
                icon = Icons.Default.Palette,
                title = "主题配置",
                subtitle = "自定义应用主题、颜色与字体",
                onClick = onNavigateToThemeSettings,
            )
        }

        item {
            SettingsEntryCard(
                icon = Icons.Default.ColorLens,
                title = "色板",
                subtitle = "预览 MD3 配色 token 的亮/暗效果（开发者）",
                onClick = onNavigateToColorPalette,
            )
        }

        item {
            SettingsEntryCard(
                icon = Icons.Default.Dashboard,
                title = "弹窗配置",
                subtitle = "调整弹窗网格布局与显示方式",
                onClick = onNavigateToDialogConfig,
            )
        }

        item {
            SettingsEntryCard(
                icon = Icons.Default.ViewCarousel,
                title = "主页布局配置",
                subtitle = "自定义网格列数、行高、间距等布局参数",
                onClick = onNavigateToHomeLayoutConfig,
            )
        }

        item {
            SettingsEntryCard(
                icon = Icons.Default.Storage,
                title = "数据管理",
                subtitle = "导出、导入与迁移应用数据",
                onClick = onNavigateToDataManagement,
            )
        }

        item {
            SettingsEntryCard(
                icon = Icons.Default.Tune,
                title = "高级",
                subtitle = "日志记录等高级功能",
                onClick = onNavigateToAdvancedSettings,
            )
        }
    }
}
