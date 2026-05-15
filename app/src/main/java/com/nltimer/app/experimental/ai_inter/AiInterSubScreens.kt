package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.designsystem.component.GroupCard
import com.nltimer.core.designsystem.component.PlaceholderScreen
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton

@Composable
fun AiProviderConfigRoute() {
    var apiAddress by remember { mutableStateOf("https://integrate.api.nvidia.com/v1") }
    var apiPath by remember { mutableStateOf("/chat/completions") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("openai/gpt-oss-120b") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp + LocalImmersiveTopPadding.current,
            end = 16.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "接口设置",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            GroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = apiAddress,
                        onValueChange = { apiAddress = it },
                        label = { Text("API 地址") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiPath,
                        onValueChange = { apiPath = it },
                        label = { Text("API 路径") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("/chat/completions") }
                    )
                }
            }
        }

        item {
            Text(
                text = "鉴权与模型",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            GroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-...") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("当前模型") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* TODO: Fetch models */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text("获取模型列表")
                    }
                }
            }
        }
    }
}

@Composable
fun AiToolsListRoute() {
    val tools = listOf(
        "behavior_start" to "开始一个新的行为记录",
        "behavior_stop" to "停止当前正在进行的行为",
        "note_parse" to "解析复杂备注内容",
        "search_activities" to "根据关键词搜索现有活动"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp + LocalImmersiveTopPadding.current,
            end = 16.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tools.size) { index ->
            val (name, desc) = tools[index]
            GroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AiCallLogsRoute() {
    val mockLogs = remember {
        listOf(
            LogEntry("2026-05-15 20:10", "Chat", "Success", "1.2s"),
            LogEntry("2026-05-15 20:12", "Task", "Failed", "0.5s"),
            LogEntry("2026-05-15 20:15", "Plan", "Success", "2.1s"),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp + LocalImmersiveTopPadding.current,
            end = 16.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mockLogs.size) { index ->
            val log = mockLogs[index]
            GroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = log.time, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "类型: ${log.type}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "状态: ${log.status}",
                            color = if (log.status == "Success") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(text = log.duration, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class LogEntry(val time: String, val type: String, val status: String, val duration: String)

@Composable
fun AiPromptConfigRoute() {
    val promptPoints = listOf(
        "主页备注解析" to "负责将自然语言备注转化为结构化行为数据",
        "任务自动生成" to "负责基于当前状态建议下一步行动",
        "对话助手" to "通用对话与系统能力调用"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp + LocalImmersiveTopPadding.current,
            end = 16.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(promptPoints.size) { index ->
            val (title, subtitle) = promptPoints[index]
            SettingsEntryCard(
                icon = Icons.Default.Settings,
                title = title,
                subtitle = subtitle,
                onClick = { /* TODO: Edit prompt */ }
            )
        }
    }
}

@Composable
fun AiTestChatRoute() {
    PlaceholderScreen(
        icon = Icons.AutoMirrored.Filled.Chat,
        title = "测试对话"
    )
}
