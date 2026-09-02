package com.nltimer.core.data.repository

import com.nltimer.core.data.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * TagRepository 标签仓库接口
 * 提供标签的增删改查、搜索、按分类筛选以及分类管理功能
 */
interface TagRepository {
    fun getAllActive(): Flow<List<Tag>>
    fun getAll(): Flow<List<Tag>>
    fun getArchived(): Flow<List<Tag>>
    fun getByCategory(category: String): Flow<List<Tag>>
    fun search(query: String): Flow<List<Tag>>
    fun getByActivityId(activityId: Long): Flow<List<Tag>>
    suspend fun getById(id: Long): Tag?
    suspend fun getByName(name: String): Tag?
    suspend fun insert(tag: Tag): Long
    suspend fun update(tag: Tag)
    suspend fun setArchived(id: Long, archived: Boolean)
    
    fun getDistinctCategories(): Flow<List<String>>
    suspend fun renameCategory(oldName: String, newName: String)
    suspend fun resetCategory(category: String)

    suspend fun getActivityIdsForTag(tagId: Long): List<Long>
    suspend fun setActivityTagBindings(tagId: Long, activityIds: List<Long>)
}
