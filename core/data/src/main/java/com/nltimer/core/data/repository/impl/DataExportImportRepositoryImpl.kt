package com.nltimer.core.data.repository.impl

import androidx.room.withTransaction
import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.TagEntity
import com.nltimer.core.data.model.ExportData
import com.nltimer.core.data.model.ExportedActivity
import com.nltimer.core.data.model.ExportedActivityGroup
import com.nltimer.core.data.model.ExportedTag
import com.nltimer.core.data.repository.DataExportImportRepository
import com.nltimer.core.data.repository.ImportMode
import com.nltimer.core.data.repository.ImportResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExportImportRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao,
    private val activityGroupDao: ActivityGroupDao,
    private val tagDao: TagDao,
    private val behaviorDao: BehaviorDao,
    private val database: NLtimerDatabase,
) : DataExportImportRepository {

    override suspend fun exportAll(): ExportData {
        val groups = activityGroupDao.getAllSync()
        val activities = activityDao.getAllActiveSync()
        val tags = tagDao.getAllDistinctSync()
        val categories = tagDao.getDistinctCategoriesSync()

        val activityTagBindings = behaviorDao.getAllActivityTagBindingsSync()
        val tagIdToName = tags.associate { it.id to it.name }
        val activityIdToTagNames = activityTagBindings.groupBy { it.activityId }
            .mapValues { (_, bindings) ->
                bindings.mapNotNull { tagIdToName[it.tagId] }
            }

        val groupIdToName = groups.associate { it.id to it.name }

        return ExportData(
            activities = activities.map { it.toExported(groupIdToName, activityIdToTagNames) },
            activityGroups = groups.map { it.toExported() },
            tags = tags.map { it.toExported() },
            tagCategories = categories,
        )
    }

    override suspend fun exportActivities(): ExportData {
        val groups = activityGroupDao.getAllSync()
        val activities = activityDao.getAllActiveSync()
        val tags = tagDao.getAllDistinctSync()

        val activityTagBindings = behaviorDao.getAllActivityTagBindingsSync()
        val tagIdToName = tags.associate { it.id to it.name }
        val activityIdToTagNames = activityTagBindings.groupBy { it.activityId }
            .mapValues { (_, bindings) ->
                bindings.mapNotNull { tagIdToName[it.tagId] }
            }

        val groupIdToName = groups.associate { it.id to it.name }

        return ExportData(
            activities = activities.map { it.toExported(groupIdToName, activityIdToTagNames) },
        )
    }

    override suspend fun exportTags(): ExportData {
        val tags = tagDao.getAllDistinctSync()
        val categories = tagDao.getDistinctCategoriesSync()
        return ExportData(
            tags = tags.map { it.toExported() },
            tagCategories = categories,
        )
    }

    override suspend fun exportCategories(): ExportData {
        val groups = activityGroupDao.getAllSync()
        val categories = tagDao.getDistinctCategoriesSync()
        return ExportData(
            activityGroups = groups.map { it.toExported() },
            tagCategories = categories,
        )
    }

    override suspend fun importAll(data: ExportData, mode: ImportMode): ImportResult {
        return try {
            when (mode) {
                ImportMode.SMART -> importAllSmart(data)
                ImportMode.OVERWRITE -> importAllOverwrite(data)
            }
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun importActivities(data: ExportData, mode: ImportMode): ImportResult {
        val activities = data.activities ?: return ImportResult.Success()
        return try {
            when (mode) {
                ImportMode.SMART -> importActivitiesSmart(activities)
                ImportMode.OVERWRITE -> importActivitiesOverwrite(activities)
            }
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun importTags(data: ExportData, mode: ImportMode): ImportResult {
        val tags = data.tags ?: return ImportResult.Success()
        return try {
            when (mode) {
                ImportMode.SMART -> importTagsSmart(tags)
                ImportMode.OVERWRITE -> importTagsOverwrite(tags)
            }
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun importCategories(data: ExportData, mode: ImportMode): ImportResult {
        val categories = data.tagCategories ?: return ImportResult.Success()
        return try {
            when (mode) {
                ImportMode.SMART -> importCategoriesSmart(categories)
                ImportMode.OVERWRITE -> importCategoriesOverwrite(categories)
            }
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun importAllSmart(data: ExportData): ImportResult {
        val groups = data.activityGroups ?: emptyList()
        val tags = data.tags ?: emptyList()
        val activities = data.activities ?: emptyList()

        val groupNameToId = mutableMapOf<String, Long>()
        for (group in groups) {
            val existing = activityGroupDao.getByName(group.name)
            if (existing != null) {
                val merged = existing.mergeFrom(group)
                activityGroupDao.update(merged)
                groupNameToId[group.name] = existing.id
            } else {
                val id = activityGroupDao.insert(group.toEntity())
                groupNameToId[group.name] = id
            }
        }

        val tagNameToId = mutableMapOf<String, Long>()
        for (tag in tags) {
            val existing = tagDao.getByName(tag.name)
            if (existing != null) {
                val merged = existing.mergeFrom(tag)
                tagDao.update(merged)
                tagNameToId[tag.name] = existing.id
            } else {
                val id = tagDao.insert(tag.toEntity())
                tagNameToId[tag.name] = id
            }
        }

        val activityTagBindings = mutableListOf<ActivityTagBindingEntity>()
        for (activity in activities) {
            val groupId = activity.groupName?.let { groupNameToId[it] }
            val existing = activityDao.getByName(activity.name)
            if (existing != null) {
                val merged = existing.mergeFrom(activity, groupId)
                activityDao.update(merged)
            } else {
                val id = activityDao.insert(activity.toEntity(groupId))
                val tagIds = activity.tagNames.mapNotNull { tagNameToId[it] }
                for (tagId in tagIds) {
                    activityTagBindings.add(ActivityTagBindingEntity(activityId = id, tagId = tagId))
                }
            }
        }
        if (activityTagBindings.isNotEmpty()) {
            behaviorDao.insertActivityTagBindings(activityTagBindings)
        }

        return ImportResult.Success(
            activityGroupsImported = groups.size,
            activitiesImported = activities.size,
            tagsImported = tags.size,
            tagCategoriesImported = data.tagCategories?.size ?: 0,
        )
    }

    private suspend fun importAllOverwrite(data: ExportData): ImportResult {
        database.withTransaction {
            behaviorDao.deleteAllTagCrossRefs()
            behaviorDao.deleteAllActivityTagBindings()
            behaviorDao.deleteAll()
            tagDao.deleteAll()
            activityDao.deleteAll()
            activityGroupDao.deleteAll()

            val importedGroups = data.activityGroups ?: emptyList()
            val groupNameToId = mutableMapOf<String, Long>()
            for (group in importedGroups) {
                val id = activityGroupDao.insert(group.toEntity())
                groupNameToId[group.name] = id
            }

            val importedTags = data.tags ?: emptyList()
            val tagNameToId = mutableMapOf<String, Long>()
            for (tag in importedTags) {
                val id = tagDao.insert(tag.toEntity())
                tagNameToId[tag.name] = id
            }

            val importedActivities = data.activities ?: emptyList()
            val activityTagBindings = mutableListOf<ActivityTagBindingEntity>()
            for (activity in importedActivities) {
                val groupId = activity.groupName?.let { groupNameToId[it] }
                val id = activityDao.insert(activity.toEntity(groupId))
                val tagIds = activity.tagNames.mapNotNull { tagNameToId[it] }
                for (tagId in tagIds) {
                    activityTagBindings.add(ActivityTagBindingEntity(activityId = id, tagId = tagId))
                }
            }
            if (activityTagBindings.isNotEmpty()) {
                behaviorDao.insertActivityTagBindings(activityTagBindings)
            }
        }

        return ImportResult.Success(
            activityGroupsImported = data.activityGroups?.size ?: 0,
            activitiesImported = data.activities?.size ?: 0,
            tagsImported = data.tags?.size ?: 0,
            tagCategoriesImported = data.tagCategories?.size ?: 0,
        )
    }

    private suspend fun importActivitiesSmart(activities: List<ExportedActivity>): ImportResult {
        val groups = activityGroupDao.getAllSync()
        val groupNameToId = groups.associate { it.name to it.id }

        val activityTagBindings = mutableListOf<ActivityTagBindingEntity>()
        var imported = 0
        for (activity in activities) {
            val groupId = activity.groupName?.let { groupNameToId[it] }
            val existing = activityDao.getByName(activity.name)
            if (existing != null) {
                val merged = existing.mergeFrom(activity, groupId)
                activityDao.update(merged)
            } else {
                val id = activityDao.insert(activity.toEntity(groupId))
                val tags = tagDao.getAllDistinctSync()
                val tagNameToId = tags.associate { it.name to it.id }
                val tagIds = activity.tagNames.mapNotNull { tagNameToId[it] }
                for (tagId in tagIds) {
                    activityTagBindings.add(ActivityTagBindingEntity(activityId = id, tagId = tagId))
                }
                imported++
            }
        }
        if (activityTagBindings.isNotEmpty()) {
            behaviorDao.insertActivityTagBindings(activityTagBindings)
        }

        return ImportResult.Success(activitiesImported = imported)
    }

    private suspend fun importActivitiesOverwrite(activities: List<ExportedActivity>): ImportResult {
        database.withTransaction {
            behaviorDao.deleteAllActivityTagBindings()
            behaviorDao.deleteAll()
            activityDao.deleteAll()

            val tags = tagDao.getAllDistinctSync()
            val tagNameToId = tags.associate { it.name to it.id }
            val groups = activityGroupDao.getAllSync()
            val groupNameToId = groups.associate { it.name to it.id }

            val activityTagBindings = mutableListOf<ActivityTagBindingEntity>()
            for (activity in activities) {
                val groupId = activity.groupName?.let { groupNameToId[it] }
                val id = activityDao.insert(activity.toEntity(groupId))
                val tagIds = activity.tagNames.mapNotNull { tagNameToId[it] }
                for (tagId in tagIds) {
                    activityTagBindings.add(ActivityTagBindingEntity(activityId = id, tagId = tagId))
                }
            }
            if (activityTagBindings.isNotEmpty()) {
                behaviorDao.insertActivityTagBindings(activityTagBindings)
            }
        }

        return ImportResult.Success(activitiesImported = activities.size)
    }

    private suspend fun importTagsSmart(tags: List<ExportedTag>): ImportResult {
        var imported = 0
        for (tag in tags) {
            val existing = tagDao.getByName(tag.name)
            if (existing != null) {
                val merged = existing.mergeFrom(tag)
                tagDao.update(merged)
            } else {
                tagDao.insert(tag.toEntity())
                imported++
            }
        }
        return ImportResult.Success(tagsImported = imported)
    }

    private suspend fun importTagsOverwrite(tags: List<ExportedTag>): ImportResult {
        database.withTransaction {
            behaviorDao.deleteAllTagCrossRefs()
            behaviorDao.deleteAllActivityTagBindings()
            tagDao.deleteAll()

            for (tag in tags) {
                tagDao.insert(tag.toEntity())
            }
        }
        return ImportResult.Success(tagsImported = tags.size)
    }

    override suspend fun exportByDateRange(startMs: Long, endMs: Long): ExportData {
        val behaviors = behaviorDao.getByTimeRangeSync(startMs, endMs)
        val activityIds = behaviors.map { it.activityId }.toSet()
        val groups = activityGroupDao.getAllSync()
        val groupIdToName = groups.associate { it.id to it.name }
        val activities = activityIds.mapNotNull { activityDao.getById(it) }
        val involvedGroupIds = activities.mapNotNull { it.groupId }.toSet()
        val involvedGroups = groups.filter { it.id in involvedGroupIds }

        val activityTagBindings = behaviorDao.getAllActivityTagBindingsSync()
            .filter { it.activityId in activityIds }
        val tagIdsFromActivities = activityTagBindings.map { it.tagId }.toMutableSet()
        val behaviorTagRows = behaviorDao.getTagsForBehaviorsSync(behaviors.map { it.id })
        tagIdsFromActivities.addAll(behaviorTagRows.map { it.id })
        val allTagEntities = tagDao.getAllDistinctSync()
        val involvedTags = allTagEntities.filter { it.id in tagIdsFromActivities }
        val tagIdToName = involvedTags.associate { it.id to it.name }
        val activityIdToTagNames = activityTagBindings.groupBy { it.activityId }
            .mapValues { (_, bindings) -> bindings.mapNotNull { tagIdToName[it.tagId] } }

        val categories = tagDao.getDistinctCategoriesSync()

        return ExportData(
            activities = activities.map { it.toExported(groupIdToName, activityIdToTagNames) },
            activityGroups = involvedGroups.map { it.toExported() },
            tags = involvedTags.map { it.toExported() },
            tagCategories = categories,
        )
    }

    private suspend fun importCategoriesSmart(categories: List<String>): ImportResult {
        val existing = tagDao.getDistinctCategoriesSync().toSet()
        var imported = 0
        for (category in categories) {
            if (category !in existing) {
                imported++
            }
        }
        return ImportResult.Success(tagCategoriesImported = imported)
    }

    private suspend fun importCategoriesOverwrite(categories: List<String>): ImportResult {
        return ImportResult.Success(tagCategoriesImported = categories.size)
    }
}

private fun ActivityEntity.toExported(
    groupIdToName: Map<Long, String>,
    activityIdToTagNames: Map<Long, List<String>>,
): ExportedActivity {
    return ExportedActivity(
        name = name,
        iconKey = iconKey,
        keywords = keywords,
        groupName = groupId?.let { groupIdToName[it] },
        isPreset = isPreset,
        isArchived = isArchived,
        archivedAt = archivedAt,
        color = color,
        usageCount = usageCount,
        tagNames = activityIdToTagNames[id] ?: emptyList(),
    )
}

private fun ActivityGroupEntity.toExported(): ExportedActivityGroup {
    return ExportedActivityGroup(
        name = name,
        sortOrder = sortOrder,
        isArchived = isArchived,
        archivedAt = archivedAt,
    )
}

private fun TagEntity.toExported(): ExportedTag {
    return ExportedTag(
        name = name,
        color = color,
        iconKey = iconKey,
        category = category,
        priority = priority,
        usageCount = usageCount,
        sortOrder = sortOrder,
        keywords = keywords,
        isArchived = isArchived,
        archivedAt = archivedAt,
    )
}

private fun ExportedActivityGroup.toEntity(): ActivityGroupEntity {
    return ActivityGroupEntity(
        name = name,
        sortOrder = sortOrder,
        isArchived = isArchived,
        archivedAt = archivedAt,
    )
}

private fun ExportedTag.toEntity(): TagEntity {
    return TagEntity(
        name = name,
        color = color,
        iconKey = iconKey,
        category = category,
        priority = priority,
        usageCount = usageCount,
        sortOrder = sortOrder,
        keywords = keywords,
        isArchived = isArchived,
        archivedAt = archivedAt,
    )
}

private fun ExportedActivity.toEntity(groupId: Long?): ActivityEntity {
    return ActivityEntity(
        name = name,
        iconKey = iconKey,
        keywords = keywords,
        groupId = groupId,
        isPreset = isPreset,
        isArchived = isArchived,
        archivedAt = archivedAt,
        color = color,
        usageCount = usageCount,
    )
}

private fun ActivityGroupEntity.mergeFrom(other: ExportedActivityGroup): ActivityGroupEntity {
    return copy(
        sortOrder = if (other.sortOrder != 0) other.sortOrder else sortOrder,
        isArchived = other.isArchived,
        archivedAt = other.archivedAt ?: archivedAt,
    )
}

private fun TagEntity.mergeFrom(other: ExportedTag): TagEntity {
    return copy(
        color = other.color ?: color,
        iconKey = other.iconKey ?: iconKey,
        category = other.category ?: category,
        priority = if (other.priority != 0) other.priority else priority,
        usageCount = if (other.usageCount != 0) other.usageCount else usageCount,
        sortOrder = if (other.sortOrder != 0) other.sortOrder else sortOrder,
        keywords = other.keywords ?: keywords,
        isArchived = other.isArchived,
        archivedAt = other.archivedAt ?: archivedAt,
    )
}

private fun ActivityEntity.mergeFrom(other: ExportedActivity, groupId: Long?): ActivityEntity {
    return copy(
        iconKey = other.iconKey ?: iconKey,
        keywords = other.keywords ?: keywords,
        groupId = groupId ?: this.groupId,
        isArchived = other.isArchived,
        archivedAt = other.archivedAt ?: archivedAt,
        color = other.color ?: color,
        usageCount = if (other.usageCount != 0) other.usageCount else usageCount,
    )
}
