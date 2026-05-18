package com.nltimer.app.experimental.ai_inter.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    modelName: String,
    onOpenDrawer: () -> Unit,
    onTitleClick: () -> Unit,
    onNewConversation: () -> Unit,
    onClearCurrent: () -> Unit,
    onExport: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isImmersive: Boolean = false,
    hazeState: HazeState,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = Modifier.hazeEffect(
            state = hazeState,
            style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.surfaceContainerLow),
        ),
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "会话列表") }
        },
        title = {
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
        },
        actions = {
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
        },
    )
}
