package com.nltimer.core.behaviorui.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Tag

@Suppress("LongParameterList")
@Composable
internal fun SheetPickerDialogs(
    showAddActivityDialog: Boolean,
    showAddTagDialog: Boolean,
    showActivityPicker: Boolean,
    showTagPicker: Boolean,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    selectedActivityId: Long?,
    selectedTagIds: Set<Long>,
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onAddActivityDialogDismiss: () -> Unit,
    onAddTagDialogDismiss: () -> Unit,
    onActivityPickerDismiss: () -> Unit,
    onTagPickerDismiss: () -> Unit,
    onActivitySelected: (Long) -> Unit,
    onTagsSelected: (Set<Long>) -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onAddActivity: (name: String, iconKey: String?, color: Long?, groupId: Long?, keywords: String?, tagIds: List<Long>) -> Unit,
    onAddTag: (name: String, color: Long?, icon: String?, priority: Int, category: String?, keywords: String?, activityId: Long?) -> Unit,
    onShowAddActivityDialog: () -> Unit,
    onShowAddTagDialog: () -> Unit,
) {
    if (showAddActivityDialog) {
        // Todo 待完善增加活动弹窗 以及绑定数据
        AddActivityDialog(
            allGroups = activityGroups,
            allTags = allTags,
            onDismiss = onAddActivityDialogDismiss,
            onConfirm = { name, iconKey, color, groupId, keywords, tagIds ->
                onAddActivity(name, iconKey, color, groupId, keywords, tagIds)
                onAddActivityDialogDismiss()
            },
        )
    }

    if (showAddTagDialog) {
        // Todo 待完善增加标签弹窗 以及绑定数据
        AddTagDialog(
            categories = emptyList(),
            allActivities = activities,
            activityGroups = activityGroups,
            onDismiss = onAddTagDialogDismiss,
            onConfirm = { name, color, icon, priority, category, keywords, activityId ->
                onAddTag(name, color, icon, priority, category, keywords, activityId)
                onAddTagDialogDismiss()
            },
        )
    }

    val categorizableActivities = remember(activities, activityLastUsedMap) {
        activities.map { ActivityCategorizable(it, activityLastUsedMap[it.id]) }
    }
    val categorizableTags = remember(allTags, tagLastUsedMap) {
        allTags.map { TagCategorizable(it, tagLastUsedMap[it.id]) }
    }

    if (showActivityPicker) {
        val activityGroupsMap = remember(activityGroups) {
            activityGroups.associateBy { it.id }
        }
        val groupedActivities = remember(activities, activityGroups, activityLastUsedMap) {
            val groups = activities.groupBy { it.groupId }
                .map { (groupId, items) ->
                    val group = if (groupId != null) activityGroupsMap[groupId] else null
                    CategoryGroup(
                        id = groupId ?: -1L,
                        name = group?.name ?: "未分类",
                        items = items.map { ActivityCategorizable(it, activityLastUsedMap[it.id]) },
                    )
                }
                .sortedBy { if (it.id == -1L) Long.MIN_VALUE else activityGroupsMap[it.id]?.sortOrder?.toLong() ?: Long.MAX_VALUE }
            groups
        }

        CategoryPickerDialog(
            title = "选择活动",
            items = categorizableActivities,
            categoryGroups = groupedActivities,
            selectedId = selectedActivityId,
            onItemSelected = { id ->
                onActivitySelected(id)
                onActivityPickerDismiss()
            },
            onDismiss = onActivityPickerDismiss,
            onCategoryReordered = { orderedIds ->
                onActivityGroupsReordered(orderedIds.filter { it != -1L })
            },
            onAddNew = {
                onActivityPickerDismiss()
                onShowAddActivityDialog()
            },
        )
    }

    if (showTagPicker) {
        val groupedTags = remember(allTags, tagLastUsedMap, tagCategoryOrder) {
            val order = tagCategoryOrder.withIndex().associate { it.value to it.index }
            val groups = allTags.groupBy { it.category ?: "未分类" }
                .map { (category, items) ->
                    CategoryGroup(
                        id = category.hashCode().toLong(),
                        name = category,
                        items = items.map { TagCategorizable(it, tagLastUsedMap[it.id]) },
                    )
                }
                .sortedWith(
                    compareBy<CategoryGroup<TagCategorizable>> { if (it.name == "未分类") Int.MIN_VALUE else order[it.name] ?: Int.MAX_VALUE }
                        .thenBy { it.name }
                )
            groups
        }

        CategoryPickerDialog(
            title = "选择标签",
            items = categorizableTags,
            categoryGroups = groupedTags,
            selectedIds = selectedTagIds,
            multiSelect = true,
            onItemsSelected = onTagsSelected,
            onDismiss = onTagPickerDismiss,
            onCategoryReordered = { orderedIds ->
                val idToName = groupedTags.associate { it.id to it.name }
                onTagCategoriesReordered(orderedIds.mapNotNull { idToName[it] })
            },
            onAddNew = {
                onTagPickerDismiss()
                onShowAddTagDialog()
            },
        )
    }
}
