package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.BehaviorEventValueEntity
import kotlinx.coroutines.flow.Flow

/**
 * BehaviorEventValueDao 行为事件字段值（EAV 行）数据访问对象
 * 提供 behavior_event_value 表的增删改查；值的整体替换语义在仓库层事务内完成
 */
@Dao
interface BehaviorEventValueDao {
    @Insert
    suspend fun insert(value: BehaviorEventValueEntity): Long

    @Insert
    suspend fun insertAll(values: List<BehaviorEventValueEntity>)

    @Update
    suspend fun update(value: BehaviorEventValueEntity)

    @Query("DELETE FROM behavior_event_value WHERE id = :id")
    suspend fun delete(id: Long)

    /** 删除事件全部字段值（更新事件时先删后插，需在仓库层事务内调用） */
    @Query("DELETE FROM behavior_event_value WHERE eventId = :eventId")
    suspend fun deleteByEvent(eventId: Long)

    @Query("SELECT * FROM behavior_event_value WHERE eventId = :eventId ORDER BY fieldId ASC")
    fun observeByEvent(eventId: Long): Flow<List<BehaviorEventValueEntity>>

    @Query("SELECT * FROM behavior_event_value WHERE eventId = :eventId ORDER BY fieldId ASC")
    suspend fun getByEventSync(eventId: Long): List<BehaviorEventValueEntity>

    /** 批量查询多个事件的字段值（详情组装用，调用方按 eventId 分组） */
    @Query("SELECT * FROM behavior_event_value WHERE eventId IN (:eventIds)")
    suspend fun getByEventIdsSync(eventIds: List<Long>): List<BehaviorEventValueEntity>

    @Query("SELECT * FROM behavior_event_value WHERE fieldId = :fieldId")
    suspend fun getByFieldIdSync(fieldId: Long): List<BehaviorEventValueEntity>

    @Query("SELECT * FROM behavior_event_value WHERE fieldId IN (:fieldIds)")
    suspend fun getByFieldIdsSync(fieldIds: List<Long>): List<BehaviorEventValueEntity>
}
