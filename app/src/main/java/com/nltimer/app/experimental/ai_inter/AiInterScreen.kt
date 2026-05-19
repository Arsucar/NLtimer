package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nltimer.app.navigation.NLtimerRoutes
import com.nltimer.core.designsystem.component.SettingsEntryCard

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
        onNavigateToAssistantChat = { navController.navigate(NLtimerRoutes.AI_ASSISTANT_CHAT) },
    )
}

@Composable
fun AiInterScreen(
    onNavigateToProviderConfig: () -> Unit,
    onNavigateToToolsList: () -> Unit,
    onNavigateToCallLogs: () -> Unit,
    onNavigateToPromptConfig: () -> Unit,
    onNavigateToTestChat: () -> Unit,
    onNavigateToAssistantChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsEntryCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "AI 助手对话",
                subtitle = "正式对话界面：多会话 / Markdown / 导出",
                onClick = onNavigateToAssistantChat,
            )
        }

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
                icon = Icons.Default.Science,
                title = "测试对话",
                subtitle = "开发调试视图：当轮流式 + 工具调用日志",
                onClick = onNavigateToTestChat,
            )
        }
    }
}
