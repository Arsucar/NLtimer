package com.nltimer.core.data.repository.impl

import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.BehaviorEventDao
import com.nltimer.core.data.database.dao.BehaviorEventValueDao
import com.nltimer.core.data.database.entity.BehaviorEventEntity
import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventSummary
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventQueryScope
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.mapList
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BehaviorEventRepositoryImpl 行为事件仓库实现
 * 事件 + 字段值（EAV）的写入收敛在 withTransaction 事务内（事务方法均返回 Unit，
 * 保证 MockK JVM 测试可放行）；addEvent 用外部变量承接 insert 返回的 id。
 */
@Singleton
class BehaviorEventRepositoryImpl @Inject constructor(
    private val eventDao: BehaviorEventDao,
    private val valueDao: BehaviorEventValueDao,
    private val clockService: ClockService,
    private val database: NLtimerDatabase,
) : BehaviorEventRepository {

    private companion object {
        const val TAG = "BehaviorEventRepository"
    }

    override fun observeEvents(scope: EventQueryScope): Flow<List<BehaviorEvent>> =
        observeEntities(scope).mapList { BehaviorEvent.fromEntity(it) }

    override fun observeEventsWithValues(scope: EventQueryScope): Flow<List<BehaviorEventWithValues>> =
        observeEntities(scope).map { entities ->
            assembleEventsWithValues(entities)
        }.catch { e ->
            if (e is CancellationException) throw e
            android.util.Log.e(TAG, "Failed to load events with values", e)
            emit(emptyList())
        }

    override suspend fun getEventById(id: Long): BehaviorEvent? =
        eventDao.getById(id)?.let { BehaviorEvent.fromEntity(it) }

    override suspend fun getEventWithValues(id: Long): BehaviorEventWithValues? {
        val entity = eventDao.getById(id) ?: return null
        val values = valueDao.getByEventSync(id).map { BehaviorEventValue.fromEntity(it) }
        return BehaviorEventWithValues(event = BehaviorEvent.fromEntity(entity), values = values)
    }

    override fun observeLatestEventByBehavior(behaviorId: Long): Flow<BehaviorEvent?> =
        eventDao.observeLatestByBehavior(behaviorId).map { entity ->
            entity?.let { BehaviorEvent.fromEntity(it) }
        }

    override fun observeLatestEventWithValuesByBehavior(behaviorId: Long): Flow<BehaviorEventWithValues?> =
        eventDao.observeLatestByBehavior(behaviorId).map { entity ->
            entity?.let { e ->
                val values = valueDao.getByEventSync(e.id).map { BehaviorEventValue.fromEntity(it) }
                BehaviorEventWithValues(event = BehaviorEvent.fromEntity(e), values = values)
            }
        }

    override fun observeEventCountByBehavior(behaviorId: Long): Flow<Int> =
        eventDao.observeEventCountByBehavior(behaviorId)

    override fun observeSummariesForBehaviors(behaviorIds: List<Long>): Flow<Map<Long, BehaviorEventSummary>> =
        eventDao.getSummariesForBehaviors(behaviorIds).map { rows ->
            rows.associate { row ->
                row.behaviorId to BehaviorEventSummary(
                    behaviorId = row.behaviorId,
                    eventCount = row.eventCount,
                    latestTimestamp = row.latestTimestamp,
                )
            }
        }

    override fun observeEventsByOptionValue(fieldId: Long, optionText: String): Flow<List<BehaviorEvent>> =
        eventDao.observeEventsByOptionValue(fieldId, optionText).mapList { BehaviorEvent.fromEntity(it) }

    override fun observeEventsByNumberRange(fieldId: Long, min: Double, max: Double): Flow<List<BehaviorEvent>> =
        eventDao.observeEventsByNumberRange(fieldId, min, max).mapList { BehaviorEvent.fromEntity(it) }

    override fun observeEventsByTextLike(fieldId: Long, query: String): Flow<List<BehaviorEvent>> =
        eventDao.observeEventsByTextLike(fieldId, query).mapList { BehaviorEvent.fromEntity(it) }

    override suspend fun addEvent(event: BehaviorEvent, values: List<BehaviorEventValue>): Long {
        var eventId = 0L
        database.withTransaction {
            eventId = eventDao.insert(event.toEntity())
            if (values.isNotEmpty()) {
                valueDao.insertAll(values.map { it.toEntity().copy(eventId = eventId, id = 0) })
            }
        }
        return eventId
    }

    override suspend fun saveEventValues(eventId: Long, values: List<BehaviorEventValue>) {
        database.withTransaction {
            valueDao.deleteByEvent(eventId)
            if (values.isNotEmpty()) {
                valueDao.insertAll(values.map { it.toEntity().copy(eventId = eventId, id = 0) })
            }
        }
    }

    override suspend fun updateEvent(event: BehaviorEvent, values: List<BehaviorEventValue>) {
        database.withTransaction {
            eventDao.update(event.toEntity())
            valueDao.deleteByEvent(event.id)
            if (values.isNotEmpty()) {
                valueDao.insertAll(values.map { it.toEntity().copy(eventId = event.id, id = 0) })
            }
        }
    }

    override suspend fun deleteEvent(id: Long) = eventDao.delete(id)

    override suspend fun setEventBehaviorId(eventId: Long, behaviorId: Long?) =
        eventDao.setBehaviorId(eventId, behaviorId, clockService.currentTimeMillis())

    /** 按查询范围分发到对应 DAO 观察查询 */
    private fun observeEntities(scope: EventQueryScope): Flow<List<BehaviorEventEntity>> = when (scope) {
        is EventQueryScope.All -> eventDao.observeAllEvents()
        is EventQueryScope.ByActivity -> eventDao.observeByActivity(scope.activityId)
        is EventQueryScope.ByTag -> eventDao.observeByTag(scope.tagId)
        is EventQueryScope.ByBehavior -> eventDao.observeByBehavior(scope.behaviorId)
        is EventQueryScope.ByTemplate -> eventDao.observeByTemplate(scope.templateId)
    }

    /** 批量组装事件详情：批量 IN 查询字段值后按 eventId 分组 */
    private suspend fun assembleEventsWithValues(entities: List<BehaviorEventEntity>): List<BehaviorEventWithValues> {
        if (entities.isEmpty()) return emptyList()
        val eventIds = entities.map { it.id }
        val valuesMap = valueDao.getByEventIdsSync(eventIds)
            .groupBy { it.eventId }
            .mapValues { (_, rows) -> rows.map { BehaviorEventValue.fromEntity(it) } }
        return entities.map { entity ->
            BehaviorEventWithValues(
                event = BehaviorEvent.fromEntity(entity),
                values = valuesMap[entity.id] ?: emptyList(),
            )
        }
    }
}
