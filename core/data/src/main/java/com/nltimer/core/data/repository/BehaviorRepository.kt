package com.nltimer.core.data.repository

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * BehaviorRepository 行为记录仓库接口
 * 提供行为记录的增删改查、计时控制、标签关联以及日结操作
 */
interface BehaviorRepository {
    fun getByDayRange(dayStart: Long, dayEnd: Long): Flow<List<Behavior>>
    fun getCurrentBehavior(): Flow<Behavior?>
    fun getHomeBehaviors(dayStart: Long, dayEnd: Long): Flow<List<Behavior>>
    fun getTagsForBehavior(behaviorId: Long): Flow<List<Tag>>
    fun getPendingBehaviors(): Flow<List<Behavior>>
    suspend fun getBehaviorWithDetails(behaviorId: Long): BehaviorWithDetails?
    suspend fun getNextPending(): Behavior?
    suspend fun getMaxSequence(): Int
    suspend fun getEarliestBehaviorDate(): java.time.LocalDate?
    suspend fun insert(behavior: Behavior, tagIds: List<Long> = emptyList()): Long
    suspend fun setEndTime(id: Long, endTime: Long)
    suspend fun setStatus(id: Long, status: String)
    suspend fun setStartTime(id: Long, startTime: Long)
    suspend fun setActualDuration(id: Long, duration: Long)
    suspend fun setAchievementLevel(id: Long, level: Int)
    suspend fun setSequence(id: Long, sequence: Int)
    suspend fun setNote(id: Long, note: String?)
    suspend fun endCurrentBehavior(endTime: Long)
    suspend fun completeCurrentAndStartNext(currentId: Long, idleMode: Boolean): Behavior?
    suspend fun reorderGoals(orderedIds: List<Long>)
    suspend fun delete(id: Long)
    suspend fun settleDay(dayStart: Long, dayEnd: Long)

    suspend fun updateBehavior(
        id: Long,
        activityId: Long,
        startTime: Long,
        endTime: Long?,
        status: String,
        note: String?,
    )

    suspend fun updateTagsForBehavior(behaviorId: Long, tagIds: List<Long>)

    fun getBehaviorsOverlappingRange(rangeStart: Long, rangeEnd: Long): Flow<List<Behavior>>

    suspend fun getTagsForBehaviors(behaviorIds: List<Long>): Map<Long, List<Tag>>

    fun getBehaviorsWithDetailsByTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorWithDetails>>
    fun getBehaviorsWithDetailsOverlappingTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorWithDetails>>
    suspend fun getBehaviorsWithDetailsByTimeRangeSync(startTime: Long, endTime: Long): List<BehaviorWithDetails>
    fun getTotalDurationAllBehaviors(): Flow<Long>

    fun getAllActivityLastUsed(): Flow<Map<Long, Long?>>

    fun getAllTagLastUsed(): Flow<Map<Long, Long?>>
}
