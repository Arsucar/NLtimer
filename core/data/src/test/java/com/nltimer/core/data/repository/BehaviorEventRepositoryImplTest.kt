package com.nltimer.core.data.repository

import androidx.room.withTransaction
import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.BehaviorEventDao
import com.nltimer.core.data.database.dao.BehaviorEventSummaryRow
import com.nltimer.core.data.database.dao.BehaviorEventValueDao
import com.nltimer.core.data.database.entity.BehaviorEventEntity
import com.nltimer.core.data.database.entity.BehaviorEventValueEntity
import com.nltimer.core.data.database.entity.BehaviorTagCrossRefEntity
import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.EventQueryScope
import com.nltimer.core.data.repository.impl.BehaviorEventRepositoryImpl
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.SystemClockService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BehaviorEventRepositoryImplTest {

    private lateinit var events: MutableList<BehaviorEventEntity>
    private lateinit var values: MutableList<BehaviorEventValueEntity>
    private lateinit var crossRefs: MutableList<BehaviorTagCrossRefEntity>
    private lateinit var fakeEventDao: FakeBehaviorEventDao
    private lateinit var fakeValueDao: FakeBehaviorEventValueDao
    private lateinit var fakeDatabase: NLtimerDatabase
    private lateinit var clockService: ClockService
    private lateinit var repository: BehaviorEventRepositoryImpl

    @Before
    fun setup() {
        events = mutableListOf()
        values = mutableListOf()
        crossRefs = mutableListOf()
        fakeEventDao = FakeBehaviorEventDao(events, values, crossRefs)
        fakeValueDao = FakeBehaviorEventValueDao(values)
        fakeDatabase = mockk<NLtimerDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { fakeDatabase.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            // args[0] = Receiver（mock 数据库本体），args[1] = 事务 block
            (args[1] as suspend () -> Unit).invoke()
        }
        clockService = SystemClockService()
        repository = BehaviorEventRepositoryImpl(fakeEventDao, fakeValueDao, clockService, fakeDatabase)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --- observeEvents ---

    @Test
    fun `observeAllEvents orders by timestamp desc`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = 1, activityId = 1, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = 1, activityId = 1, templateId = 1, timestamp = 300, createdAt = 2, updatedAt = 2))
        events.add(BehaviorEventEntity(id = 3, behaviorId = null, activityId = null, templateId = 1, timestamp = 200, createdAt = 3, updatedAt = 3))

        val result = repository.observeEvents(EventQueryScope.All).first()

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `observeEvents by activity filters`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = 1, activityId = 8, templateId = 1, timestamp = 200, createdAt = 2, updatedAt = 2))

        val result = repository.observeEvents(EventQueryScope.ByActivity(7)).first()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `observeEvents by behavior filters independent events out`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = null, activityId = 7, templateId = 1, timestamp = 200, createdAt = 2, updatedAt = 2))

        val result = repository.observeEvents(EventQueryScope.ByBehavior(1)).first()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `observeEvents by tag joins behavior tag cross ref`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = 5, activityId = 7, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = 6, activityId = 7, templateId = 1, timestamp = 200, createdAt = 2, updatedAt = 2))
        crossRefs.add(BehaviorTagCrossRefEntity(behaviorId = 5, tagId = 3))
        crossRefs.add(BehaviorTagCrossRefEntity(behaviorId = 6, tagId = 4))

        val tagged = repository.observeEvents(EventQueryScope.ByTag(3)).first()
        val other = repository.observeEvents(EventQueryScope.ByTag(4)).first()

        assertEquals(listOf(1L), tagged.map { it.id })
        assertEquals(listOf(2L), other.map { it.id })
    }

    @Test
    fun `observeEvents by template filters`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = null, activityId = null, templateId = 3, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = null, activityId = null, templateId = 4, timestamp = 200, createdAt = 2, updatedAt = 2))

        val result = repository.observeEvents(EventQueryScope.ByTemplate(3)).first()

        assertEquals(1, result.size)
        assertEquals(3L, result[0].templateId)
    }

    // --- addEvent / saveEventValues ---

    @Test
    fun `addEvent inserts event then values with event id`() = runTest {
        val eventId = repository.addEvent(
            BehaviorEvent(id = 0, behaviorId = null, activityId = 7, templateId = 1, timestamp = 100, createdAt = 100, updatedAt = 100),
            listOf(
                BehaviorEventValue(eventId = 0, fieldId = 10, valueNumber = 3.0),
                BehaviorEventValue(eventId = 0, fieldId = 11, valueText = "好"),
            ),
        )

        assertEquals(1L, eventId)
        assertEquals(1, events.size)
        assertEquals(2, values.size)
        assertTrue(values.all { it.eventId == eventId })
    }

    @Test
    fun `saveEventValues replaces previous value rows`() = runTest {
        val eventId = repository.addEvent(
            BehaviorEvent(id = 0, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 100, updatedAt = 100),
            listOf(BehaviorEventValue(fieldId = 10, valueNumber = 1.0)),
        )

        repository.saveEventValues(eventId, listOf(BehaviorEventValue(fieldId = 11, valueText = "替换")))

        assertEquals(1, values.size)
        assertEquals(11L, values.single().fieldId)
        assertEquals("替换", values.single().valueText)
        assertEquals(eventId, values.single().eventId)
    }

    // --- updateEvent / deleteEvent ---

    @Test
    fun `updateEvent writes event row and replaces values`() = runTest {
        val eventId = repository.addEvent(
            BehaviorEvent(id = 0, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 100, updatedAt = 100),
            listOf(BehaviorEventValue(fieldId = 10, valueNumber = 1.0)),
        )

        repository.updateEvent(
            BehaviorEvent(id = eventId, behaviorId = 1, activityId = 7, templateId = 2, timestamp = 500, createdAt = 100, updatedAt = 600),
            listOf(BehaviorEventValue(fieldId = 20, valueText = "改")),
        )

        val updated = events.single { it.id == eventId }
        assertEquals(2L, updated.templateId)
        assertEquals(500L, updated.timestamp)
        assertEquals(1, values.size)
        assertEquals(20L, values.single().fieldId)
        assertEquals("改", values.single().valueText)
    }

    @Test
    fun `deleteEvent removes row`() = runTest {
        val eventId = repository.addEvent(
            BehaviorEvent(id = 0, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1),
            listOf(BehaviorEventValue(fieldId = 10, valueText = "x")),
        )

        repository.deleteEvent(eventId)

        assertTrue(events.isEmpty())
        // 事件值的级联删除属数据库外键行为；Fake 不做级联，仅校验事件行删除
        assertEquals(1, values.size)
    }

    // --- setEventBehaviorId ---

    @Test
    fun `setEventBehaviorId null converts to independent`() = runTest {
        val eventId = repository.addEvent(
            BehaviorEvent(id = 0, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 100, updatedAt = 100),
            emptyList(),
        )

        repository.setEventBehaviorId(eventId, null)

        val converted = events.single { it.id == eventId }
        assertNull(converted.behaviorId)
        assertEquals(7L, converted.activityId)
        assertTrue(converted.updatedAt > converted.createdAt)
    }

    // --- assembly ---

    @Test
    fun `getEventWithValues returns event with grouped values`() = runTest {
        val eventId = repository.addEvent(
            BehaviorEvent(id = 0, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 100, updatedAt = 100),
            listOf(
                BehaviorEventValue(fieldId = 10, valueNumber = 2.0),
                BehaviorEventValue(fieldId = 11, valueText = "备注"),
            ),
        )

        val result = repository.getEventWithValues(eventId)!!

        assertEquals(1L, result.event.templateId)
        assertEquals(2, result.values.size)
        assertEquals(10L, result.values.first().fieldId)
    }

    @Test
    fun `getEventWithValues returns null when absent`() = runTest {
        assertNull(repository.getEventWithValues(42L))
    }

    @Test
    fun `observeEventsWithValues groups values by event`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = null, activityId = null, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = null, activityId = null, templateId = 1, timestamp = 200, createdAt = 2, updatedAt = 2))
        values.add(BehaviorEventValueEntity(id = 1, eventId = 1, fieldId = 10, valueNumber = 1.0))
        values.add(BehaviorEventValueEntity(id = 2, eventId = 2, fieldId = 10, valueNumber = 2.0))
        values.add(BehaviorEventValueEntity(id = 3, eventId = 2, fieldId = 11, valueText = "ok"))

        val result = repository.observeEventsWithValues(EventQueryScope.All).first()

        assertEquals(2, result.size)
        assertEquals(1, result.first { it.event.id == 1L }.values.size)
        assertEquals(2, result.first { it.event.id == 2L }.values.size)
    }

    // --- latestByBehavior ---

    @Test
    fun `observeLatestEventByBehavior returns most recent`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 300, createdAt = 2, updatedAt = 2))

        val latest = repository.observeLatestEventByBehavior(1L).first()

        assertEquals(2L, latest?.id)
    }

    // --- summaries ---

    @Test
    fun `observeSummariesForBehaviors counts per behavior`() = runTest {
        events.add(BehaviorEventEntity(id = 1, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 100, createdAt = 1, updatedAt = 1))
        events.add(BehaviorEventEntity(id = 2, behaviorId = 1, activityId = 7, templateId = 1, timestamp = 200, createdAt = 2, updatedAt = 2))
        events.add(BehaviorEventEntity(id = 3, behaviorId = null, activityId = null, templateId = 1, timestamp = 300, createdAt = 3, updatedAt = 3))

        val summaries = repository.observeSummariesForBehaviors(listOf(1L)).first()

        assertEquals(2, summaries[1L]?.eventCount)
        assertEquals(200L, summaries[1L]?.latestTimestamp)
    }
}

/** 内存版 BehaviorEventDao：标签反向 JOIN 依赖共享 crossRefs，字段值筛选依赖共享 values */
private class FakeBehaviorEventDao(
    private val events: MutableList<BehaviorEventEntity>,
    private val values: MutableList<BehaviorEventValueEntity>,
    private val crossRefs: MutableList<BehaviorTagCrossRefEntity>,
) : BehaviorEventDao {
    private var nextId = 1L

    override suspend fun insert(event: BehaviorEventEntity): Long {
        val id = if (event.id == 0L) nextId++ else event.id
        val entity = event.copy(id = id)
        events.removeAll { it.id == id }
        events.add(entity)
        return id
    }

    override suspend fun update(event: BehaviorEventEntity) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) events[index] = event
    }

    override suspend fun delete(id: Long) {
        events.removeAll { it.id == id }
    }

    override suspend fun setBehaviorId(id: Long, behaviorId: Long?, updatedAt: Long) {
        val index = events.indexOfFirst { it.id == id }
        if (index >= 0) events[index] = events[index].copy(behaviorId = behaviorId, updatedAt = updatedAt)
    }

    override suspend fun detachFromBehavior(behaviorId: Long, updatedAt: Long) {
        events.replaceAll {
            if (it.behaviorId == behaviorId) it.copy(behaviorId = null, updatedAt = updatedAt) else it
        }
    }

    override suspend fun getById(id: Long): BehaviorEventEntity? =
        events.firstOrNull { it.id == id }

    override suspend fun getLatestByBehavior(behaviorId: Long): BehaviorEventEntity? =
        events.filter { it.behaviorId == behaviorId }.maxByOrNull { it.timestamp }

    override fun observeLatestByBehavior(behaviorId: Long): Flow<BehaviorEventEntity?> =
        flowOf(events.filter { it.behaviorId == behaviorId }.maxByOrNull { it.timestamp })

    override suspend fun countByBehavior(behaviorId: Long): Int =
        events.count { it.behaviorId == behaviorId }

    override fun observeEventCountByBehavior(behaviorId: Long): Flow<Int> =
        flowOf(events.count { it.behaviorId == behaviorId })

    override fun getSummariesForBehaviors(behaviorIds: List<Long>): Flow<List<BehaviorEventSummaryRow>> =
        flowOf(
            events.filter { it.behaviorId != null && it.behaviorId in behaviorIds }
                .groupBy { it.behaviorId!! }
                .map { (behaviorId, list) ->
                    BehaviorEventSummaryRow(
                        behaviorId = behaviorId,
                        eventCount = list.size,
                        latestTimestamp = list.maxOfOrNull { it.timestamp },
                    )
                }
        )

    override fun observeAllEvents(): Flow<List<BehaviorEventEntity>> =
        flowOf(events.sortedByDescending { it.timestamp })

    override fun observeByActivity(activityId: Long): Flow<List<BehaviorEventEntity>> =
        flowOf(events.filter { it.activityId == activityId }.sortedByDescending { it.timestamp })

    override fun observeByBehavior(behaviorId: Long): Flow<List<BehaviorEventEntity>> =
        flowOf(events.filter { it.behaviorId == behaviorId }.sortedByDescending { it.timestamp })

    override fun observeByTemplate(templateId: Long): Flow<List<BehaviorEventEntity>> =
        flowOf(events.filter { it.templateId == templateId }.sortedByDescending { it.timestamp })

    override fun observeByTag(tagId: Long): Flow<List<BehaviorEventEntity>> =
        flowOf(
            events.filter { entity ->
                entity.behaviorId != null &&
                    crossRefs.any { it.behaviorId == entity.behaviorId && it.tagId == tagId }
            }.sortedByDescending { it.timestamp }
        )

    private fun hasValue(event: BehaviorEventEntity, predicate: (BehaviorEventValueEntity) -> Boolean): Boolean =
        values.any { it.eventId == event.id && predicate(it) }

    override fun observeEventsByOptionValue(fieldId: Long, optionText: String): Flow<List<BehaviorEventEntity>> =
        flowOf(
            events.filter { hasValue(it) { v -> v.fieldId == fieldId && v.valueText == optionText } }
                .sortedByDescending { it.timestamp }
        )

    override fun observeEventsByNumberRange(fieldId: Long, min: Double, max: Double): Flow<List<BehaviorEventEntity>> =
        flowOf(
            events.filter { hasValue(it) { v -> v.fieldId == fieldId && v.valueNumber != null && v.valueNumber!! >= min && v.valueNumber!! <= max } }
                .sortedByDescending { it.timestamp }
        )

    override fun observeEventsByTextLike(fieldId: Long, query: String): Flow<List<BehaviorEventEntity>> =
        flowOf(
            events.filter {
                hasValue(it) { v -> v.fieldId == fieldId && v.valueText?.contains(query, ignoreCase = true) == true }
            }.sortedByDescending { it.timestamp }
        )
}

private class FakeBehaviorEventValueDao(
    private val values: MutableList<BehaviorEventValueEntity>,
) : BehaviorEventValueDao {
    private var nextId = 1L

    override suspend fun insert(value: BehaviorEventValueEntity): Long {
        val id = if (value.id == 0L) nextId++ else value.id
        val entity = value.copy(id = id)
        values.removeAll { it.id == id }
        values.add(entity)
        return id
    }

    override suspend fun insertAll(values: List<BehaviorEventValueEntity>) {
        values.forEach { insert(it) }
    }

    override suspend fun update(value: BehaviorEventValueEntity) {
        val index = values.indexOfFirst { it.id == value.id }
        if (index >= 0) values[index] = value
    }

    override suspend fun delete(id: Long) {
        values.removeAll { it.id == id }
    }

    override suspend fun deleteByEvent(eventId: Long) {
        values.removeAll { it.eventId == eventId }
    }

    override fun observeByEvent(eventId: Long): Flow<List<BehaviorEventValueEntity>> =
        flowOf(values.filter { it.eventId == eventId }.sortedBy { it.fieldId })

    override suspend fun getByEventSync(eventId: Long): List<BehaviorEventValueEntity> =
        values.filter { it.eventId == eventId }.sortedBy { it.fieldId }

    override suspend fun getByEventIdsSync(eventIds: List<Long>): List<BehaviorEventValueEntity> =
        values.filter { it.eventId in eventIds }

    override suspend fun getByFieldIdSync(fieldId: Long): List<BehaviorEventValueEntity> =
        values.filter { it.fieldId == fieldId }

    override suspend fun getByFieldIdsSync(fieldIds: List<Long>): List<BehaviorEventValueEntity> =
        values.filter { it.fieldId in fieldIds }
}
