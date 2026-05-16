package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nltimer.app.experimental.ai_inter.viewmodel.AiInterViewModel
import com.nltimer.core.designsystem.component.GroupCard
import com.nltimer.core.designsystem.component.PlaceholderScreen
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderConfigRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val modelsError by viewModel.modelsError.collectAsState()

    var showModelSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(modelsError) {
        modelsError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearModelsError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp + LocalImmersiveTopPadding.current,
                end = 16.dp,
                bottom = 24.dp
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
                            value = config.apiAddress,
                            onValueChange = {
                                viewModel.updateConfig(it, config.apiPath, config.apiKey, config.modelName)
                            },
                            label = { Text("API 地址") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = config.apiPath,
                            onValueChange = {
                                viewModel.updateConfig(config.apiAddress, it, config.apiKey, config.modelName)
                            },
                            label = { Text("API 路径") },
                            modifier = Modifier.fillMaxWidth()
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
                            value = config.apiKey,
                            onValueChange = {
                                viewModel.updateConfig(config.apiAddress, config.apiPath, it, config.modelName)
                            },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = config.modelName,
                            onValueChange = {
                                viewModel.updateConfig(config.apiAddress, config.apiPath, config.apiKey, it)
                            },
                            label = { Text("当前模型") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = {
                                viewModel.fetchModels()
                                showModelSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingModels && config.apiAddress.isNotBlank()
                        ) {
                            if (isLoadingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("获取模型列表")
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                var query by remember { mutableStateOf("") }
                val filteredModels = remember(availableModels, query) {
                    if (query.isBlank()) availableModels
                    else availableModels.filter { it.contains(query, ignoreCase = true) }
                }
                val headerText = when {
                    isLoadingModels -> "正在获取模型列表…"
                    availableModels.isEmpty() -> "未获取到任何模型"
                    query.isNotBlank() -> "匹配模型 (${filteredModels.size}/${availableModels.size})"
                    else -> "可用模型 (${availableModels.size})"
                }
                Text(headerText, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoadingModels -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "请求中…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    availableModels.isEmpty() -> {
                        Text(
                            text = modelsError ?: "服务端返回为空，请检查 API 地址是否包含 /v1 等前缀",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("搜索模型…") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (filteredModels.isEmpty()) {
                            Text(
                                "无匹配的模型",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                items(filteredModels) { model ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateConfig(config.apiAddress, config.apiPath, config.apiKey, model)
                                                showModelSheet = false
                                            },
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = model,
                                                style = MaterialTheme.typography.bodyMedium
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCallLogsRoute(
    onNavigateToLogDetail: (Long) -> Unit = {},
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "最近调用 (${logs.size})", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(Icons.Default.Delete, contentDescription = "清除日志")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                GroupCard(
                    modifier = Modifier.clickable {
                        onNavigateToLogDetail(log.id)
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = log.status,
                                color = if (log.status == "Success") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[${log.type}] ${log.model}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        if (log.tools.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                log.tools.split(",").forEach { tool ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(tool.trim(), fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = log.prompt.take(100) + if (log.prompt.length > 100) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${log.durationMs}ms", style = MaterialTheme.typography.labelSmall)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiPromptConfigRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    
    var promptNotes by remember(config.promptNotes) { mutableStateOf(config.promptNotes) }
    var promptTaskGen by remember(config.promptTaskGen) { mutableStateOf(config.promptTaskGen) }
    var promptChat by remember(config.promptChat) { mutableStateOf(config.promptChat) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp + LocalImmersiveTopPadding.current,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PromptEditCard(
                title = "主页备注解析",
                value = promptNotes,
                onValueChange = { 
                    promptNotes = it
                    viewModel.updatePrompts(it, promptTaskGen, promptChat)
                }
            )
        }
        item {
            PromptEditCard(
                title = "任务自动生成",
                value = promptTaskGen,
                onValueChange = { 
                    promptTaskGen = it
                    viewModel.updatePrompts(promptNotes, it, promptChat)
                }
            )
        }
        item {
            PromptEditCard(
                title = "对话助手",
                value = promptChat,
                onValueChange = { 
                    promptChat = it
                    viewModel.updatePrompts(promptNotes, promptTaskGen, it)
                }
            )
        }
    }
}

@Composable
private fun PromptEditCard(title: String, value: String, onValueChange: (String) -> Unit) {
    GroupCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("（提示词当前为空，以避免预设偏见）") }
            )
        }
    }
}

@Composable
fun AiTestChatRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val config by viewModel.config.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingContent) {
        val targetIndex = (messages.size - 1).coerceAtLeast(0) + if (isSending) 1 else 0
        if (targetIndex >= 0 && (messages.isNotEmpty() || isSending)) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = LocalImmersiveTopPadding.current)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (config.modelName.isNotBlank()) "模型：${config.modelName}" else "未选择模型",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (messages.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.Delete, contentDescription = "清空对话")
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isSending) {
                item {
                    StreamingBubble(streamingContent)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入测试消息...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isSending) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                enabled = !isSending
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isSending) {
                IconButton(onClick = { viewModel.stopStreaming() }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "停止生成",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content.ifEmpty { "正在生成…" },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ChatBubble(msg: com.nltimer.app.experimental.ai_inter.viewmodel.ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AiToolsListRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val tools = remember { viewModel.getAllTools() }

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
        if (tools.isEmpty()) {
            item {
                GroupCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "尚未注册任何工具",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(tools) { tool ->
                GroupCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tool.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = tool.category.name,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (tool.parameters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "参数：",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            tool.parameters.forEach { param ->
                                Text(
                                    text = "  · ${param.name}${if (param.required) "*" else ""}: ${param.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
