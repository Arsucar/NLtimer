package com.nltimer.core.behaviorui.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.AddTagCallback
import com.nltimer.core.data.model.Tag
import com.nltimer.core.designsystem.form.ActivityFormSpecs
import com.nltimer.core.designsystem.form.FormRow
import com.nltimer.core.designsystem.form.GenericFormSheet
import com.nltimer.core.designsystem.form.parseColorHex

@Composable
fun AddTagDialog(
    categories: List<String>,
    allActivities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    onDismiss: () -> Unit,
    onConfirm: AddTagCallback,
) {
    var selectedCategory by remember { mutableStateOf(null as String?) }
    var selectedActivityId by remember { mutableStateOf(null as Long?) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showActivityPicker by remember { mutableStateOf(false) }

    val activityName = allActivities.find { it.id == selectedActivityId }?.name
    val activityCountText = activityName ?: "+ 增加"

    val specWithCategory = ActivityFormSpecs.createTag.copy(
        sections = ActivityFormSpecs.createTag.sections.map { section ->
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

    GenericFormSheet(
        spec = specWithCategory,
        initialData = null,
        onDismiss = onDismiss,
        onSubmit = { formState ->
            val name = formState["name"]?.trim() ?: ""
            val icon = formState["icon"]?.trim()?.ifBlank { null }
            val colorHex = formState["color"]?.trim()?.ifBlank { null }
            val priority = formState["priority"]?.toIntOrNull() ?: 0
            val keywords = formState["keywords"]?.trim()?.ifBlank { null }
            val color = parseColorHex(colorHex)
            if (name.isNotBlank()) {
                onConfirm(name, color, icon, priority, selectedCategory, keywords, selectedActivityId)
            }
        },
        overlay = {
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
                            clearLabel = "清除",
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
    )
}
