package com.nltimer.feature.debug.ui.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nltimer.feature.debug.data.DebugDatabaseHelper
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * 数据库工具调试预览入口
 * 提供清除/插入/查询数据库数据的调试操作界面，
 * 支持全局操作和按表粒度的单独操作
 */
@Composable
fun DatabaseToolsPreview() {
    val helper = getDebugDatabaseHelper()
    val scope = rememberCoroutineScope()

    // 各表记录数状态
    var activityCount by remember { mutableStateOf(0) }
    var groupCount by remember { mutableStateOf(0) }
    var tagCount by remember { mutableStateOf(0) }
    var behaviorCount by remember { mutableStateOf(0) }
    var bindingCount by remember { mutableStateOf(0) }
    var crossRefCount by remember { mutableStateOf(0) }

    // 查询结果状态
    var queryResult by remember { mutableStateOf<Map<String, List<String>>?>(null) }
    var expandedTables by remember { mutableStateOf(setOf<String>()) }

    // 确认对话框状态
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmMessage by remember { mutableStateOf("") }

    // 操作反馈
    var snackbarMessage by remember { mutableStateOf("") }

    // 刷新各表记录数
    suspend fun refreshCounts() {
        val data = helper.queryAllData()
        activityCount = data["activities"]?.size ?: 0
        groupCount = data["activity_groups"]?.size ?: 0
        tagCount = data["tags"]?.size ?: 0
        behaviorCount = data["behaviors"]?.size ?: 0
        bindingCount = data["activity_tag_binding"]?.size ?: 0
        crossRefCount = data["behavior_tag_cross_ref"]?.size ?: 0
        if (queryResult != null) {
            queryResult = data
        }
    }

    // 初始加载记录数
    LaunchedEffect(Unit) {
        refreshCounts()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 全局操作区域
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "全局操作",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 全部清除按钮
                    OutlinedButton(
                        onClick = {
                            confirmMessage = "确定要清除所有数据库数据吗？此操作不可撤销。"
                            confirmAction = {
                                scope.launch {
                                    helper.clearAllTables()
                                    refreshCounts()
                                    snackbarMessage = "已清除所有数据"
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("全部清除")
                    }
                    // 批量插入全部按钮
                    Button(
                        onClick = {
                            scope.launch {
                                helper.insertAllTestData()
                                refreshCounts()
                                snackbarMessage = "已插入全部测试数据"
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("批量插入全部")
                    }
                }
            }
        }

        // 每表操作卡片
        val tableCards = listOf(
            Triple("activities", "活动", activityCount),
            Triple("activity_groups", "活动分组", groupCount),
            Triple("tags", "标签", tagCount),
            Triple("behaviors", "行为记录", behaviorCount),
            Triple("activity_tag_binding", "活动-标签绑定", bindingCount),
            Triple("behavior_tag_cross_ref", "行为-标签关联", crossRefCount),
        )

        tableCards.forEach { (tableName, displayName, count) ->
            TableOperationCard(
                _tableName = tableName,
                displayName = displayName,
                recordCount = count,
                onInsert = {
                    scope.launch {
                        helper.insertTestData(tableName)
                        refreshCounts()
                        snackbarMessage = "已为 $displayName 插入测试数据"
                    }
                },
                onClear = {
                    confirmMessage = "确定要清除 $displayName 的所有数据吗？"
                    confirmAction = {
                        scope.launch {
                            helper.clearTable(tableName)
                            refreshCounts()
                            snackbarMessage = "已清除 $displayName 数据"
                        }
                    }
                },
            )
        }

        // 查询全部数据按钮
        Button(
            onClick = {
                scope.launch {
                    queryResult = helper.queryAllData()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("查询全部数据")
        }

        // 查询结果展示
        queryResult?.let { data ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "查询结果",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )

            data.forEach { (tableName, rows) ->
                val displayName = when (tableName) {
                    "activities" -> "活动"
                    "activity_groups" -> "活动分组"
                    "tags" -> "标签"
                    "behaviors" -> "行为记录"
                    "activity_tag_binding" -> "活动-标签绑定"
                    "behavior_tag_cross_ref" -> "行为-标签关联"
                    else -> tableName
                }
                val isExpanded = tableName in expandedTables

                Surface(
                    onClick = {
                        expandedTables = if (isExpanded) expandedTables - tableName
                        else expandedTables + tableName
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 卡片标题行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$displayName (${rows.size}条)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            )
                            Text(
                                text = if (isExpanded) "▼" else "▶",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        // 展开后的详细数据
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (rows.isEmpty()) {
                                    Text(
                                        text = "（空表）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    rows.forEach { rowStr ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                        ) {
                                            Text(
                                                text = rowStr,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                ),
                                                modifier = Modifier.padding(6.dp),
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

        // Snackbar 反馈
        if (snackbarMessage.isNotEmpty()) {
            LaunchedEffect(snackbarMessage) {
                kotlinx.coroutines.delay(2000)
                snackbarMessage = ""
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Text(
                    text = snackbarMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    // 确认对话框
    confirmAction?.let {
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("确认操作") },
            text = { Text(confirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        it.invoke()
                        confirmAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmAction = null }) {
                    Text("取消")
                }
            },
        )
    }
}

/**
 * 单表操作卡片
 * 展示表名、记录数，提供插入测试数据和清除数据按钮
 *
 * @param tableName 数据库表名
 * @param displayName 中文显示名
 * @param recordCount 当前记录数
 * @param onInsert 插入测试数据回调
 * @param onClear 清除数据回调
 * @param modifier 可选的修饰符
 */
@Composable
private fun TableOperationCard(
    _tableName: String,
    displayName: String,
    recordCount: Int,
    onInsert: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：表名 + 记录数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = "(${recordCount}条)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onInsert,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("插入测试数据")
                }
                OutlinedButton(
                    onClick = onClear,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("清除数据")
                }
            }
        }
    }
}

/**
 * Hilt EntryPoint 接口，用于在非 Hilt 管理的 Composable 中获取 DebugDatabaseHelper
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugDatabaseHelperEntryPoint {
    fun debugDatabaseHelper(): DebugDatabaseHelper
}

/**
 * 通过 Hilt EntryPoint 获取 DebugDatabaseHelper 实例
 */
@Composable
private fun getDebugDatabaseHelper(): DebugDatabaseHelper {
    val context = LocalContext.current
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        DebugDatabaseHelperEntryPoint::class.java
    ).debugDatabaseHelper()
}
