package com.nltimer.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding

@Composable
fun AdvancedSettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLogList: () -> Unit,
) {
    AdvancedSettingsScreen(
        _onNavigateBack = onNavigateBack,
        onNavigateToLogList = onNavigateToLogList,
    )
}

@Composable
fun AdvancedSettingsScreen(
    modifier: Modifier = Modifier,
    _onNavigateBack: () -> Unit = {},
    onNavigateToLogList: () -> Unit = {},
) {
    SettingsSubpageContainer(modifier = modifier) {
        item {
            SettingsEntryCard(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "日志记录",
                subtitle = "查看应用运行日志（图标需求、调用记录等）",
                onClick = onNavigateToLogList,
            )
        }
    }
}
