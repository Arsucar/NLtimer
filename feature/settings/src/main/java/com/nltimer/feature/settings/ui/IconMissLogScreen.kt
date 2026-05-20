package com.nltimer.feature.settings.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.database.entity.IconSearchMissEntity
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconMissLogRoute(
    onNavigateBack: () -> Unit,
    viewModel: IconMissLogViewModel = hiltViewModel(),
) {
    val misses by viewModel.misses.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清除") },
            text = { Text("将清除所有 ${misses.size} 条图标搜索未命中记录，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图标库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (misses.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = formatMissesForCopy(misses)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("图标未命中日志", text))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制日志")
                        }
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清除日志")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (misses.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            MissList(
                misses = misses,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "暂无图标需求记录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MissList(
    misses: List<IconSearchMissEntity>,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "用户搜索图标但未找到的记录（共 ${misses.size} 条）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(misses, key = { it.id }) { miss ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = miss.query,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${miss.library} · ${dateFormat.format(miss.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "⁉",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }
}

private fun formatMissesForCopy(misses: List<IconSearchMissEntity>): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val sb = StringBuilder()
    sb.appendLine("NLtimer 图标需求日志")
    sb.appendLine("导出时间: ${dateFormat.format(System.currentTimeMillis())}")
    sb.appendLine("共 ${misses.size} 条记录")
    sb.appendLine("---")
    for (m in misses) {
        sb.appendLine("${dateFormat.format(m.timestamp)} | ${m.library} | ${m.query}")
    }
    return sb.toString()
}
