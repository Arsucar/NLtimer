package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * TagDao 标签数据访问对象
 * 提供 tags 表的基础 CRUD、按分类/活动/行为筛选、搜索及分类管理操作
 */
@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE isArchived = 0 ORDER BY priority DESC, name")
    fun getAllActive(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name")
    fun getAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT * FROM tags WHERE category = :category AND isArchived = 0 ORDER BY priority DESC, name")
    fun getByCategory(category: String): Flow<List<TagEntity>>

    @Query("UPDATE tags SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' AND isArchived = 0")
    fun search(query: String): Flow<List<TagEntity>>

    /** 查询与指定活动绑定的标签（仅活动侧发起的绑定） */
    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN activity_tag_binding atb ON t.id = atb.tagId
        WHERE atb.activityId = :activityId AND atb.source = 'activity' AND t.isArchived = 0
        ORDER BY t.priority DESC, t.name
        """
    )
    fun getByActivityId(activityId: Long): Flow<List<TagEntity>>

    /** 查询与指定行为关联的标签（同步版本） */
    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN behavior_tag_cross_ref btc ON t.id = btc.tagId
        WHERE btc.behaviorId = :behaviorId
        ORDER BY t.priority DESC, t.name
        """
    )
    suspend fun getTagsForBehaviorSync(behaviorId: Long): List<TagEntity>

    @Query("SELECT DISTINCT category FROM tags WHERE category IS NOT NULL AND category != '' ORDER BY category")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM tags WHERE category IS NOT NULL AND category != '' ORDER BY category")
    suspend fun getDistinctCategoriesSync(): List<String>

    @Query("SELECT * FROM tags ORDER BY name")
    suspend fun getAllDistinctSync(): List<TagEntity>

    @Query("UPDATE tags SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)

    @Query("UPDATE tags SET category = NULL WHERE category = :category")
    suspend fun resetCategory(category: String)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("SELECT activityId FROM activity_tag_binding WHERE tagId = :tagId AND source = 'tag'")
    suspend fun getActivityIdsForTagSync(tagId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>)

    @Query("DELETE FROM activity_tag_binding WHERE tagId = :tagId AND source = 'tag'")
    suspend fun deleteActivityTagBindingsForTag(tagId: Long)
}
