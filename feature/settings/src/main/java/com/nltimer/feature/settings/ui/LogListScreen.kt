package com.nltimer.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding

@Composable
fun LogListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToIconMissLog: () -> Unit,
) {
    LogListScreen(
        _onNavigateBack = onNavigateBack,
        onNavigateToIconMissLog = onNavigateToIconMissLog,
    )
}

@Composable
fun LogListScreen(
    modifier: Modifier = Modifier,
    _onNavigateBack: () -> Unit = {},
    onNavigateToIconMissLog: () -> Unit = {},
) {
    SettingsSubpageContainer(modifier = modifier) {
        item {
            SettingsEntryCard(
                icon = Icons.Default.Brush,
                title = "图标库",
                subtitle = "AI 搜索图标未命中记录，帮助补充图标库",
                onClick = onNavigateToIconMissLog,
            )
        }
    }
}
