package com.nltimer.feature.settings.ui.eventtemplate

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.designsystem.component.ConfirmDialog
import com.nltimer.core.designsystem.component.EmptyStateView
import com.nltimer.feature.settings.ui.SettingsSubpageContainer

@Composable
fun EventTemplateListRoute(
    onOpenEditor: (templateId: Long?) -> Unit,
    _onNavigateBack: () -> Unit = {},
    viewModel: EventTemplateViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val fieldCounts by viewModel.fieldCounts.collectAsStateWithLifecycle()

    EventTemplateListScreen(
        templates = templates,
        fieldCounts = fieldCounts,
        onOpenEditor = onOpenEditor,
        onDeleteTemplate = { viewModel.deleteTemplate(it) },
    )
}

@Composable
internal fun EventTemplateListScreen(
    templates: List<EventTemplate>,
    fieldCounts: Map<Long, Int>,
    onOpenEditor: (templateId: Long?) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteTarget by remember { mutableStateOf<EventTemplate?>(null) }

    if (deleteTarget != null) {
        ConfirmDialog(
            title = "删除模板",
            message = "确定要删除「${deleteTarget!!.name}」？" +
                "将同时删除该模板下已有事件记录及其字段值，此操作不可撤销。",
            confirmText = "删除",
            confirmTextColor = MaterialTheme.colorScheme.error,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteTemplate(deleteTarget!!.id)
                deleteTarget = null
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (templates.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyStateView(
                    message = "还没有打点模板",
                    subtitle = "新建一个模板，为专注行为快速记录结构化事件",
                    modifier = Modifier.height(180.dp),
                )
                NewTemplateButton(
                    onClick = { onOpenEditor(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            SettingsSubpageContainer(
                content = {
                    item {
                        Text(
                            text = "共 ${templates.size} 个模板 · 打点时按标签自动套用",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(count = templates.size, key = { templates[it].id }) { index ->
                        val template = templates[index]
                        TemplateItem(
                            template = template,
                            fieldCount = fieldCounts[template.id] ?: 0,
                            onClick = { onOpenEditor(template.id) },
                            onDelete = { deleteTarget = template },
                        )
                    }
                    item {
                        NewTemplateButton(
                            onClick = { onOpenEditor(null) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun TemplateItem(
    template: EventTemplate,
    fieldCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$fieldCount 个字段",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "删除模板",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun NewTemplateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .height(48.dp),
    ) {
        Text("新建模板")
    }
}
