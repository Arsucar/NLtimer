package com.nltimer.feature.tag_management.ui.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Tag
import com.nltimer.core.behaviorui.sheet.ActivityCategorizable
import com.nltimer.core.behaviorui.sheet.CategoryGroup
import com.nltimer.core.behaviorui.sheet.CategoryPickerDialog
import com.nltimer.core.behaviorui.sheet.StringCategoryCategorizable
import com.nltimer.core.designsystem.form.ActivityFormSpecs
import com.nltimer.core.designsystem.form.FormRow
import com.nltimer.core.designsystem.form.GenericFormSheet
import com.nltimer.core.designsystem.form.parseColorHex
import com.nltimer.core.debugui.FieldDetailDialog
import com.nltimer.core.debugui.toFieldInfoList
import com.nltimer.core.debugui.toJsonString

@Composable
fun EditTagFormSheet(
    tag: Tag,
    categories: List<String>,
    allActivities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    initialActivityId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Tag, Long?) -> Unit,
    onDelete: () -> Unit = {},
) {
    var selectedCategory by remember { mutableStateOf(tag.category) }
    var selectedActivityId by remember { mutableStateOf(initialActivityId) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showActivityPicker by remember { mutableStateOf(false) }
    var showFieldDetail by remember { mutableStateOf(false) }

    val activityName = allActivities.find { it.id == selectedActivityId }?.name
    val activityCountText = activityName ?: "+ 增加"

    // DIFF: 复杂多字段变更，无法用 withUpdatedLabelAction 简化
    val specWithCategory = ActivityFormSpecs.editTag().copy(
        sections = ActivityFormSpecs.editTag().sections.map { section ->
            section.copy(
                rows = section.rows.map { row ->
                    when {
                        row is FormRow.LabelAction && row.key == "category" -> row.copy(
                            actionText = selectedCategory ?: "未分类",
                            onClick = { showCategoryPicker = true },
                        )
                        row is FormRow.LabelAction && row.key == "activities" -> row.copy(
                            actionText = activityCountText,
                            onClick = { showActivityPicker = true },
                        )
                        else -> row
                    }
                },
            )
        },
    )

    val initialData = mapOf(
        "icon" to (tag.iconKey ?: "🏷️"),
        "color" to (tag.color?.let { (it and 0xFFFFFFFF.toLong()).toString(16) } ?: ""),
        "name" to tag.name,
        "keywords" to (tag.keywords ?: ""),
        "priority" to tag.priority.toString(),
        "isArchived" to tag.isArchived.toString(),
    )

    GenericFormSheet(
        spec = specWithCategory,
        initialData = initialData,
        onDismiss = onDismiss,
        onSubmit = { formState ->
            val name = formState["name"]?.trim() ?: tag.name
            val icon = formState["icon"]?.trim()?.ifBlank { null }
            val colorHex = formState["color"]?.trim()?.ifBlank { null }
            val priority = formState["priority"]?.toIntOrNull() ?: tag.priority
            val isArchived = formState["isArchived"]?.toBooleanStrictOrNull() ?: tag.isArchived
            val keywords = formState["keywords"]?.trim()?.ifBlank { null }
            val color = parseColorHex(colorHex)
            onConfirm(
                tag.copy(
                    name = name,
                    iconKey = icon,
                    color = color,
                    priority = priority,
                    category = selectedCategory,
                    keywords = keywords,
                    isArchived = isArchived,
                ),
                selectedActivityId,
            )
        },
        overlay = {
            val context = LocalContext.current
            val isDebug = remember(context) {
                (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            }
            if (isDebug) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { showFieldDetail = true },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "字段详细",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (showCategoryPicker) {
                val categoryItems = remember(categories) {
                    categories.map { StringCategoryCategorizable(it) }
                }
                val groupedCategories = remember(categoryItems) {
                    listOf(
                        CategoryGroup(
                            id = 0L,
                            name = "所有分类",
                            items = categoryItems,
                            onClear = {
                                selectedCategory = null
                                showCategoryPicker = false
                            },
                            clearLabel = "清除",
                        )
                    )
                }
                CategoryPickerDialog(
                    title = "选择所属分类",
                    items = categoryItems,
                    categoryGroups = groupedCategories,
                    selectedId = selectedCategory?.hashCode()?.toLong() ?: 0L,
                    onItemSelected = { id ->
                        val selectedName = categoryItems.find { it.itemId == id }?.name
                        selectedCategory = selectedName
                        showCategoryPicker = false
                    },
                    onDismiss = { showCategoryPicker = false },
                    showHeader = false,
                )
            }
            if (showActivityPicker) {
                val categorizableActivities = remember(allActivities) {
                    allActivities.map { ActivityCategorizable(it) }
                }
                val activityGroupsMap = remember(activityGroups) {
                    activityGroups.associateBy { it.id }
                }
                val groupedActivities = remember(allActivities, activityGroups) {
                    val initialGroups = allActivities.groupBy { it.groupId }
                    val hasUncategorized = initialGroups.containsKey(null)
                    
                    val baseGroups = initialGroups.map { (groupId, items) ->
                        val group = if (groupId != null) activityGroupsMap[groupId] else null
                        CategoryGroup(
                            id = groupId ?: -1L,
                            name = group?.name ?: "未分类",
                            items = items.map { ActivityCategorizable(it) },
                            onClear = if (groupId == null) {
                                {
                                    selectedActivityId = null
                                    showActivityPicker = false
                                }
                            } else null,
                            clearLabel = if (groupId == null) "清除" else null,
                        )
                    }
                    
                    val finalGroups = if (hasUncategorized) {
                        baseGroups
                    } else {
                        baseGroups + CategoryGroup<ActivityCategorizable>(
                            id = -1L,
                            name = "未分类",
                            items = emptyList(),
                            onClear = {
                                selectedActivityId = null
                                showActivityPicker = false
                            },
                            clearLabel = "未关联",
                        )
                    }
                    
                    finalGroups.sortedBy { if (it.id == -1L) Long.MIN_VALUE else activityGroupsMap[it.id]?.sortOrder?.toLong() ?: Long.MAX_VALUE }
                }

                CategoryPickerDialog(
                    title = "关联活动",
                    items = categorizableActivities,
                    categoryGroups = groupedActivities,
                    selectedId = selectedActivityId ?: -1L,
                    onItemSelected = { id ->
                        selectedActivityId = if (id == -1L) null else id
                        showActivityPicker = false
                    },
                    onDismiss = { showActivityPicker = false },
                )
            }
        },
        trailing = {
            TextButton(
                onClick = { onDismiss(); onDelete() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            ) {
                Text("删除标签", color = MaterialTheme.colorScheme.error)
            }
        },
    )

    if (showFieldDetail) {
        val fields = remember(tag) { tag.toFieldInfoList() }
        val rawJson = remember(fields) { fields.toJsonString() }

        FieldDetailDialog(
            title = "标签字段详情",
            fields = fields,
            rawJson = rawJson,
            onDismiss = { showFieldDetail = false },
        )
    }
}
