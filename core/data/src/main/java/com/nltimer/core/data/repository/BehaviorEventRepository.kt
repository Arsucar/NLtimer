package com.nltimer.core.data.repository

import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventSummary
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventQueryScope
import kotlinx.coroutines.flow.Flow

/**
 * BehaviorEventRepository 行为事件仓库接口
 * 提供事件的观察查询（五类范围 + 字段值筛选）、增删改与转独立事件操作
 */
interface BehaviorEventRepository {
    fun observeEvents(scope: EventQueryScope): Flow<List<BehaviorEvent>>
    fun observeEventsWithValues(scope: EventQueryScope): Flow<List<BehaviorEventWithValues>>

    suspend fun getEventById(id: Long): BehaviorEvent?
    suspend fun getEventWithValues(id: Long): BehaviorEventWithValues?

    /** 行为的最新一条事件（活跃卡片摘要行） */
    fun observeLatestEventByBehavior(behaviorId: Long): Flow<BehaviorEvent?>
    fun observeLatestEventWithValuesByBehavior(behaviorId: Long): Flow<BehaviorEventWithValues?>

    /** 行为的事件计数（实时徽标） */
    fun observeEventCountByBehavior(behaviorId: Long): Flow<Int>

    /** 批量行为事件计数与最新时间（key = behaviorId） */
    fun observeSummariesForBehaviors(behaviorIds: List<Long>): Flow<Map<Long, BehaviorEventSummary>>

    // 字段值筛选查询（单选等值 / 数值星级范围 / 文本 LIKE）
    fun observeEventsByOptionValue(fieldId: Long, optionText: String): Flow<List<BehaviorEvent>>
    fun observeEventsByNumberRange(fieldId: Long, min: Double, max: Double): Flow<List<BehaviorEvent>>
    fun observeEventsByTextLike(fieldId: Long, query: String): Flow<List<BehaviorEvent>>

    /** 新增事件并写入字段值（返回新事件 id） */
    suspend fun addEvent(event: BehaviorEvent, values: List<BehaviorEventValue> = emptyList()): Long

    /** 整体替换事件字段值（事务内先删后插，行 id 重新生成） */
    suspend fun saveEventValues(eventId: Long, values: List<BehaviorEventValue>)

    /** 更新事件本体并整体替换字段值（事务内） */
    suspend fun updateEvent(event: BehaviorEvent, values: List<BehaviorEventValue>)

    suspend fun deleteEvent(id: Long)

    /** 转独立事件（behaviorId 置空，activityId 与字段值保留）/ 重新挂接行为 */
    suspend fun setEventBehaviorId(eventId: Long, behaviorId: Long?)
}
