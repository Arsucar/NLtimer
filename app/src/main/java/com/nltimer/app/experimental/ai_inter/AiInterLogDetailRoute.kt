package com.nltimer.app.experimental.ai_inter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.app.experimental.ai_inter.viewmodel.AiInterViewModel
import com.nltimer.app.experimental.ai_inter.viewmodel.ToolCallRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val detailJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLogDetailRoute(
    logId: Long,
    onBackClick: () -> Unit,
    viewModel: AiInterViewModel = hiltViewModel()
) {
    val log by viewModel.getLogById(logId).collectAsStateWithLifecycle(initialValue = null)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调用日志详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        val currentLog = log
        if (currentLog == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val toolCalls = remember(currentLog.toolCallsJson) {
            parseToolCallRecords(currentLog.toolCallsJson)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailField("时间戳", dateFormat.format(Date(currentLog.timestamp)))
            if (currentLog.requestUrl.isNotBlank()) {
                DetailField("请求 URL", currentLog.requestUrl)
            }
            DetailField("类型", currentLog.type)
            StatusField("状态", currentLog.status)
            DetailField("耗时", "${currentLog.durationMs} ms")
            DetailField("模型", currentLog.model)
            if (currentLog.tools.isNotBlank()) {
                DetailField("工具", currentLog.tools)
            }
            DetailField("请求 Token", if (currentLog.requestTokens > 0) currentLog.requestTokens.toString() else "-")
            DetailField("响应 Token", if (currentLog.responseTokens > 0) currentLog.responseTokens.toString() else "-")

            HorizontalDivider()

            SectionHeader("提示词")
            CodeBlock(currentLog.prompt.ifEmpty { "(空)" })

            if (currentLog.reasoning.isNotBlank()) {
                CollapsibleSection(
                    title = "思考过程",
                    icon = Icons.Default.Psychology,
                    defaultExpanded = false,
                ) {
                    CodeBlock(currentLog.reasoning)
                }
            }

            if (toolCalls.isNotEmpty()) {
                CollapsibleSection(
                    title = "调用工具 (${toolCalls.size})",
                    icon = Icons.Default.Build,
                    defaultExpanded = true,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        toolCalls.forEach { call ->
                            ToolCallDetailCard(call)
                        }
                    }
                }
            }

            SectionHeader("响应体")
            CodeBlock(
                currentLog.response.ifEmpty {
                    if (currentLog.status == "Failed") "(请求失败)" else "(空)"
                }
            )

            currentLog.errorMessage?.let { error ->
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
private fun CollapsibleSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    defaultExpanded: Boolean,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) content()
    }
}

@Composable
private fun ToolCallDetailCard(call: ToolCallRecord) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (call.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (call.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = call.name.ifEmpty { "(未知工具)" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${call.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (call.id.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "id: ${call.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "参数",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = call.arguments.ifEmpty { "(无)" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "返回结果",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = call.result.ifEmpty { "(空)" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun parseToolCallRecords(jsonStr: String): List<ToolCallRecord> {
    if (jsonStr.isBlank()) return emptyList()
    return try {
        val element = detailJson.parseToJsonElement(jsonStr)
        val arr = element as? JsonArray ?: return emptyList()
        arr.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            ToolCallRecord(
                id = (obj["id"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                name = (obj["name"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                arguments = (obj["arguments"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                result = (obj["result"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                success = (obj["success"] as? JsonPrimitive)?.booleanOrNull ?: false,
                durationMs = (obj["durationMs"] as? JsonPrimitive)?.longOrNull ?: 0L,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
