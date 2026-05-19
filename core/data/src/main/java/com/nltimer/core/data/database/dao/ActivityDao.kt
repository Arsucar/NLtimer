package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import kotlinx.coroutines.flow.Flow

/**
 * ActivityDao 活动数据访问对象
 * 提供 activities 表的基础 CRUD、搜索、分组查询及归档操作
 */
@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Update
    suspend fun update(activity: ActivityEntity)

    @Delete
    suspend fun delete(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE isArchived = 0 ORDER BY name")
    fun getAllActive(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY name")
    fun getAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Query("SELECT * FROM activities WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ActivityEntity?

    @Query("UPDATE activities SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT * FROM activities WHERE name LIKE '%' || :query || '%' AND isArchived = 0")
    fun search(query: String): Flow<List<ActivityEntity>>

    /** 获取未分配分组的活动 */
    @Query("SELECT * FROM activities WHERE groupId IS NULL AND isArchived = 0 ORDER BY name")
    fun getUncategorized(): Flow<List<ActivityEntity>>

    /** 获取指定分组下的活动 */
    @Query("SELECT * FROM activities WHERE groupId = :groupId AND isArchived = 0 ORDER BY name")
    fun getByGroup(groupId: Long): Flow<List<ActivityEntity>>

    /** 获取所有预设活动 */
    @Query("SELECT * FROM activities WHERE isPreset = 1 AND isArchived = 0 ORDER BY name")
    fun getAllPresets(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isPreset = 1 AND isArchived = 0 ORDER BY name")
    suspend fun getAllPresetsSync(): List<ActivityEntity>

    /** 移动活动到指定分组（groupId 为 null 则取消分组） */
    @Query("UPDATE activities SET groupId = :groupId WHERE id = :activityId")
    suspend fun moveToGroup(activityId: Long, groupId: Long?)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()

    @Query("SELECT tagId FROM activity_tag_binding WHERE activityId = :activityId")
    suspend fun getTagIdsForActivitySync(activityId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>)

    @Query("DELETE FROM activity_tag_binding WHERE activityId = :activityId")
    suspend fun deleteActivityTagBindingsForActivity(activityId: Long)

    @Query("SELECT * FROM activities WHERE isArchived = 0 ORDER BY name")
    suspend fun getAllActiveSync(): List<ActivityEntity>
}
