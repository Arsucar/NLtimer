package com.nltimer.feature.settings.ui.eventtemplate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.usecase.CreateOrUpdateTemplateUseCase
import com.nltimer.core.designsystem.component.ConfirmDialog

@Composable
fun EventTemplateEditRoute(
    templateId: Long?,
    _onNavigateBack: () -> Unit = {},
    viewModel: EventTemplateViewModel = hiltViewModel(),
) {
    LaunchedEffect(templateId) { viewModel.prepareEdit(templateId) }
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    EventTemplateEditScreen(
        editState = editState,
        onUpdateName = viewModel::updateEditName,
        onAddField = viewModel::addField,
        onRemoveField = viewModel::removeField,
        onMoveField = viewModel::moveField,
        onUpdateField = viewModel::updateFieldDraft,
        onToggleExpand = viewModel::toggleFieldExpand,
        onSave = {
            viewModel.saveTemplate { result ->
                if (result is CreateOrUpdateTemplateUseCase.Result.Success) _onNavigateBack()
            }
        },
        onDelete = { showDeleteConfirm = true },
    )

    if (showDeleteConfirm && editState.editingId != null) {
        ConfirmDialog(
            title = "删除模板",
            message = "确定要删除「${editState.name.ifBlank { "编辑中的模板" }}」？" +
                "将同时删除该模板下已有事件记录及其字段值，此操作不可撤销。",
            confirmText = "删除",
            confirmTextColor = MaterialTheme.colorScheme.error,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                editState.editingId?.let { viewModel.deleteTemplate(it, onDeleted = _onNavigateBack) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventTemplateEditScreen(
    editState: EventTemplateEditState,
    onUpdateName: (String) -> Unit,
    onAddField: () -> Unit,
    onRemoveField: (Int) -> Unit,
    onMoveField: (Int, Boolean) -> Unit,
    onUpdateField: (Int, (EventTemplateFieldDraft) -> EventTemplateFieldDraft) -> Unit,
    onToggleExpand: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = editState.name,
                onValueChange = onUpdateName,
                label = { Text("模板名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "字段（编号 / 名称 / 类型 / 选项 / 排序）",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAddField) {
                    Icon(Icons.Default.Add, contentDescription = "添加字段", modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("添加")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                editState.fields.forEach { draft ->
                    FieldDraftCard(
                        draft = draft,
                        canMove = editState.fields.size > 1,
                        onToggleExpand = { onToggleExpand(draft.uid) },
                        onRemove = { onRemoveField(draft.uid) },
                        onMoveUp = { onMoveField(draft.uid, true) },
                        onMoveDown = { onMoveField(draft.uid, false) },
                        onNameChange = { value -> onUpdateField(draft.uid) { it.copy(name = value) } },
                        onTypeChange = { type ->
                            onUpdateField(draft.uid) {
                                it.copy(
                                    type = type,
                                    optionsText = if (type == EventFieldType.SELECT) it.optionsText else "",
                                    isExpanded = true,
                                )
                            }
                        },
                        onOptionsTextChange = { value ->
                            onUpdateField(draft.uid) { it.copy(optionsText = value) }
                        },
                    )
                }
            }

            editState.validationError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = "提示：全部字段保持唯一命名；切换模板时字段值将按「同名同类型」尽量迁移",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            FilledTonalButton(
                onClick = onSave,
                enabled = !editState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .height(48.dp),
            ) {
                Text(if (editState.isSaving) "保存中…" else "保存模板")
            }

            if (editState.editingId != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除模板", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldDraftCard(
    draft: EventTemplateFieldDraft,
    canMove: Boolean,
    onToggleExpand: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (EventFieldType) -> Unit,
    onOptionsTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (draft.isExpanded) {
                    IconButton(onClick = onToggleExpand) {
                        Icon(Icons.Default.ArrowDropUp, contentDescription = "收起")
                    }
                } else {
                    IconButton(onClick = onToggleExpand) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "展开")
                    }
                }
                Text(
                    text = draft.name.ifBlank { "未命名字段" } + " · " + typeLabel(draft.type),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (canMove) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除字段",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (draft.isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = onNameChange,
                        label = { Text("字段名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EventFieldType.entries.forEach { type ->
                            FilterChip(
                                selected = draft.type == type,
                                onClick = { onTypeChange(type) },
                                label = { Text(typeLabel(type)) },
                            )
                        }
                    }
                    if (draft.type == EventFieldType.SELECT) {
                        OutlinedTextField(
                            value = draft.optionsText,
                            onValueChange = onOptionsTextChange,
                            label = { Text("单选选项（用逗号分隔）") },
                            placeholder = { Text("例如：优，良，中，差") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    when (draft.type) {
                        EventFieldType.RATING -> Text(
                            text = "打点时以 1~5 星评分，计入数值列",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        EventFieldType.NUMBER -> Text(
                            text = "打点时输入数值",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        EventFieldType.TEXT -> Text(
                            text = "打点时输入长文本",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        EventFieldType.SELECT -> Text(
                            text = "打点时从选项中单选",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun typeLabel(type: EventFieldType): String = when (type) {
    EventFieldType.SELECT -> "单选"
    EventFieldType.TEXT -> "文本"
    EventFieldType.NUMBER -> "数值"
    EventFieldType.RATING -> "星级"
}
