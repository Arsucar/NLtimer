package com.nltimer.app.experimental.ai_inter.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.nltimer.app.navigation.NLtimerRoutes
import com.nltimer.app.experimental.ai_inter.chat.components.ChatDrawer
import com.nltimer.app.experimental.ai_inter.chat.components.ChatInput
import com.nltimer.app.experimental.ai_inter.chat.components.ChatList
import com.nltimer.app.experimental.ai_inter.chat.components.ChatTopBar
import com.nltimer.app.experimental.ai_inter.chat.components.ExportSheet
import com.nltimer.app.experimental.ai_inter.chat.export.ExportFormat
import com.nltimer.app.experimental.ai_inter.chat.export.ExportOptions
import com.nltimer.core.designsystem.theme.LocalTheme
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatRoute(
    navController: NavHostController,
    mainDrawerState: DrawerState? = null,
    viewModel: AiAssistantChatViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    val chatHistoryDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val streaming by viewModel.streamingState.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()

    val current = conversations.firstOrNull { it.id == currentId }
    val listState = rememberLazyListState()
    val hazeState = rememberHazeState()
    var inputText by remember { mutableStateOf("") }
    var showExport by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isImmersive = LocalTheme.current.isImmersive
    val topBarHaze = LocalTheme.current.topBarHaze

    BackHandler(enabled = chatHistoryDrawerState.isOpen) {
        scope.launch { chatHistoryDrawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = chatHistoryDrawerState,
        gesturesEnabled = false,
        drawerContent = {
            ChatDrawer(
                conversations = conversations,
                currentId = currentId,
                onSelect = {
                    viewModel.selectConversation(it)
                    scope.launch { chatHistoryDrawerState.close() }
                },
                onCreate = {
                    viewModel.newConversation()
                    scope.launch { chatHistoryDrawerState.close() }
                },
                onRename = viewModel::renameConversation,
                onDelete = viewModel::deleteConversation,
            )
        },
    ) {
        Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                            ),
                        ),
                    )
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                topBar = {
                    ChatTopBar(
                        title = current?.title ?: "新对话",
                        modelName = config.modelName,
                        onOpenChatHistory = if (mainDrawerState != null) {
                            { scope.launch { chatHistoryDrawerState.open() } }
                        } else null,
                        onTitleClick = { if (current != null) showRename = true },
                        onNewConversation = { viewModel.newConversation() },
                        onClearCurrent = { viewModel.clearCurrent() },
                        onExport = { showExport = true },
                        onNavigateToAiInter = { navController.navigate(NLtimerRoutes.AI_INTER) },
                        scrollBehavior = topBarScrollBehavior,
                        isImmersive = isImmersive,
                        hazeState = if (topBarHaze) hazeState else null,
                    )
                },
                bottomBar = {
                    ChatInput(
                        text = inputText,
                        isSending = isSending,
                        onTextChange = { inputText = it },
                        onSend = {
                            val msg = inputText.trim()
                            if (msg.isNotBlank()) {
                                viewModel.sendMessage(msg)
                                inputText = ""
                            }
                        },
                        onStop = { viewModel.stopStreaming() },
                        onPickModel = {
                            viewModel.refreshModels()
                            showModelSheet = true
                        },
                        hazeState = hazeState,
                    )
                },
            ) { innerPadding ->
                ChatList(
                    innerPadding = innerPadding,
                    messages = messages,
                    streaming = streaming,
                    isSending = isSending,
                    chatError = chatError,
                    listState = listState,
                    hazeState = hazeState,
                    onCopy = { msg ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("AI 消息", msg.content))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = { viewModel.regenerateLastAssistant() },
                    onDelete = { msg -> viewModel.deleteMessage(msg.id) },
                    onDismissError = { viewModel.clearChatError() },
                )
            }
    }

    if (showRename && current != null) {
        var input by remember(current.id) { mutableStateOf(current.title) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRename = false },
            title = { androidx.compose.material3.Text("重命名对话") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.renameConversation(current.id, input)
                    showRename = false
                }) { androidx.compose.material3.Text("保存") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRename = false }) { androidx.compose.material3.Text("取消") }
            },
        )
    }

    if (showModelSheet) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showModelSheet = false }) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                androidx.compose.material3.Text("选择模型", style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text("当前：${config.modelName}", style = MaterialTheme.typography.labelSmall)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                availableModels.forEach { name ->
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.selectModel(name)
                            showModelSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { androidx.compose.material3.Text(name) }
                }
                if (availableModels.isEmpty()) {
                    androidx.compose.material3.Text(
                        "未拉取到模型列表，请先在 AI Inter → 提供商配置 中获取",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showExport && current != null) {
        ExportSheet(
            onDismiss = { showExport = false },
            onExport = { fmt: ExportFormat, opts: ExportOptions -> viewModel.exportConversation(current.id, fmt, opts) },
        )
    }
}
