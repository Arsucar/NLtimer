package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.BehaviorEntity
import com.nltimer.core.data.database.entity.BehaviorTagCrossRefEntity
import com.nltimer.core.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

data class BehaviorTagRow(
    val behaviorId: Long,
    val id: Long,
    val name: String,
    val color: Long?,
    val iconKey: String?,
    val category: String?,
    val priority: Int,
    val usageCount: Int,
    val sortOrder: Int,
    val keywords: String?,
    val isArchived: Boolean,
    val archivedAt: Long?,
)

data class ActivityStatsRow(
    val usageCount: Int = 0,
    val totalDurationMinutes: Long = 0,
    val lastUsedTimestamp: Long? = null,
)

data class LastUsedRow(
    val id: Long,
    val lastUsedTimestamp: Long?,
)

/**
 * BehaviorDao 行为记录数据访问对象
 * 提供 behaviors 表的基础 CRUD、计时字段更新、状态变更、统计查询及标签关联管理
 */
@Dao
interface BehaviorDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(behavior: BehaviorEntity): Long

    // 单字段更新方法
    @Query("UPDATE behaviors SET endTime = :endTime WHERE id = :id")
    suspend fun setEndTime(id: Long, endTime: Long)

    @Query("UPDATE behaviors SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE behaviors SET startTime = :startTime WHERE id = :id")
    suspend fun setStartTime(id: Long, startTime: Long)

    @Query("UPDATE behaviors SET actualDuration = :duration WHERE id = :id")
    suspend fun setActualDuration(id: Long, duration: Long)

    @Query("UPDATE behaviors SET achievementLevel = :level WHERE id = :id")
    suspend fun setAchievementLevel(id: Long, level: Int)

    @Query("UPDATE behaviors SET sequence = :sequence WHERE id = :id")
    suspend fun setSequence(id: Long, sequence: Int)

    @Query("UPDATE behaviors SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String?)

    @Query("DELETE FROM behaviors WHERE id = :id")
    suspend fun delete(id: Long)

    /** 查询指定时间范围内的行为记录 */
    @Query(
        """
        SELECT * FROM behaviors
        WHERE startTime >= :dayStart AND startTime < :dayEnd
        ORDER BY startTime ASC
        """
    )
    fun getByDayRange(dayStart: Long, dayEnd: Long): Flow<List<BehaviorEntity>>

    /** 获取当前正在进行的活动（status=active 且未结束） */
    @Query("SELECT * FROM behaviors WHERE status = 'active' AND endTime IS NULL LIMIT 1")
    fun getCurrentBehavior(): Flow<BehaviorEntity?>

    /** 结束所有当前进行中的行为 */
    @Query("UPDATE behaviors SET endTime = :endTime, status = 'completed' WHERE status = 'active' AND endTime IS NULL")
    suspend fun endCurrentBehavior(endTime: Long)

    /** 获取首页展示的行为列表：今日记录 + 所有待办 */
    @Query(
        """
        SELECT * FROM behaviors
        WHERE (startTime >= :dayStart AND startTime < :dayEnd)
           OR status = 'pending'
        ORDER BY startTime ASC
        """
    )
    fun getHomeBehaviors(dayStart: Long, dayEnd: Long): Flow<List<BehaviorEntity>>

    /** 获取排序最靠前的待办行为 */
    @Query("SELECT * FROM behaviors WHERE status = 'pending' ORDER BY sequence ASC LIMIT 1")
    suspend fun getNextPending(): BehaviorEntity?

    @Query("SELECT COALESCE(MAX(sequence), -1) FROM behaviors")
    suspend fun getMaxSequence(): Int

    @Query("SELECT * FROM behaviors WHERE id = :id")
    suspend fun getById(id: Long): BehaviorEntity?

    /** 获取所有待办行为（按排序） */
    @Query("SELECT * FROM behaviors WHERE status = 'pending' ORDER BY sequence ASC")
    fun getPendingBehaviors(): Flow<List<BehaviorEntity>>

    // 统计查询
    @Query("SELECT COUNT(*) FROM behaviors WHERE activityId = :activityId AND status = 'completed'")
    fun getUsageCount(activityId: Long): Flow<Int>

    /** 计算活动总耗时（取 actualDuration，没有则用 endTime - startTime 推算） */
    @Query("SELECT SUM(COALESCE(actualDuration, (endTime - startTime))) FROM behaviors WHERE activityId = :activityId AND status = 'completed'")
    fun getTotalDurationMs(activityId: Long): Flow<Long?>

    @Query("SELECT MAX(startTime) FROM behaviors WHERE activityId = :activityId AND status != 'pending'")
    fun getLastUsedTimestamp(activityId: Long): Flow<Long?>

    // 标签关联操作
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRef(crossRef: BehaviorTagCrossRefEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRefs(crossRefs: List<BehaviorTagCrossRefEntity>)

    /** 移除行为与指定标签的关联 */
    @Query(
        """
        DELETE FROM behavior_tag_cross_ref
        WHERE behaviorId = :behaviorId AND tagId IN (:tagIds)
        """
    )
    suspend fun removeTagCrossRefs(behaviorId: Long, tagIds: List<Long>)

    /** 流式查询行为关联的标签 */
    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN behavior_tag_cross_ref btc ON t.id = btc.tagId
        WHERE btc.behaviorId = :behaviorId
        ORDER BY t.priority DESC, t.name
        """
    )
    fun getTagsForBehavior(behaviorId: Long): Flow<List<TagEntity>>

    /** 批量更新行为字段 */
    @Query(
        """
        UPDATE behaviors
        SET activityId = :activityId,
            startTime = :startTime,
            endTime = :endTime,
            status = :status,
            note = :note
        WHERE id = :id
        """
    )
    suspend fun update(
        id: Long,
        activityId: Long,
        startTime: Long,
        endTime: Long?,
        status: String,
        note: String?,
    )

    /** 删除行为的所有标签关联 */
    @Query("DELETE FROM behavior_tag_cross_ref WHERE behaviorId = :behaviorId")
    suspend fun deleteTagsForBehavior(behaviorId: Long)

    /** 同步查询行为关联的标签 */
    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN behavior_tag_cross_ref btc ON t.id = btc.tagId
        WHERE btc.behaviorId = :behaviorId
        ORDER BY t.priority DESC, t.name
        """
    )
    suspend fun getTagsForBehaviorSync(behaviorId: Long): List<TagEntity>

    /** 批量同步查询多个行为关联的标签 */
    @Query(
        """
        SELECT btc.behaviorId, t.id, t.name, t.color, t.iconKey, t.category,
               t.priority, t.usageCount, t.sortOrder, t.keywords, t.isArchived, t.archivedAt
        FROM tags t
        INNER JOIN behavior_tag_cross_ref btc ON t.id = btc.tagId
        WHERE btc.behaviorId IN (:behaviorIds)
        ORDER BY t.priority DESC, t.name
        """
    )
    suspend fun getTagsForBehaviorsSync(behaviorIds: List<Long>): List<BehaviorTagRow>

    @Query("DELETE FROM behaviors WHERE activityId = :activityId")
    suspend fun deleteByActivityId(activityId: Long)

    @Query("DELETE FROM behavior_tag_cross_ref WHERE behaviorId IN (SELECT id FROM behaviors WHERE activityId = :activityId)")
    suspend fun deleteTagCrossRefsByActivityId(activityId: Long)

    @Query("DELETE FROM behaviors")
    suspend fun deleteAll()

    @Query("DELETE FROM behavior_tag_cross_ref")
    suspend fun deleteAllTagCrossRefs()

    @Query("DELETE FROM activity_tag_binding")
    suspend fun deleteAllActivityTagBindings()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>)

    @Query("SELECT * FROM behavior_tag_cross_ref")
    suspend fun getAllCrossRefsSync(): List<BehaviorTagCrossRefEntity>

    @Query("SELECT * FROM activity_tag_binding")
    suspend fun getAllActivityTagBindingsSync(): List<ActivityTagBindingEntity>

    /**
     * 查询与指定时间范围重叠的行为记录
     *
     * 重叠判断条件（半开区间）：
     * - 已有行为的 startTime < 查询范围的 end
     * - 已有行为的 endTime 为 NULL（ACTIVE 状态）或 endTime >= 查询范围的 start
     * - 排除 PENDING 状态和无效 startTime
     */
    @Query(
        """
        SELECT * FROM behaviors
        WHERE startTime < :rangeEnd
          AND (
              endTime IS NULL
              OR endTime >= :rangeStart
          )
          AND status != 'pending'
          AND startTime > 0
        """
    )
    fun getBehaviorsOverlappingRange(
        rangeStart: Long,
        rangeEnd: Long,
    ): Flow<List<BehaviorEntity>>

    /** 按时间重叠范围查询行为（流式，用于行为管理 UI 观察） */
    @Query(
        """
        SELECT * FROM behaviors
        WHERE startTime < :rangeEnd
          AND (
              endTime IS NULL
              OR endTime > :rangeStart
          )
          AND status != 'pending'
          AND startTime > 0
        ORDER BY startTime ASC
        """
    )
    fun getByOverlappingTimeRange(rangeStart: Long, rangeEnd: Long): Flow<List<BehaviorEntity>>

    @Query(
        """
        SELECT COUNT(*) as usageCount,
               COALESCE(SUM(COALESCE(actualDuration, (endTime - startTime))), 0) / 60000 as totalDurationMinutes,
               MAX(CASE WHEN status != 'pending' THEN startTime END) as lastUsedTimestamp
        FROM behaviors WHERE activityId = :activityId AND status = 'completed'
        """
    )
    fun getActivityStatsSync(activityId: Long): Flow<ActivityStatsRow>

    /** 按时间范围查询行为（流式，用于 UI 观察） */
    @Query(
        """
        SELECT * FROM behaviors
        WHERE startTime >= :startTime AND startTime < :endTime
        ORDER BY startTime ASC
        """
    )
    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorEntity>>

    /** 按时间范围同步查询行为（用于导出） */
    @Query(
        """
        SELECT * FROM behaviors
        WHERE startTime >= :startTime AND startTime < :endTime
        ORDER BY startTime ASC
        """
    )
    suspend fun getByTimeRangeSync(startTime: Long, endTime: Long): List<BehaviorEntity>

    /** 计算所有已完成行为的总耗时（毫秒） */
    @Query("SELECT COALESCE(SUM(COALESCE(actualDuration, (endTime - startTime))), 0) FROM behaviors WHERE status = 'completed'")
    fun getTotalDurationAllBehaviors(): Flow<Long>

    /** 查询数据库中最早一条 behavior 的 startTime（毫秒），无数据返回 null */
    @Query("SELECT MIN(startTime) FROM behaviors WHERE startTime > 0")
    suspend fun getEarliestStartTime(): Long?

    @Query(
        """
        SELECT a.id, MAX(b.startTime) as lastUsedTimestamp
        FROM activities a
        LEFT JOIN behaviors b ON a.id = b.activityId AND b.status = 'completed'
        GROUP BY a.id
        """
    )
    fun getAllActivityLastUsed(): Flow<List<LastUsedRow>>

    @Query(
        """
        SELECT t.id, MAX(b.startTime) as lastUsedTimestamp
        FROM tags t
        LEFT JOIN behavior_tag_cross_ref btc ON t.id = btc.tagId
        LEFT JOIN behaviors b ON btc.behaviorId = b.id AND b.status = 'completed'
        GROUP BY t.id
        """
    )
    fun getAllTagLastUsed(): Flow<List<LastUsedRow>>
}
