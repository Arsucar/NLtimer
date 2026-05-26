package com.nltimer.core.data.repository.impl

import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.util.mapList
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val database: com.nltimer.core.data.database.NLtimerDatabase,
) : TagRepository {

    override fun getAllActive(): Flow<List<Tag>> =
        tagDao.getAllActive().mapList { Tag.fromEntity(it) }

    override fun getAll(): Flow<List<Tag>> =
        tagDao.getAll().mapList { Tag.fromEntity(it) }

    override fun getByCategory(category: String): Flow<List<Tag>> =
        tagDao.getByCategory(category).mapList { Tag.fromEntity(it) }

    override fun search(query: String): Flow<List<Tag>> =
        tagDao.search(query).mapList { Tag.fromEntity(it) }

    override fun getByActivityId(activityId: Long): Flow<List<Tag>> =
        tagDao.getByActivityId(activityId).mapList { Tag.fromEntity(it) }

    override suspend fun getById(id: Long): Tag? =
        tagDao.getById(id)?.let { Tag.fromEntity(it) }

    override suspend fun getByName(name: String): Tag? =
        tagDao.getByName(name)?.let { Tag.fromEntity(it) }

    override suspend fun insert(tag: Tag): Long =
        tagDao.insert(tag.toEntity())

    override suspend fun update(tag: Tag) =
        tagDao.update(tag.toEntity())

    override suspend fun setArchived(id: Long, archived: Boolean) =
        tagDao.setArchived(id, archived)

    override fun getDistinctCategories(): Flow<List<String>> =
        tagDao.getDistinctCategories()

    override suspend fun renameCategory(oldName: String, newName: String) =
        tagDao.renameCategory(oldName, newName)

    override suspend fun resetCategory(category: String) =
        tagDao.resetCategory(category)

    override suspend fun getActivityIdsForTag(tagId: Long): List<Long> =
        tagDao.getActivityIdsForTagSync(tagId)

    override suspend fun setActivityTagBindings(tagId: Long, activityIds: List<Long>) {
        database.withTransaction {
            tagDao.deleteActivityTagBindingsForTag(tagId)
            if (activityIds.isNotEmpty()) {
                tagDao.insertActivityTagBindings(
                    activityIds.map { activityId ->
                        ActivityTagBindingEntity(activityId = activityId, tagId = tagId, source = "tag")
                    }
                )
            }
        }
    }
}
