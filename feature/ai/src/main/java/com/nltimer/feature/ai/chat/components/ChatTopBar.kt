package com.nltimer.feature.ai.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    modelName: String,
    onOpenChatHistory: (() -> Unit)? = null,
    onTitleClick: () -> Unit,
    onNewConversation: () -> Unit,
    onClearCurrent: () -> Unit,
    onExport: () -> Unit,
    onNavigateToAiInter: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isImmersive: Boolean = false,
    hazeState: HazeState? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        navigationIcon = {
            if (onOpenChatHistory != null) {
                IconButton(onClick = onOpenChatHistory) { Icon(Icons.AutoMirrored.Filled.Chat, "会话历史") }
            }
        },
        title = {
            val titleContent = @Composable {
                Surface(onClick = onTitleClick, color = Color.Transparent) {
                    Column {
                        Text(
                            text = title.ifBlank { "新对话" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (modelName.isNotBlank()) {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = LocalContentColor.current.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (hazeState != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    titleContent()
                }
            } else {
                titleContent()
            }
        },
        actions = {
            val actionsContent = @Composable {
                IconButton(onClick = onNavigateToAiInter) { Icon(Icons.Default.AutoAwesome, "AI Inter") }
                IconButton(onClick = onExport) { Icon(Icons.Default.IosShare, "导出") }
                IconButton(onClick = onNewConversation) { Icon(Icons.Default.Add, "新建对话") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "更多") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("清空当前对话") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; onClearCurrent() },
                        )
                    }
                }
            }
            if (hazeState != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow),
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actionsContent()
                }
            } else {
                actionsContent()
            }
        },
    )
}
