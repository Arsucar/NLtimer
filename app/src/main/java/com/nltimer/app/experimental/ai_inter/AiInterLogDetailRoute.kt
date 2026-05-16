package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.app.experimental.ai_inter.viewmodel.AiInterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLogDetailRoute(
    onBackClick: () -> Unit,
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()

    if (selectedLog == null) {
        onBackClick()
        return
    }

    val log = selectedLog!!
    val dateFormat = androidx.compose.runtime.remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调用日志详情") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedLog()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailField("时间戳", dateFormat.format(Date(log.timestamp)))
            if (log.requestUrl.isNotBlank()) {
                DetailField("请求 URL", log.requestUrl)
            }
            DetailField("类型", log.type)
            StatusField("状态", log.status)
            DetailField("耗时", "${log.durationMs} ms")
            DetailField("模型", log.model)
            if (log.tools.isNotBlank()) {
                DetailField("工具", log.tools)
            }
            DetailField("请求 Token", if (log.requestTokens > 0) log.requestTokens.toString() else "-")
            DetailField("响应 Token", if (log.responseTokens > 0) log.responseTokens.toString() else "-")

            HorizontalDivider()

            SectionHeader("提示词")
            CodeBlock(log.prompt.ifEmpty { "(空)" })

            SectionHeader("响应体")
            CodeBlock(
                log.response.ifEmpty {
                    if (log.status == "Failed") "(请求失败)" else "(空)"
                }
            )

            log.errorMessage?.let { error ->
                SectionHeader("错误信息")
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusField(label: String, status: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val color = if (status == "Success") MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CodeBlock(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun remember(function: () -> SimpleDateFormat): SimpleDateFormat {
    return androidx.compose.runtime.remember { function() }
}
