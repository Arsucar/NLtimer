package com.nltimer.feature.home.ui.components.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.feature.home.model.EventSheetTarget
import java.time.Instant
import java.time.ZoneId

/** 草稿 key：字段名 + 类型（跨模板切换时按「同名同类型」迁移草稿值） */
private fun EventTemplateField.draftKey(): String = "$name|${type.key}"

private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

internal fun eventFieldTypeLabel(type: EventFieldType): String = when (type) {
    EventFieldType.SELECT -> "单选"
    EventFieldType.TEXT -> "文本"
    EventFieldType.NUMBER -> "数值"
    EventFieldType.RATING -> "星级"
}

/**
 * 事件表单页：归属行 + 模板行 + 字段（快速模式=单选/星级/数值；展开=含长文本）+ 保存
 * 草稿值统一以文本形式暂存（draftKey），保存时按字段类型转换回 BehaviorEventValue
 */
@Composable
internal fun EventFormPage(
    target: EventSheetTarget,
    templates: List<EventTemplate>,
    onQueryFields: suspend (Long) -> List<EventTemplateField>,
    onQueryEvent: suspend (Long) -> BehaviorEventWithValues?,
    onSubmit: (templateId: Long, attachToBehavior: Boolean, values: List<BehaviorEventValue>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var templateId by remember(target) { mutableStateOf<Long?>(target.initialTemplateId) }
    var fields by remember { mutableStateOf<List<EventTemplateField>>(emptyList()) }
    var drafts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var expanded by remember { mutableStateOf(false) }
    var attachToBehavior by remember(target) { mutableStateOf(target.behaviorId != null) }
    var pendingTemplatePick by remember { mutableStateOf<Long?>(null) }
    var showTemplateMenu by remember { mutableStateOf(false) }

    // 打开时：编辑态回填（事件详情 + 字段 + 草稿按 fieldId 回填）；新增态按初始模板装载
    LaunchedEffect(target) {
        val editId = target.editEventId
        if (editId != null) {
            val detail = onQueryEvent(editId)
            if (detail != null) {
                val loadedFields = onQueryFields(detail.event.templateId)
                templateId = detail.event.templateId
                fields = loadedFields
                drafts = loadedFields.associate { field ->
                    val value = detail.values.firstOrNull { it.fieldId == field.id }
                    field.draftKey() to when {
                        value?.valueText != null -> value.valueText!!
                        value?.valueNumber != null -> trimNumber(value.valueNumber!!)
                        else -> ""
                    }
                }
                return@LaunchedEffect
            }
        }
        val initial = target.initialTemplateId
        templateId = initial
        if (initial == null) {
            fields = emptyList()
            drafts = emptyMap()
        } else {
            val loadedFields = onQueryFields(initial)
            fields = loadedFields
            drafts = loadedFields.associate { it.draftKey() to "" }
        }
    }

    // 手动切换模板：同名同类型草稿沿用，其余清空（draftKey 天然完成映射）
    LaunchedEffect(pendingTemplatePick) {
        val picked = pendingTemplatePick ?: return@LaunchedEffect
        pendingTemplatePick = null
        if (picked == templateId) return@LaunchedEffect
        val oldDrafts = drafts
        val loadedFields = onQueryFields(picked)
        templateId = picked
        fields = loadedFields
        drafts = loadedFields.associate { field -> field.draftKey() to (oldDrafts[field.draftKey()] ?: "") }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .weight(1f, fill = false),
        ) {
            Spacer(Modifier.height(8.dp))

            // 归属行：目标行为 vs 独立事件切换（无 behaviorId 时固定为独立事件）
            if (target.behaviorId != null) {
                Text(
                    text = "归属：${target.activityName ?: "该行为"} · 开始 ${formatStart(target.startEpochMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventScopeChip(
                        label = "挂当前行为",
                        selected = attachToBehavior,
                        onClick = { attachToBehavior = true },
                    )
                    EventScopeChip(
                        label = "独立事件",
                        selected = !attachToBehavior,
                        onClick = { attachToBehavior = false },
                    )
                }
            } else {
                Text(
                    text = "独立事件 · 现在打点（不挂当前行为）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 模板行
            val templateName = templates.firstOrNull { it.id == templateId }?.name
            Box {
                TextButton(onClick = { showTemplateMenu = true }) {
                    Text(
                        text = templateName ?: "选择打点模板",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "切换模板")
                }
                DropdownMenu(
                    expanded = showTemplateMenu,
                    onDismissRequest = { showTemplateMenu = false },
                ) {
                    templates.forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template.name) },
                            trailingIcon = if (template.id == templateId) {
                                { Icon(Icons.Default.Check, contentDescription = "当前模板") }
                            } else {
                                null
                            },
                            onClick = {
                                showTemplateMenu = false
                                pendingTemplatePick = template.id
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (fields.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "还没有可用的打点模板\n请在 设置 → 打点模板 创建一个，再回来体验结构化打点",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) { Text("知道了") }
            } else {
                val visibleFields = if (expanded) fields else fields.filter { it.type != EventFieldType.TEXT }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visibleFields.forEach { field ->
                        EventFieldEditor(
                            field = field,
                            value = drafts[field.draftKey()].orEmpty(),
                            onValueChange = { raw -> drafts = drafts + (field.draftKey() to raw) },
                        )
                    }
                }

                if (fields.any { it.type == EventFieldType.TEXT }) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "收起完整表单" else "展开完整表单（含文本字段）")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // 底部保存条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text("取消") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    templateId?.let { id ->
                        onSubmit(id, attachToBehavior, collectValues(fields, drafts))
                    }
                },
                enabled = templateId != null,
            ) {
                Text(if (target.editEventId != null) "保存修改" else "记一笔")
            }
        }
    }
}

/** 草稿 → 事件字段值（空值跳过；星级 0 视为未填） */
private fun collectValues(
    fields: List<EventTemplateField>,
    drafts: Map<String, String>,
): List<BehaviorEventValue> = fields.mapNotNull { field ->
    val raw = drafts[field.draftKey()].orEmpty().trim()
    when (field.type) {
        EventFieldType.SELECT, EventFieldType.TEXT ->
            if (raw.isEmpty()) null else BehaviorEventValue(fieldId = field.id, valueText = raw)
        EventFieldType.NUMBER ->
            raw.toDoubleOrNull()?.let { BehaviorEventValue(fieldId = field.id, valueNumber = it) }
        EventFieldType.RATING ->
            raw.toIntOrNull()?.takeIf { it in 1..5 }
                ?.let { BehaviorEventValue(fieldId = field.id, valueNumber = it.toDouble()) }
    }
}

private fun formatStart(epochMs: Long?): String {
    if (epochMs == null) return "--:--"
    val time = Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

@Composable
private fun EventFieldEditor(
    field: EventTemplateField,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = field.name + " · " + eventFieldTypeLabel(field.type),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        when (field.type) {
            EventFieldType.SELECT -> SelectChipRow(
                options = field.options,
                selected = value,
                onSelect = { picked -> onValueChange(if (picked == value) "" else picked) },
            )
            EventFieldType.RATING -> StarRatingBar(
                rating = value.toIntOrNull() ?: 0,
                onRatingChange = { rating -> onValueChange(if (rating <= 0) "" else rating.toString()) },
            )
            EventFieldType.NUMBER -> OutlinedTextField(
                value = value,
                onValueChange = { raw -> onValueChange(raw) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("输入数值") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            EventFieldType.TEXT -> OutlinedTextField(
                value = value,
                onValueChange = { raw -> onValueChange(raw) },
                placeholder = { Text("输入内容") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelectChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun EventScopeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
