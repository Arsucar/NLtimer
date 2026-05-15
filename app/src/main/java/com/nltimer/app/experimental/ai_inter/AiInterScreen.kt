package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.automirrored.filled.Chat
import com.nltimer.app.navigation.NLtimerRoutes
import com.nltimer.core.designsystem.component.BottomBarDragFab
import com.nltimer.core.designsystem.component.LocalNavBarWidth
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.component.rememberDragFabState
import com.nltimer.core.designsystem.theme.BottomBarMode
import com.nltimer.core.designsystem.theme.LocalTheme
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding

@Composable
fun AiInterRoute(
    navController: NavHostController,
) {
    AiInterScreen(
        onNavigateToProviderConfig = { navController.navigate(NLtimerRoutes.AI_PROVIDER_CONFIG) },
        onNavigateToToolsList = { navController.navigate(NLtimerRoutes.AI_TOOLS_LIST) },
        onNavigateToCallLogs = { navController.navigate(NLtimerRoutes.AI_CALL_LOGS) },
        onNavigateToPromptConfig = { navController.navigate(NLtimerRoutes.AI_PROMPT_CONFIG) },
        onNavigateToTestChat = { navController.navigate(NLtimerRoutes.AI_TEST_CHAT) },
    )
}

@Composable
fun AiInterScreen(
    onNavigateToProviderConfig: () -> Unit,
    onNavigateToToolsList: () -> Unit,
    onNavigateToCallLogs: () -> Unit,
    onNavigateToPromptConfig: () -> Unit,
    onNavigateToTestChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragFabState = rememberDragFabState()
    val dragOptions = listOf("新建对话", "查看任务", "生成计划")
    var allExpanded by remember { mutableStateOf(false) }
    
    val navBarWidth = LocalNavBarWidth.current.value
    val isCenterFab = LocalTheme.current.bottomBarMode == BottomBarMode.CENTER_FAB
    val expandFabStartPadding = if (isCenterFab && navBarWidth > 0.dp) {
        navBarWidth / 2f + 40.dp
    } else {
        16.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { 
                dragFabState.boxPositionInWindow = it.positionInWindow() 
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    top = padding.calculateTopPadding() + 12.dp + LocalImmersiveTopPadding.current, 
                    end = 16.dp, 
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SettingsEntryCard(
                        icon = Icons.Default.CloudQueue,
                        title = "提供商配置",
                        subtitle = "设置 AI 模型提供商与 API 密钥",
                        onClick = onNavigateToProviderConfig,
                    )
                }

                item {
                    SettingsEntryCard(
                        icon = Icons.Default.Build,
                        title = "现有工具",
                        subtitle = "查看 AI 可调用的系统工具与能力",
                        onClick = onNavigateToToolsList,
                    )
                }

                item {
                    SettingsEntryCard(
                        icon = Icons.Default.History,
                        title = "调用日志",
                        subtitle = "查看 AI 处理请求的历史记录与耗时",
                        onClick = onNavigateToCallLogs,
                    )
                }

                item {
                    SettingsEntryCard(
                        icon = Icons.Default.Description,
                        title = "提示词配置",
                        subtitle = "针对不同调用点细化配置系统提示词",
                        onClick = onNavigateToPromptConfig,
                    )
                }

                item {
                    SettingsEntryCard(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "测试对话",
                        subtitle = "在当前配置下测试 AI 响应与工具���用",
                        onClick = onNavigateToTestChat,
                    )
                }
            }
        }

        BottomBarDragFab(
            state = dragFabState,
            icon = Icons.Default.Add,
            dragOptions = dragOptions,
            onClick = { /* Handle click */ },
            onOptionSelected = { /* Handle option */ }
        )

        FilledTonalIconButton(
            onClick = { allExpanded = !allExpanded },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = expandFabStartPadding, bottom = 8.dp)
                .size(56.dp),
        ) {
            Icon(
                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                contentDescription = if (allExpanded) "一键收纳" else "一键展开",
            )
        }
    }
}
