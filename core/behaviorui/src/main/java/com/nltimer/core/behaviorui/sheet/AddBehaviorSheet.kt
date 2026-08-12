package com.nltimer.core.behaviorui.sheet

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nltimer.core.data.model.AddActivityCallback
import com.nltimer.core.data.model.AddTagCallback
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.Tag
import com.nltimer.core.designsystem.theme.NLtimerTheme
import com.nltimer.core.tools.match.NoteProcessOutcome
import com.nltimer.core.tools.match.NoteScanResult
import java.time.LocalDateTime

/** Sheet 透传给 ViewModel 的"智能识别"统一回调；默认 no-op 兜底无 directive 流的页面。 */
typealias OnProcessNote = suspend (note: String, selectedActivityId: Long?) -> NoteProcessOutcome

/** 查询活动绑定的标签ID列表 */
typealias QueryTagsForActivity = suspend (activityId: Long) -> List<Long>

/** 查询标签绑定的活动ID列表 */
typealias QueryActivitiesForTag = suspend (tagId: Long) -> List<Long>

private const val ScrimAlpha = 0.32f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBehaviorSheet(
    modifier: Modifier = Modifier,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag> = emptyList(),
    dialogConfig: DialogGridConfig = DialogGridConfig(),
    initialStartTime: LocalDateTime? = null,
    initialEndTime: LocalDateTime? = null,
    initialActivityId: Long? = null,
    initialTagIds: List<Long> = emptyList(),
    initialNote: String? = null,
    editBehaviorId: Long? = null,
    existingBehaviors: List<Behavior> = emptyList(),
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onAddActivity: AddActivityCallback = { _, _, _, _, _, _ -> },
    onAddTag: AddTagCallback = { _, _, _, _, _, _, _ -> },
    onProcessNote: OnProcessNote = { _, _ -> NoteProcessOutcome.Empty },
    onMatchNote: (String) -> NoteScanResult = { NoteScanResult(null, emptySet()) },
    onQueryTagsForActivity: QueryTagsForActivity = { emptyList() },
    onQueryActivitiesForTag: QueryActivitiesForTag = { emptyList() },
) {
    BehaviorSheetWrapper(
        modifier = modifier,
        mode = BehaviorNature.COMPLETED,
        activities = activities,
        activityGroups = activityGroups,
        allTags = allTags,
        dialogConfig = dialogConfig,
        initialStartTime = initialStartTime,
        initialEndTime = initialEndTime,
        initialActivityId = initialActivityId,
        initialTagIds = initialTagIds,
        initialNote = initialNote,
        editBehaviorId = editBehaviorId,
        existingBehaviors = existingBehaviors,
        activityLastUsedMap = activityLastUsedMap,
        tagLastUsedMap = tagLastUsedMap,
        tagCategoryOrder = tagCategoryOrder,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onActivityGroupsReordered = onActivityGroupsReordered,
        onTagCategoriesReordered = onTagCategoriesReordered,
        onAddActivity = onAddActivity,
        onAddTag = onAddTag,
        onProcessNote = onProcessNote,
        onMatchNote = onMatchNote,
        onQueryTagsForActivity = onQueryTagsForActivity,
        onQueryActivitiesForTag = onQueryActivitiesForTag,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCurrentBehaviorSheet(
    modifier: Modifier = Modifier,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag> = emptyList(),
    dialogConfig: DialogGridConfig = DialogGridConfig(),
    initialStartTime: LocalDateTime? = null,
    initialActivityId: Long? = null,
    initialTagIds: List<Long> = emptyList(),
    initialNote: String? = null,
    editBehaviorId: Long? = null,
    existingBehaviors: List<Behavior> = emptyList(),
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onAddActivity: AddActivityCallback = { _, _, _, _, _, _ -> },
    onAddTag: AddTagCallback = { _, _, _, _, _, _, _ -> },
    onProcessNote: OnProcessNote = { _, _ -> NoteProcessOutcome.Empty },
    onMatchNote: (String) -> NoteScanResult = { NoteScanResult(null, emptySet()) },
    onQueryTagsForActivity: QueryTagsForActivity = { emptyList() },
    onQueryActivitiesForTag: QueryActivitiesForTag = { emptyList() },
) {
    BehaviorSheetWrapper(
        modifier = modifier,
        mode = BehaviorNature.ACTIVE,
        activities = activities,
        activityGroups = activityGroups,
        allTags = allTags,
        dialogConfig = dialogConfig,
        initialStartTime = initialStartTime,
        initialActivityId = initialActivityId,
        initialTagIds = initialTagIds,
        initialNote = initialNote,
        editBehaviorId = editBehaviorId,
        existingBehaviors = existingBehaviors,
        activityLastUsedMap = activityLastUsedMap,
        tagLastUsedMap = tagLastUsedMap,
        tagCategoryOrder = tagCategoryOrder,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onActivityGroupsReordered = onActivityGroupsReordered,
        onTagCategoriesReordered = onTagCategoriesReordered,
        onAddActivity = onAddActivity,
        onAddTag = onAddTag,
        onProcessNote = onProcessNote,
        onMatchNote = onMatchNote,
        onQueryTagsForActivity = onQueryTagsForActivity,
        onQueryActivitiesForTag = onQueryActivitiesForTag,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTargetBehaviorSheet(
    modifier: Modifier = Modifier,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag> = emptyList(),
    dialogConfig: DialogGridConfig = DialogGridConfig(),
    initialActivityId: Long? = null,
    initialTagIds: List<Long> = emptyList(),
    initialNote: String? = null,
    initialEstimatedDurationMs: Long? = null,
    editBehaviorId: Long? = null,
    existingBehaviors: List<Behavior> = emptyList(),
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onAddActivity: AddActivityCallback = { _, _, _, _, _, _ -> },
    onAddTag: AddTagCallback = { _, _, _, _, _, _, _ -> },
    onProcessNote: OnProcessNote = { _, _ -> NoteProcessOutcome.Empty },
    onMatchNote: (String) -> NoteScanResult = { NoteScanResult(null, emptySet()) },
    onQueryTagsForActivity: QueryTagsForActivity = { emptyList() },
    onQueryActivitiesForTag: QueryActivitiesForTag = { emptyList() },
) {
    BehaviorSheetWrapper(
        modifier = modifier,
        mode = BehaviorNature.PENDING,
        activities = activities,
        activityGroups = activityGroups,
        allTags = allTags,
        dialogConfig = dialogConfig,
        initialActivityId = initialActivityId,
        initialTagIds = initialTagIds,
        initialNote = initialNote,
        initialEstimatedDurationMs = initialEstimatedDurationMs,
        editBehaviorId = editBehaviorId,
        existingBehaviors = existingBehaviors,
        activityLastUsedMap = activityLastUsedMap,
        tagLastUsedMap = tagLastUsedMap,
        tagCategoryOrder = tagCategoryOrder,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onActivityGroupsReordered = onActivityGroupsReordered,
        onTagCategoriesReordered = onTagCategoriesReordered,
        onAddActivity = onAddActivity,
        onAddTag = onAddTag,
        onProcessNote = onProcessNote,
        onMatchNote = onMatchNote,
        onQueryTagsForActivity = onQueryTagsForActivity,
        onQueryActivitiesForTag = onQueryActivitiesForTag,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BehaviorSheetWrapper(
    modifier: Modifier,
    mode: BehaviorNature,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    dialogConfig: DialogGridConfig,
    initialStartTime: LocalDateTime? = null,
    initialEndTime: LocalDateTime? = null,
    initialActivityId: Long? = null,
    initialTagIds: List<Long> = emptyList(),
    initialNote: String? = null,
    initialEstimatedDurationMs: Long? = null,
    editBehaviorId: Long? = null,
    existingBehaviors: List<Behavior> = emptyList(),
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onAddActivity: AddActivityCallback,
    onAddTag: AddTagCallback,
    onProcessNote: OnProcessNote,
    onMatchNote: (String) -> NoteScanResult,
    onQueryTagsForActivity: QueryTagsForActivity,
    onQueryActivitiesForTag: QueryActivitiesForTag,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha),
    ) {
        AddBehaviorSheetContent(
            modifier = modifier.imePadding(),
            mode = mode,
            activities = activities,
            activityGroups = activityGroups,
            allTags = allTags,
            dialogConfig = dialogConfig,
            initialStartTime = initialStartTime,
            initialEndTime = initialEndTime,
            initialActivityId = initialActivityId,
            initialTagIds = initialTagIds,
            initialNote = initialNote,
            initialEstimatedDurationMs = initialEstimatedDurationMs,
            editBehaviorId = editBehaviorId,
            existingBehaviors = existingBehaviors,
            activityLastUsedMap = activityLastUsedMap,
            tagLastUsedMap = tagLastUsedMap,
            tagCategoryOrder = tagCategoryOrder,
            onConfirm = { activityId, tagIds, startTime, endTime, nature, note, estDuration ->
                onConfirm(activityId, tagIds, startTime, endTime, nature, note, estDuration)
                onDismiss()
            },
            onDismiss = onDismiss,
            onActivityGroupsReordered = onActivityGroupsReordered,
            onTagCategoriesReordered = onTagCategoriesReordered,
            onAddActivity = onAddActivity,
            onAddTag = onAddTag,
            onProcessNote = onProcessNote,
            onMatchNote = onMatchNote,
            onQueryTagsForActivity = onQueryTagsForActivity,
            onQueryActivitiesForTag = onQueryActivitiesForTag,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun AddBehaviorSheetPreview() {
    val sampleActivities = listOf(
        Activity(1, "Coding", "👨‍💻"),
        Activity(2, "Reading", "📚"),
        Activity(3, "Workout", "💪")
    )
    val sampleTags = listOf(
        Tag(1, "Work", null, null, null, null, 0, 0, 0, null, false),
        Tag(2, "Study", null, null, null, null, 0, 0, 0, null, false)
    )
    val sampleGroups = listOf(
        ActivityGroup(1, "工作", null, 0),
        ActivityGroup(2, "学习", null, 1),
    )

    NLtimerTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            AddBehaviorSheetContent(
                activities = sampleActivities,
                activityGroups = sampleGroups,
                allTags = sampleTags,
                dialogConfig = DialogGridConfig(),
                existingBehaviors = emptyList(),
                onConfirm = { _, _, _, _, _, _, _ -> },
                onDismiss = { },
            )
        }
    }
}
