package com.nltimer.core.data.repository

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import kotlinx.coroutines.flow.Flow

/**
 * ActivityRepository 活动基础仓库接口
 * 提供活动与分组的通用查询、搜索与归档操作
 */
interface ActivityRepository {
    fun getAllActive(): Flow<List<Activity>>
    fun getAll(): Flow<List<Activity>>
    fun getArchived(): Flow<List<Activity>>
    fun getAllGroups(): Flow<List<ActivityGroup>>
    fun search(query: String): Flow<List<Activity>>
    suspend fun getById(id: Long): Activity?
    suspend fun getByName(name: String): Activity?
    suspend fun insert(activity: Activity): Long
    suspend fun update(activity: Activity)
    suspend fun setArchived(id: Long, archived: Boolean)
}
