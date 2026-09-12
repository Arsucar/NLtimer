package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.BehaviorEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 行为事件聚合摘要扁平结果行（最新一条摘要 / 计数徽标用）
 * behaviorId 参与分组，null（独立事件）不入此结果
 */
data class BehaviorEventSummaryRow(
    val behaviorId: Long,
    val eventCount: Int,
    val latestTimestamp: Long?,
)

/**
 * BehaviorEventDao 行为事件数据访问对象
 * 提供 behavior_event 表的观察查询（全量/活动/行为/标签/模板）、字段值筛选 +
 * 基础 CRUD。标签查询为 behavior_event 经 behavior_tag_cross_ref 的反向 JOIN（新写）。
 * 事务统一在仓库层 database.withTransaction {} 完成，本 DAO 不写 @Transaction。
 */
@Dao
interface BehaviorEventDao {
    // ---- 观察查询 ----

    /** 全部事件混排（timestamp 倒序） */
    @Query("SELECT * FROM behavior_event ORDER BY `timestamp` DESC")
    fun observeAllEvents(): Flow<List<BehaviorEventEntity>>

    @Query("SELECT * FROM behavior_event WHERE activityId = :activityId ORDER BY `timestamp` DESC")
    fun observeByActivity(activityId: Long): Flow<List<BehaviorEventEntity>>

    @Query("SELECT * FROM behavior_event WHERE behaviorId = :behaviorId ORDER BY `timestamp` DESC")
    fun observeByBehavior(behaviorId: Long): Flow<List<BehaviorEventEntity>>

    @Query("SELECT * FROM behavior_event WHERE templateId = :templateId ORDER BY `timestamp` DESC")
    fun observeByTemplate(templateId: Long): Flow<List<BehaviorEventEntity>>

    /**
     * 按标签查询事件
     * 反向 JOIN（新写）：behavior_event 经 behavior_tag_cross_ref 关联 tags，
     * 仅覆盖挂接在行为上的事件（独立事件无标签语义，不参与此查询）
     */
    @Query(
        """
        SELECT e.* FROM behavior_event e
        INNER JOIN behavior_tag_cross_ref btc ON btc.behaviorId = e.behaviorId
        WHERE btc.tagId = :tagId
        ORDER BY e.`timestamp` DESC
        """
    )
    fun observeByTag(tagId: Long): Flow<List<BehaviorEventEntity>>

    // ---- 字段值筛选查询（EAV EXISTS 子查询） ----

    /** 单选字段选项等值筛选 */
    @Query(
        """
        SELECT e.* FROM behavior_event e
        WHERE EXISTS (
            SELECT 1 FROM behavior_event_value v
            WHERE v.eventId = e.id AND v.fieldId = :fieldId AND v.valueText = :optionText
        )
        ORDER BY e.`timestamp` DESC
        """
    )
    fun observeEventsByOptionValue(fieldId: Long, optionText: String): Flow<List<BehaviorEventEntity>>

    /** 数值/星级字段范围筛选（闭区间 BETWEEN） */
    @Query(
        """
        SELECT e.* FROM behavior_event e
        WHERE EXISTS (
            SELECT 1 FROM behavior_event_value v
            WHERE v.eventId = e.id AND v.fieldId = :fieldId AND v.valueNumber BETWEEN :min AND :max
        )
        ORDER BY e.`timestamp` DESC
        """
    )
    fun observeEventsByNumberRange(fieldId: Long, min: Double, max: Double): Flow<List<BehaviorEventEntity>>

    /** 文本字段模糊筛选（LIKE，大小写不敏感，无转义处理） */
    @Query(
        """
        SELECT e.* FROM behavior_event e
        WHERE EXISTS (
            SELECT 1 FROM behavior_event_value v
            WHERE v.eventId = e.id AND v.fieldId = :fieldId AND v.valueText LIKE '%' || :query || '%'
        )
        ORDER BY e.`timestamp` DESC
        """
    )
    fun observeEventsByTextLike(fieldId: Long, query: String): Flow<List<BehaviorEventEntity>>

    // ---- 基础 CRUD ----

    @Insert
    suspend fun insert(event: BehaviorEventEntity): Long

    @Update
    suspend fun update(event: BehaviorEventEntity)

    @Query("DELETE FROM behavior_event WHERE id = :id")
    suspend fun delete(id: Long)

    /** 转独立事件 / 重新挂接行为（ConvertEventToIndependentUseCase / 删除行为保留事件） */
    @Query("UPDATE behavior_event SET behaviorId = :behaviorId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setBehaviorId(id: Long, behaviorId: Long?, updatedAt: Long)

    @Query("UPDATE behavior_event SET behaviorId = NULL, updatedAt = :updatedAt WHERE behaviorId = :behaviorId")
    suspend fun detachFromBehavior(behaviorId: Long, updatedAt: Long)

    @Query("SELECT * FROM behavior_event WHERE id = :id")
    suspend fun getById(id: Long): BehaviorEventEntity?

    /** 行为的最新一条事件（活跃卡片摘要行用） */
    @Query("SELECT * FROM behavior_event WHERE behaviorId = :behaviorId ORDER BY `timestamp` DESC LIMIT 1")
    suspend fun getLatestByBehavior(behaviorId: Long): BehaviorEventEntity?

    /** 行为的最新一条事件（流式） */
    @Query("SELECT * FROM behavior_event WHERE behaviorId = :behaviorId ORDER BY `timestamp` DESC LIMIT 1")
    fun observeLatestByBehavior(behaviorId: Long): Flow<BehaviorEventEntity?>

    @Query("SELECT COUNT(*) FROM behavior_event WHERE behaviorId = :behaviorId")
    suspend fun countByBehavior(behaviorId: Long): Int

    /** 行为的事件计数（流式徽标） */
    @Query("SELECT COUNT(*) FROM behavior_event WHERE behaviorId = :behaviorId")
    fun observeEventCountByBehavior(behaviorId: Long): Flow<Int>

    /** 批量查询行为的事件计数与最新时间（首页活跃卡徽标用） */
    @Query(
        """
        SELECT e.behaviorId AS behaviorId, COUNT(*) AS eventCount, MAX(e.`timestamp`) AS latestTimestamp
        FROM behavior_event e
        WHERE e.behaviorId IN (:behaviorIds)
        GROUP BY e.behaviorId
        """
    )
    fun getSummariesForBehaviors(behaviorIds: List<Long>): Flow<List<BehaviorEventSummaryRow>>
}
