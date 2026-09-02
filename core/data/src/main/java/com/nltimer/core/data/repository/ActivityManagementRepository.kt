package com.nltimer.core.data.repository

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.ActivityStats
import kotlinx.coroutines.flow.Flow

/**
 * ActivityManagementRepository 活动管理仓库接口
 * 提供活动与分组的增删改查、统计与预设初始化功能
 */
interface ActivityManagementRepository {
    /** 获取所有未归档的活动 */
    fun getAllActivities(): Flow<List<Activity>>
    /** 获取所有已归档的活动 */
    fun getArchived(): Flow<List<Activity>>
    /** 获取未分组的活动 */
    fun getUncategorizedActivities(): Flow<List<Activity>>
    /** 获取指定分组下的活动 */
    fun getActivitiesByGroup(groupId: Long): Flow<List<Activity>>
    /** 获取所有分组 */
    fun getAllGroups(): Flow<List<ActivityGroup>>
    /** 获取指定活动的使用统计 */
    fun getActivityStats(activityId: Long): Flow<ActivityStats>

    suspend fun addActivity(activity: Activity): Long
    suspend fun updateActivity(activity: Activity)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun deleteActivity(id: Long)
    suspend fun moveActivityToGroup(activityId: Long, groupId: Long?)

    suspend fun addGroup(name: String): Long
    suspend fun renameGroup(id: Long, newName: String)
    suspend fun deleteGroup(id: Long)
    suspend fun reorderGroups(orderedIds: List<Long>)

    /** 初始化预设活动列表 */
    suspend fun initializePresets()

    suspend fun getTagIdsForActivity(activityId: Long): List<Long>
    suspend fun setActivityTagBindings(activityId: Long, tagIds: List<Long>)
    suspend fun getAllActivitiesSync(): List<Activity>
}
