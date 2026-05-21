package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.clickable
import com.nltimer.core.ai.toolcall.AiChatToolHelper
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Slider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nltimer.app.experimental.ai_inter.viewmodel.AiInterViewModel
import com.nltimer.app.experimental.ai_inter.viewmodel.ChatMessage
import com.nltimer.core.ai.toolcall.ToolCallRecord
import com.nltimer.core.tools.ToolCategory
import com.nltimer.core.designsystem.component.GroupCard
import com.nltimer.core.designsystem.component.PlaceholderScreen
import com.nltimer.core.designsystem.component.SettingsEntryCard
import com.nltimer.core.designsystem.theme.LocalImmersiveTopPadding
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderConfigRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val modelsError by viewModel.modelsError.collectAsStateWithLifecycle()

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

            item {
                Text(
                    text = "高级配置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                GroupCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "工具调用轮数上限",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI 单次对话中最多执行 ${config.maxToolRounds} 轮工具调用（1–15）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = config.maxToolRounds.toFloat(),
                            onValueChange = {
                                viewModel.updateMaxToolRounds(it.toInt())
                            },
                            valueRange = 1f..15f,
                            steps = 13,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                GroupCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "批量操作上限",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "批量工具单次最多处理 ${config.maxBatchSize} 条数据（1–200）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = config.maxBatchSize.toFloat(),
                            onValueChange = {
                                viewModel.updateMaxBatchSize(it.toInt())
                            },
                            valueRange = 1f..200f,
                            steps = 198,
                            modifier = Modifier.fillMaxWidth()
                        )
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
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val dateFormat = remember { DateTimeFormatter.ofPattern("MM-dd HH:mm:ss") }

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
                                text = dateFormat.format(Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()),
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
    val config by viewModel.config.collectAsStateWithLifecycle()
    
    var promptNotes by remember(config.promptNotes) { mutableStateOf(config.promptNotes) }
    var promptTaskGen by remember(config.promptTaskGen) { mutableStateOf(config.promptTaskGen) }
    var promptChat by remember(config.promptChat) { mutableStateOf(config.promptChat) }
    var promptSystem by remember(config.promptSystem) { mutableStateOf(config.promptSystem.ifBlank { AiChatToolHelper.TOOLS_SYSTEM_PROMPT }) }

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
                title = "系统提示词",
                value = promptSystem,
                onValueChange = {
                    promptSystem = it
                    viewModel.updatePrompts(promptNotes, promptTaskGen, promptChat, it)
                }
            )
        }
        item {
            PromptEditCard(
                title = "主页备注解析",
                value = promptNotes,
                onValueChange = { 
                    promptNotes = it
                    viewModel.updatePrompts(it, promptTaskGen, promptChat, promptSystem)
                }
            )
        }
        item {
            PromptEditCard(
                title = "任务自动生成",
                value = promptTaskGen,
                onValueChange = { 
                    promptTaskGen = it
                    viewModel.updatePrompts(promptNotes, it, promptChat, promptSystem)
                }
            )
        }
        item {
            PromptEditCard(
                title = "对话助手",
                value = promptChat,
                onValueChange = { 
                    promptChat = it
                    viewModel.updatePrompts(promptNotes, promptTaskGen, it, promptSystem)
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
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val streamingState by viewModel.streamingState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingState) {
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
                    StreamingBubble(
                        reasoning = streamingState.reasoning,
                        content = streamingState.content,
                        toolCalls = streamingState.toolCalls,
                    )
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
private fun StreamingBubble(
    reasoning: String,
    content: String,
    toolCalls: List<ToolCallRecord>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (reasoning.isNotEmpty()) {
                    ReasoningBlock(reasoning, defaultExpanded = false)
                }
                if (toolCalls.isNotEmpty()) {
                    if (reasoning.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                    ToolCallsBlock(toolCalls, defaultExpanded = false)
                }
                if (content.isNotEmpty()) {
                    if (reasoning.isNotEmpty() || toolCalls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    MarkdownText(
                        markdown = content,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (reasoning.isEmpty() && toolCalls.isEmpty() && content.isEmpty()) {
                    Text(
                        text = "正在生成…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (isUser) {
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (msg.reasoning.isNotEmpty()) {
                        ReasoningBlock(msg.reasoning, defaultExpanded = false)
                    }
                    if (msg.toolCalls.isNotEmpty()) {
                        if (msg.reasoning.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                        ToolCallsBlock(msg.toolCalls, defaultExpanded = false)
                    }
                    if (msg.reasoning.isNotEmpty() || msg.toolCalls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    MarkdownText(
                        markdown = msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/**
 * 折叠的"思考过程"区块；点击 header 切换展开。默认折叠以减少对正文的干扰。
 */
@Composable
private fun ReasoningBlock(reasoning: String, defaultExpanded: Boolean = false) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "思考过程",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                MarkdownText(
                    markdown = reasoning,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 折叠的"调用工具"区块；点击 header 切换展开，展开后逐条列出工具调用 + 结果。
 */
@Composable
private fun ToolCallsBlock(toolCalls: List<ToolCallRecord>, defaultExpanded: Boolean = false) {
    var expanded by remember(toolCalls.size) { mutableStateOf(defaultExpanded) }
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "调用工具 (${toolCalls.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    toolCalls.forEach { call ->
                        ToolCallCard(call)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCallCard(call: ToolCallRecord) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (call.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (call.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = call.name.ifEmpty { "(未知工具)" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${call.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (call.arguments.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "参数：${call.arguments}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "结果：${call.result}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AiToolsListRoute(
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val tools = remember { viewModel.getAllTools() }
    val groupedTools = remember(tools) { tools.groupBy { it.category } }

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
            ToolCategory.entries.forEach { category ->
                val categoryTools = groupedTools[category] ?: return@forEach
                item(key = "header_${category.name}") {
                    Text(
                        text = categoryDisplayName(category),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(
                    items = categoryTools,
                    key = { it.name }
                ) { tool ->
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
}

private fun categoryDisplayName(category: ToolCategory): String = when (category) {
    ToolCategory.TIMING -> "计时"
    ToolCategory.BEHAVIOR -> "行为"
    ToolCategory.ACTIVITY -> "活动"
    ToolCategory.TAG -> "标签"
    ToolCategory.CATEGORY -> "分类"
    ToolCategory.DATA -> "数据"
    ToolCategory.SEARCH -> "搜索"
}
