package com.nltimer.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.Tag
import com.nltimer.feature.home.model.AddSheetMode
import com.nltimer.feature.home.model.HomeUiState
import com.nltimer.core.behaviorui.sheet.AddBehaviorSheet
import com.nltimer.core.behaviorui.sheet.AddCurrentBehaviorSheet
import com.nltimer.core.behaviorui.sheet.AddTargetBehaviorSheet
import com.nltimer.core.tools.match.NoteProcessOutcome
import com.nltimer.core.tools.match.NoteScanResult
import java.time.LocalDateTime

@Composable
internal fun HomeSheetRouter(
    uiState: HomeUiState,
    activities: List<Activity>,
    activityGroups: List<ActivityGroup>,
    allTags: List<Tag>,
    dialogConfig: DialogGridConfig,
    activityLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagLastUsedMap: Map<Long, Long?> = emptyMap(),
    tagCategoryOrder: List<String> = emptyList(),
    onDismissSheet: () -> Unit,
    onAddBehavior: (activityId: Long, tagIds: List<Long>, startTime: LocalDateTime, endTime: LocalDateTime?, nature: BehaviorNature, note: String?, estimatedDurationMs: Long?) -> Unit,
    onAddActivity: (name: String, iconKey: String?, color: Long?, groupId: Long?, keywords: String?, tagIds: List<Long>) -> Unit,
    onAddTag: (name: String, color: Long?, icon: String?, priority: Int, category: String?, keywords: String?, activityId: Long?) -> Unit,
    onActivityGroupsReordered: (List<Long>) -> Unit = {},
    onTagCategoriesReordered: (List<String>) -> Unit = {},
    onMatchNote: (String) -> NoteScanResult,
    onProcessNote: suspend (String) -> NoteProcessOutcome,
    onQueryTagsForActivity: suspend (Long) -> List<Long> = { emptyList() },
    onQueryActivitiesForTag: suspend (Long) -> List<Long> = { emptyList() },
) {
    if (uiState.addSheetMode == null) return

    val existingBehaviors by remember(uiState.momentCells) {
        derivedStateOf {
            uiState.momentCells
                .filter { it.behaviorId != null && it.status != null }
                .map { cell ->
                    Behavior(
                        id = cell.behaviorId!!,
                        activityId = 0,
                        startTime = cell.startEpochMs
                            ?: error("startEpochMs missing for behaviorId=${cell.behaviorId}"),
                        endTime = cell.endEpochMs,
                        status = cell.status!!,
                        note = cell.note,
                        pomodoroCount = cell.pomodoroCount,
                        sequence = 0,
                        estimatedDuration = cell.estimatedDuration,
                        actualDuration = cell.actualDuration,
                        achievementLevel = cell.achievementLevel,
                        wasPlanned = cell.wasPlanned,
                    )
                }
        }
    }

    val nonActiveBehaviors by remember(existingBehaviors) {
        derivedStateOf { existingBehaviors.filter { it.status != BehaviorNature.ACTIVE } }
    }

    when (uiState.addSheetMode) {
        AddSheetMode.COMPLETED -> AddBehaviorSheet(
            activities = activities,
            activityGroups = activityGroups,
            allTags = allTags,
            dialogConfig = dialogConfig,
            initialStartTime = uiState.idleStartTime ?: uiState.lastBehaviorEndTime,
            initialEndTime = uiState.idleEndTime,
            initialActivityId = uiState.editInitialActivityId,
            initialTagIds = uiState.editInitialTagIds,
            initialNote = uiState.editInitialNote,
            editBehaviorId = uiState.editBehaviorId,
            existingBehaviors = existingBehaviors,
            activityLastUsedMap = activityLastUsedMap,
            tagLastUsedMap = tagLastUsedMap,
            tagCategoryOrder = tagCategoryOrder,
            onDismiss = onDismissSheet,
            onConfirm = onAddBehavior,
            onActivityGroupsReordered = onActivityGroupsReordered,
            onTagCategoriesReordered = onTagCategoriesReordered,
            onAddActivity = onAddActivity,
            onAddTag = onAddTag,
            onMatchNote = onMatchNote,
            onProcessNote = onProcessNote,
            onQueryTagsForActivity = onQueryTagsForActivity,
            onQueryActivitiesForTag = onQueryActivitiesForTag,
        )

        AddSheetMode.CURRENT -> AddCurrentBehaviorSheet(
            activities = activities,
            activityGroups = activityGroups,
            allTags = allTags,
            dialogConfig = dialogConfig,
            initialStartTime = uiState.idleStartTime ?: LocalDateTime.now(),
            initialActivityId = uiState.editInitialActivityId,
            initialTagIds = uiState.editInitialTagIds,
            initialNote = uiState.editInitialNote,
            editBehaviorId = uiState.editBehaviorId,
            existingBehaviors = nonActiveBehaviors,
            activityLastUsedMap = activityLastUsedMap,
            tagLastUsedMap = tagLastUsedMap,
            tagCategoryOrder = tagCategoryOrder,
            onDismiss = onDismissSheet,
            onConfirm = onAddBehavior,
            onActivityGroupsReordered = onActivityGroupsReordered,
            onTagCategoriesReordered = onTagCategoriesReordered,
            onAddActivity = onAddActivity,
            onAddTag = onAddTag,
            onMatchNote = onMatchNote,
            onProcessNote = onProcessNote,
            onQueryTagsForActivity = onQueryTagsForActivity,
            onQueryActivitiesForTag = onQueryActivitiesForTag,
        )

        AddSheetMode.TARGET -> AddTargetBehaviorSheet(
            activities = activities,
            activityGroups = activityGroups,
            allTags = allTags,
            dialogConfig = dialogConfig,
            initialActivityId = uiState.editInitialActivityId,
            initialTagIds = uiState.editInitialTagIds,
            initialNote = uiState.editInitialNote,
            editBehaviorId = uiState.editBehaviorId,
            existingBehaviors = existingBehaviors,
            activityLastUsedMap = activityLastUsedMap,
            tagLastUsedMap = tagLastUsedMap,
            tagCategoryOrder = tagCategoryOrder,
            onDismiss = onDismissSheet,
            onConfirm = onAddBehavior,
            onActivityGroupsReordered = onActivityGroupsReordered,
            onTagCategoriesReordered = onTagCategoriesReordered,
            onAddActivity = onAddActivity,
            onAddTag = onAddTag,
            onMatchNote = onMatchNote,
            onProcessNote = onProcessNote,
            onQueryTagsForActivity = onQueryTagsForActivity,
            onQueryActivitiesForTag = onQueryActivitiesForTag,
        )
    }
}
