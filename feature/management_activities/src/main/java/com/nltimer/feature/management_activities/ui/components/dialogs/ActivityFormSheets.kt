package com.nltimer.feature.management_activities.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.model.AddActivityCallback
import com.nltimer.core.behaviorui.sheet.ActivityGroupCategorizable
import com.nltimer.core.behaviorui.sheet.CategoryGroup
import com.nltimer.core.behaviorui.sheet.CategoryPickerDialog
import com.nltimer.core.behaviorui.sheet.TagCategorizable
import com.nltimer.core.designsystem.form.ActivityFormSpecs
import com.nltimer.core.designsystem.form.FormRow
import com.nltimer.core.designsystem.form.GenericFormSheet
import com.nltimer.core.designsystem.component.tagCountLabel
import com.nltimer.core.designsystem.form.parseColorHex

@Composable
fun AddActivityFormSheet(
    allGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    initialGroupId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: AddActivityCallback,
) {
    var selectedGroupId by remember(initialGroupId) { mutableStateOf(initialGroupId) }
    var selectedTagIds by remember { mutableStateOf(emptySet<Long>()) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    val groupName = allGroups.find { it.id == selectedGroupId }?.name ?: "未分类"
    val tagCountText = tagCountLabel(selectedTagIds.size)

    // DIFF: 复杂多字段变更，无法用 withUpdatedLabelAction 简化
    val specWithCategory = ActivityFormSpecs.createActivity.copy(
        sections = ActivityFormSpecs.createActivity.sections.map { section ->
            section.copy(
                rows = section.rows.map { row ->
                    when {
                        row is FormRow.LabelAction && row.key == "category" -> row.copy(
                            actionText = groupName,
                            onClick = { showGroupPicker = true },
                        )
                        row is FormRow.LabelAction && row.key == "tags" -> row.copy(
                            actionText = tagCountText,
                            onClick = { showTagPicker = true },
                        )
                        else -> row
                    }
                },
            )
        },
    )

    GenericFormSheet(
        spec = specWithCategory,
        initialData = null,
        onDismiss = onDismiss,
        onSubmit = { formState ->
            val name = formState["name"]?.trim() ?: ""
            val iconKey = formState["icon"]?.trim()?.ifBlank { null }
            val colorHex = formState["color"]?.trim()?.ifBlank { null }
            val keywords = formState["keywords"]?.trim()?.ifBlank { null }
            val color = parseColorHex(colorHex)
            onConfirm(name, iconKey, color, selectedGroupId, keywords, selectedTagIds.toList())
        },
        overlay = {
            if (showGroupPicker) {
                val groupItems = remember(allGroups) {
                    allGroups.map { ActivityGroupCategorizable(it) }
                }
                val groupedGroups = remember(groupItems) {
                    listOf(
                        CategoryGroup(
                            id = 0L,
                            name = "所有分组",
                            items = groupItems,
                            onClear = {
                                selectedGroupId = null
                                showGroupPicker = false
                            },
                            clearLabel = "清除",
                        )
                    )
                }
                CategoryPickerDialog(
                    title = "选择所属分组",
                    items = groupItems,
                    categoryGroups = groupedGroups,
                    selectedId = selectedGroupId ?: 0L,
                    onItemSelected = { id ->
                        selectedGroupId = id
                        showGroupPicker = false
                    },
                    onDismiss = { showGroupPicker = false },
                    showHeader = false,
                )
            }
            if (showTagPicker) {
                val categorizableTags = remember(allTags) {
                    allTags.map { TagCategorizable(it) }
                }
                val groupedTags = remember(allTags) {
                    allTags.groupBy { it.category ?: "未分类" }
                        .map { (category, items) ->
                            CategoryGroup(
                                id = category.hashCode().toLong(),
                                name = category,
                                items = items.map { TagCategorizable(it) }
                            )
                        }
                        .sortedBy { if (it.name == "未分类") "" else it.name }
                }
                CategoryPickerDialog(
                    title = "关联标签",
                    items = categorizableTags,
                    categoryGroups = groupedTags,
                    selectedIds = selectedTagIds,
                    multiSelect = true,
                    onItemsSelected = { selectedTagIds = it },
                    onDismiss = { showTagPicker = false },
                )
            }
        },
    )
}

@Composable
fun EditActivityFormSheet(
    activity: Activity,
    allGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    initialTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Activity, List<Long>) -> Unit,
    onDelete: () -> Unit = {},
) {
    var selectedGroupId by remember(activity.id) { mutableStateOf(activity.groupId) }
    var selectedTagIds by remember { mutableStateOf(initialTagIds) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    val groupName = allGroups.find { it.id == selectedGroupId }?.name ?: "未分类"
    val tagCountText = tagCountLabel(selectedTagIds.size)

    // DIFF: 复杂多字段变更，无法用 withUpdatedLabelAction 简化
    val specWithCategory = ActivityFormSpecs.editActivity().copy(
        sections = ActivityFormSpecs.editActivity().sections.map { section ->
            section.copy(
                rows = section.rows.map { row ->
                    when {
                        row is FormRow.LabelAction && row.key == "category" -> row.copy(
                            actionText = groupName,
                            onClick = { showGroupPicker = true },
                        )
                        row is FormRow.LabelAction && row.key == "tags" -> row.copy(
                            actionText = tagCountText,
                            onClick = { showTagPicker = true },
                        )
                        else -> row
                    }
                },
            )
        },
    )

    val initialData = mapOf(
        "icon" to (activity.iconKey ?: "📖"),
        "color" to (activity.color?.let { (it and 0xFFFFFFFF).toString(16) } ?: ""),
        "name" to activity.name,
        "keywords" to (activity.keywords ?: ""),
        "isArchived" to activity.isArchived.toString(),
    )

    GenericFormSheet(
        spec = specWithCategory,
        initialData = initialData,
        onDismiss = onDismiss,
        onSubmit = { formState ->
            val name = formState["name"]?.trim() ?: activity.name
            val iconKey = formState["icon"]?.trim()?.ifBlank { null }
            val colorHex = formState["color"]?.trim()?.ifBlank { null }
            val keywords = formState["keywords"]?.trim()?.ifBlank { null }
            val isArchived = formState["isArchived"]?.toBooleanStrictOrNull() ?: activity.isArchived
            val colorLong = parseColorHex(colorHex)
            onConfirm(
                activity.copy(
                    name = name,
                    iconKey = iconKey,
                    groupId = selectedGroupId,
                    isArchived = isArchived,
                    color = colorLong ?: activity.color,
                    keywords = keywords,
                ),
                selectedTagIds.toList(),
            )
        },
        overlay = {
            if (showGroupPicker) {
                val groupItems = remember(allGroups) {
                    allGroups.map { ActivityGroupCategorizable(it) }
                }
                val groupedGroups = remember(groupItems) {
                    listOf(
                        CategoryGroup(
                            id = 0L,
                            name = "所有分组",
                            items = groupItems,
                            onClear = {
                                selectedGroupId = null
                                showGroupPicker = false
                            },
                            clearLabel = "清除",
                        )
                    )
                }
                CategoryPickerDialog(
                    title = "选择所属分组",
                    items = groupItems,
                    categoryGroups = groupedGroups,
                    selectedId = selectedGroupId ?: 0L,
                    onItemSelected = { id ->
                        selectedGroupId = id
                        showGroupPicker = false
                    },
                    onDismiss = { showGroupPicker = false },
                    showHeader = false,
                )
            }
            if (showTagPicker) {
                val categorizableTags = remember(allTags) {
                    allTags.map { TagCategorizable(it) }
                }
                val groupedTags = remember(allTags) {
                    allTags.groupBy { it.category ?: "未分类" }
                        .map { (category, items) ->
                            CategoryGroup(
                                id = category.hashCode().toLong(),
                                name = category,
                                items = items.map { TagCategorizable(it) }
                            )
                        }
                        .sortedBy { if (it.name == "未分类") "" else it.name }
                }
                CategoryPickerDialog(
                    title = "关联标签",
                    items = categorizableTags,
                    categoryGroups = groupedTags,
                    selectedIds = selectedTagIds,
                    multiSelect = true,
                    onItemsSelected = { selectedTagIds = it },
                    onDismiss = { showTagPicker = false },
                )
            }
        },
        trailing = {
            TextButton(
                onClick = { onDismiss(); onDelete() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            ) {
                Text("删除活动", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}
