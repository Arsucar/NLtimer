package com.nltimer.core.data.repository

import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityStatsRow
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.BehaviorTagRow
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.BehaviorEntity
import com.nltimer.core.data.database.entity.BehaviorTagCrossRefEntity
import com.nltimer.core.data.database.entity.TagEntity
import com.nltimer.core.data.database.NLtimerDatabase
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.impl.BehaviorRepositoryImpl
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.SystemClockService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BehaviorRepositoryImplTest {

    private lateinit var fakeBehaviorDao: FakeBehaviorDao
    private lateinit var fakeActivityDao: FakeActivityDao
    private lateinit var fakeTagDao: FakeTagDao
    private lateinit var fakeDatabase: NLtimerDatabase
    private lateinit var clockService: ClockService
    private lateinit var repository: BehaviorRepositoryImpl

    @Before
    fun setup() {
        fakeBehaviorDao = FakeBehaviorDao()
        fakeActivityDao = FakeActivityDao()
        fakeTagDao = FakeTagDao()
        fakeDatabase = mockk<NLtimerDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { fakeDatabase.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            (args[0] as suspend () -> Unit).invoke()
        }
        clockService = SystemClockService()
        repository = BehaviorRepositoryImpl(fakeBehaviorDao, fakeActivityDao, fakeTagDao, clockService, fakeDatabase)
    }

    // --- getByDayRange ---

    @Test
    fun `getByDayRange returns behaviors within range`() = runTest {
        val now = System.currentTimeMillis()
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = now - 1000, endTime = now, status = "completed")
        )
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 2, activityId = 1, startTime = now + 100_000, endTime = now + 200_000, status = "completed")
        )

        val result = repository.getByDayRange(now - 5000, now + 50_000).first()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `getByDayRange returns empty when no behaviors in range`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = 1000, endTime = 2000, status = "completed")
        )

        val result = repository.getByDayRange(5000, 10_000).first()

        assertTrue(result.isEmpty())
    }

    // --- getCurrentBehavior ---

    @Test
    fun `getCurrentBehavior returns active behavior`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = 1000, status = "active")
        )

        val result = repository.getCurrentBehavior().first()

        assertNotNull(result)
        assertEquals(BehaviorNature.ACTIVE, result?.status)
    }

    @Test
    fun `getCurrentBehavior returns null when no active behavior`() = runTest {
        val result = repository.getCurrentBehavior().first()
        assertNull(result)
    }

    // --- getHomeBehaviors ---

    @Test
    fun `getHomeBehaviors returns today and pending behaviors`() = runTest {
        val now = System.currentTimeMillis()
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = now, endTime = now + 1000, status = "completed")
        )
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 2, activityId = 1, startTime = 0, status = "pending", sequence = 0)
        )

        val result = repository.getHomeBehaviors(now - 10_000, now + 10_000).first()

        assertEquals(2, result.size)
    }

    // --- getTagsForBehavior ---

    @Test
    fun `getTagsForBehavior returns mapped tags`() = runTest {
        fakeBehaviorDao.tagEntities.add(TagEntity(id = 1, name = "标签A", color = 0xFF0000, priority = 1))
        fakeBehaviorDao.tagEntities.add(TagEntity(id = 2, name = "标签B", color = 0x00FF00, priority = 2))

        val result = repository.getTagsForBehavior(1L).first()

        assertEquals(2, result.size)
        assertEquals("标签A", result[0].name)
        assertEquals("标签B", result[1].name)
    }

    // --- getPendingBehaviors ---

    @Test
    fun `getPendingBehaviors returns only pending`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = 0, status = "pending")
        )
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 2, activityId = 1, startTime = 1000, status = "completed")
        )

        val result = repository.getPendingBehaviors().first()

        assertEquals(1, result.size)
        assertEquals(BehaviorNature.PENDING, result[0].status)
    }

    // --- getBehaviorWithDetails ---

    @Test
    fun `getBehaviorWithDetails returns full details when all data exists`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 10, startTime = 1000, endTime = 2000, status = "completed")
        )
        fakeActivityDao.activities.add(
            ActivityEntity(id = 10, name = "活动A", iconKey = "icon")
        )
        fakeTagDao.tagsForBehaviorSync.add(TagEntity(id = 1, name = "标签1"))

        val result = repository.getBehaviorWithDetails(1L)

        assertNotNull(result)
        assertEquals(1L, result?.behavior?.id)
        assertEquals("活动A", result?.activity?.name)
        assertEquals(1, result?.tags?.size)
    }

    @Test
    fun `getBehaviorWithDetails returns null when behavior not found`() = runTest {
        val result = repository.getBehaviorWithDetails(999L)
        assertNull(result)
    }

    @Test
    fun `getBehaviorWithDetails returns null when activity not found`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 999, startTime = 1000, status = "completed")
        )

        val result = repository.getBehaviorWithDetails(1L)
        assertNull(result)
    }

    @Test
    fun `getBehaviorsWithDetailsOverlappingTimeRange includes behavior crossing range start`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 10, startTime = 1_000, endTime = 6_000, status = "completed")
        )
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 2, activityId = 10, startTime = 6_000, endTime = 8_000, status = "completed")
        )
        fakeActivityDao.activities.add(
            ActivityEntity(id = 10, name = "活动A", iconKey = "icon")
        )

        val result = repository.getBehaviorsWithDetailsOverlappingTimeRange(5_000, 6_000).first()

        assertEquals(1, result.size)
        assertEquals(1L, result.first().behavior.id)
    }

    // --- insert ---

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `insert creates behavior and returns id`() = runTest {
        val behavior = createBehavior(startTime = 1000, status = BehaviorNature.COMPLETED)

        val id = repository.insert(behavior, emptyList())

        assertEquals(1L, id)
        assertEquals(1, fakeBehaviorDao.insertedEntities.size)
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `insert with tag ids creates cross refs`() = runTest {
        val behavior = createBehavior(startTime = 1000)

        repository.insert(behavior, listOf(10L, 20L))

        assertEquals(2, fakeBehaviorDao.insertedCrossRefs.size)
        assertEquals(10L, fakeBehaviorDao.insertedCrossRefs[0].tagId)
        assertEquals(20L, fakeBehaviorDao.insertedCrossRefs[1].tagId)
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `insert with empty tag ids does not create cross refs`() = runTest {
        val behavior = createBehavior(startTime = 1000)

        repository.insert(behavior, emptyList())

        assertTrue(fakeBehaviorDao.insertedCrossRefs.isEmpty())
    }

    // --- completeCurrentAndStartNext ---

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext sets end time and status`() = runTest {
        val startTime = System.currentTimeMillis() - 60_000
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = startTime, status = "active")
        )

        repository.completeCurrentAndStartNext(1L, idleMode = false)

        assertEquals("completed", fakeBehaviorDao.statusUpdates[1L])
        assertNotNull(fakeBehaviorDao.endTimeUpdates[1L])
        assertNotNull(fakeBehaviorDao.actualDurations[1L])
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext with idle mode does not start next`() = runTest {
        val startTime = System.currentTimeMillis() - 60_000
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = startTime, status = "active")
        )

        val result = repository.completeCurrentAndStartNext(1L, idleMode = true)

        assertNull(result)
        assertEquals(0, fakeBehaviorDao.activatedPendingIds.size)
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext starts next pending when not idle`() = runTest {
        val startTime = System.currentTimeMillis() - 60_000
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = startTime, status = "active")
        )
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 2, activityId = 1, startTime = 0, status = "pending", sequence = 0)
        )

        val result = repository.completeCurrentAndStartNext(1L, idleMode = false)

        assertNotNull(result)
        assertEquals(2L, result?.id)
        assertEquals(2L, fakeBehaviorDao.activatedPendingIds.firstOrNull())
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext returns null when behavior not found`() = runTest {
        val result = repository.completeCurrentAndStartNext(999L, idleMode = false)
        assertNull(result)
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext calculates achievement level for planned behavior`() = runTest {
        val startTime = System.currentTimeMillis() - 60 * 60_000
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(
                id = 1, activityId = 1, startTime = startTime, status = "active",
                wasPlanned = true, estimatedDuration = 60L
            )
        )

        repository.completeCurrentAndStartNext(1L, idleMode = true)

        val level = fakeBehaviorDao.achievementLevels[1L]
        assertNotNull(level)
        assertTrue("Achievement level should be >= 90 but was $level", level!! >= 90)
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext clamps end time when before start`() = runTest {
        val futureTime = System.currentTimeMillis() + 100_000
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = futureTime, status = "active")
        )

        repository.completeCurrentAndStartNext(1L, idleMode = true)

        assertNotNull(fakeBehaviorDao.endTimeUpdates[1L])
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `completeCurrentAndStartNext returns null when no next pending`() = runTest {
        val startTime = System.currentTimeMillis() - 60_000
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = startTime, status = "active")
        )

        val result = repository.completeCurrentAndStartNext(1L, idleMode = false)

        assertNull(result)
    }

    // --- reorderGoals ---

    @Test
    fun `reorderGoals updates sequences`() = runTest {
        repository.reorderGoals(listOf(3L, 1L, 2L))

        assertEquals(0, fakeBehaviorDao.sequenceUpdates[3L])
        assertEquals(1, fakeBehaviorDao.sequenceUpdates[1L])
        assertEquals(2, fakeBehaviorDao.sequenceUpdates[2L])
    }

    @Test
    fun `reorderGoals with empty list does nothing`() = runTest {
        repository.reorderGoals(emptyList())
        assertTrue(fakeBehaviorDao.sequenceUpdates.isEmpty())
    }

    // --- delete ---

    @Test
    fun `delete removes behavior`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = 1000, status = "completed")
        )

        repository.delete(1L)

        assertTrue(fakeBehaviorDao.deletedIds.contains(1L))
    }

    @Ignore("MockK cannot mock Room's inline withTransaction; use instrumented test")
    @Test
    fun `delete does nothing when behavior not found`() = runTest {
        repository.delete(999L)
        assertTrue(fakeBehaviorDao.deletedIds.isEmpty())
    }

    // --- updateBehavior ---

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `updateBehavior recalculates duration for completed status`() = runTest {
        repository.updateBehavior(1L, 10L, 1000L, 5000L, "completed", "备注")

        assertEquals(4000L, fakeBehaviorDao.actualDurations[1L])
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `updateBehavior sets zero duration for pending status`() = runTest {
        repository.updateBehavior(1L, 10L, 0L, null, "pending", null)

        assertEquals(0L, fakeBehaviorDao.actualDurations[1L])
    }

    // --- updateTagsForBehavior ---

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `updateTagsForBehavior deletes old and inserts new`() = runTest {
        repository.updateTagsForBehavior(1L, listOf(10L, 20L, 30L))

        assertEquals(1L, fakeBehaviorDao.deletedTagsForBehaviorId)
        assertEquals(3, fakeBehaviorDao.insertedCrossRefs.size)
        assertTrue(fakeBehaviorDao.insertedCrossRefs.all { it.behaviorId == 1L })
    }

    @Ignore("MockK cannot mock Room's inline withTransaction returning non-Unit; use instrumented test")
    @Test
    fun `updateTagsForBehavior with empty list clears all tags`() = runTest {
        repository.updateTagsForBehavior(1L, emptyList())

        assertEquals(1L, fakeBehaviorDao.deletedTagsForBehaviorId)
        assertTrue(fakeBehaviorDao.insertedCrossRefs.isEmpty())
    }

    // --- entity to model mapping ---

    @Test
    fun `entity to model maps all fields correctly`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(
                id = 42, activityId = 7, startTime = 1000, endTime = 5000,
                status = "completed", note = "备注", pomodoroCount = 3,
                sequence = 5, estimatedDuration = 60L, actualDuration = 55L,
                achievementLevel = 85, wasPlanned = true
            )
        )

        val result = repository.getByDayRange(0, 10_000).first().first()

        assertEquals(42L, result.id)
        assertEquals(7L, result.activityId)
        assertEquals(1000L, result.startTime)
        assertEquals(5000L, result.endTime)
        assertEquals(BehaviorNature.COMPLETED, result.status)
        assertEquals("备注", result.note)
        assertEquals(3, result.pomodoroCount)
        assertEquals(5, result.sequence)
        assertEquals(60L, result.estimatedDuration)
        assertEquals(55L, result.actualDuration)
        assertEquals(85, result.achievementLevel)
        assertTrue(result.wasPlanned)
    }

    @Test
    fun `unknown status key maps to PENDING`() = runTest {
        fakeBehaviorDao.behaviors.add(
            BehaviorEntity(id = 1, activityId = 1, startTime = 1000, status = "unknown_status")
        )

        val result = repository.getByDayRange(0, 10_000).first().first()

        assertEquals(BehaviorNature.PENDING, result.status)
    }

    // --- getEarliestBehaviorDate ---

    @Test
    fun `getEarliestBehaviorDate returns earliest date among valid behaviors`() = runTest {
        val zone = java.time.ZoneId.systemDefault()
        val day1 = java.time.LocalDate.of(2026, 5, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val day2 = java.time.LocalDate.of(2026, 5, 12).atStartOfDay(zone).toInstant().toEpochMilli()
        fakeBehaviorDao.behaviors.add(BehaviorEntity(id = 1L, activityId = 1L, startTime = day2))
        fakeBehaviorDao.behaviors.add(BehaviorEntity(id = 2L, activityId = 1L, startTime = day1))

        val result = repository.getEarliestBehaviorDate()

        assertEquals(java.time.LocalDate.of(2026, 5, 10), result)
    }

    @Test
    fun `getEarliestBehaviorDate returns null when no valid behavior exists`() = runTest {
        val result = repository.getEarliestBehaviorDate()
        assertNull(result)
    }

    // --- Helper ---

    private fun createBehavior(
        id: Long = 0,
        startTime: Long,
        endTime: Long? = null,
        status: BehaviorNature = BehaviorNature.COMPLETED,
    ) = Behavior(
        id = id, activityId = 1, startTime = startTime, endTime = endTime,
        status = status, note = null, pomodoroCount = 0, sequence = 0,
        estimatedDuration = null, actualDuration = null, achievementLevel = null,
        wasPlanned = false,
    )

    // --- Fake DAOs ---

    private class FakeBehaviorDao : BehaviorDao {
        val behaviors = mutableListOf<BehaviorEntity>()
        val tagEntities = mutableListOf<TagEntity>()
        val insertedEntities = mutableListOf<BehaviorEntity>()
        val insertedCrossRefs = mutableListOf<BehaviorTagCrossRefEntity>()
        val statusUpdates = mutableMapOf<Long, String>()
        val endTimeUpdates = mutableMapOf<Long, Long>()
        val actualDurations = mutableMapOf<Long, Long>()
        val achievementLevels = mutableMapOf<Long, Int>()
        val sequenceUpdates = mutableMapOf<Long, Int>()
        val deletedIds = mutableListOf<Long>()
        val activatedPendingIds = mutableListOf<Long>()
        val startTimeUpdates = mutableMapOf<Long, Long>()
        var deletedTagsForBehaviorId: Long? = null

        override suspend fun insert(behavior: BehaviorEntity): Long {
            val id = if (behavior.id == 0L) (insertedEntities.size + 1).toLong() else behavior.id
            val inserted = behavior.copy(id = id)
            insertedEntities.add(inserted)
            behaviors.add(inserted)
            return id
        }

        override suspend fun setEndTime(id: Long, endTime: Long) {
            endTimeUpdates[id] = endTime
            behaviors.replaceAll { if (it.id == id) it.copy(endTime = endTime) else it }
        }

        override suspend fun setStatus(id: Long, status: String) {
            statusUpdates[id] = status
            behaviors.replaceAll { if (it.id == id) it.copy(status = status) else it }
            if (status == "active") {
                activatedPendingIds.add(id)
            }
        }

        override suspend fun setStartTime(id: Long, startTime: Long) {
            startTimeUpdates[id] = startTime
            behaviors.replaceAll { if (it.id == id) it.copy(startTime = startTime) else it }
        }

        override suspend fun setActualDuration(id: Long, duration: Long) {
            actualDurations[id] = duration
        }

        override suspend fun setAchievementLevel(id: Long, level: Int) {
            achievementLevels[id] = level
        }

        override suspend fun setSequence(id: Long, sequence: Int) {
            sequenceUpdates[id] = sequence
        }

        override suspend fun setNote(id: Long, note: String?) {}

        override suspend fun delete(id: Long) {
            deletedIds.add(id)
            behaviors.removeAll { it.id == id }
        }

        override fun getByDayRange(dayStart: Long, dayEnd: Long): Flow<List<BehaviorEntity>> =
            MutableStateFlow(behaviors.filter { it.startTime in dayStart until dayEnd })

        override fun getCurrentBehavior(): Flow<BehaviorEntity?> =
            MutableStateFlow(behaviors.find { it.status == "active" && it.endTime == null })

        override suspend fun endCurrentBehavior(endTime: Long) {
            behaviors.replaceAll {
                if (it.status == "active" && it.endTime == null) it.copy(endTime = endTime, status = "completed") else it
            }
        }

        override fun getHomeBehaviors(dayStart: Long, dayEnd: Long): Flow<List<BehaviorEntity>> =
            MutableStateFlow(behaviors.filter {
                (it.startTime in dayStart until dayEnd) || it.status == "pending"
            })

        override suspend fun getNextPending(): BehaviorEntity? =
            behaviors.filter { it.status == "pending" }.minByOrNull { it.sequence }

        override suspend fun getMaxSequence(): Int =
            behaviors.maxOfOrNull { it.sequence } ?: -1

        override suspend fun getById(id: Long): BehaviorEntity? =
            behaviors.find { it.id == id }

        override fun getPendingBehaviors(): Flow<List<BehaviorEntity>> =
            MutableStateFlow(behaviors.filter { it.status == "pending" }.sortedBy { it.sequence })

        override fun getUsageCount(activityId: Long): Flow<Int> = flowOf(0)
        override fun getTotalDurationMs(activityId: Long): Flow<Long?> = flowOf(null)
        override fun getLastUsedTimestamp(activityId: Long): Flow<Long?> = flowOf(null)

        override suspend fun insertTagCrossRef(crossRef: BehaviorTagCrossRefEntity) {
            insertedCrossRefs.add(crossRef)
        }

        override suspend fun insertTagCrossRefs(crossRefs: List<BehaviorTagCrossRefEntity>) {
            insertedCrossRefs.addAll(crossRefs)
        }

        override suspend fun removeTagCrossRefs(behaviorId: Long, tagIds: List<Long>) {}

        override fun getTagsForBehavior(behaviorId: Long): Flow<List<TagEntity>> =
            MutableStateFlow(tagEntities)

        override suspend fun update(id: Long, activityId: Long, startTime: Long, endTime: Long?, status: String, note: String?) {
            behaviors.replaceAll {
                if (it.id == id) it.copy(activityId = activityId, startTime = startTime, endTime = endTime, status = status, note = note) else it
            }
        }

        override suspend fun deleteTagsForBehavior(behaviorId: Long) {
            deletedTagsForBehaviorId = behaviorId
        }

        override suspend fun getTagsForBehaviorSync(behaviorId: Long): List<TagEntity> = tagEntities

        override suspend fun deleteByActivityId(activityId: Long) {
            behaviors.removeAll { it.activityId == activityId }
        }
        override suspend fun deleteTagCrossRefsByActivityId(activityId: Long) {
            val behaviorIds = behaviors.filter { it.activityId == activityId }.map { it.id }.toSet()
            insertedCrossRefs.removeAll { it.behaviorId in behaviorIds }
        }
        override suspend fun deleteAll() { behaviors.clear() }
        override suspend fun deleteAllTagCrossRefs() { insertedCrossRefs.clear() }
        override suspend fun deleteAllActivityTagBindings() {}
        override suspend fun insertActivityTagBindings(bindings: List<com.nltimer.core.data.database.entity.ActivityTagBindingEntity>) {}
        override suspend fun getAllCrossRefsSync(): List<BehaviorTagCrossRefEntity> = insertedCrossRefs
        override suspend fun getAllActivityTagBindingsSync(): List<com.nltimer.core.data.database.entity.ActivityTagBindingEntity> = emptyList()

        override fun getBehaviorsOverlappingRange(rangeStart: Long, rangeEnd: Long): Flow<List<BehaviorEntity>> =
            MutableStateFlow(behaviors.filter {
                it.startTime < rangeEnd && (it.endTime == null || it.endTime >= rangeStart) && it.status != "pending" && it.startTime > 0
            })

        override fun getByOverlappingTimeRange(rangeStart: Long, rangeEnd: Long): Flow<List<BehaviorEntity>> =
            MutableStateFlow(behaviors.filter {
                it.startTime < rangeEnd && (it.endTime == null || it.endTime > rangeStart) && it.status != "pending" && it.startTime > 0
            }.sortedBy { it.startTime })

        override fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorEntity>> =
            MutableStateFlow(behaviors.filter { it.startTime in startTime until endTime })
        override suspend fun getByTimeRangeSync(startTime: Long, endTime: Long): List<BehaviorEntity> =
            behaviors.filter { it.startTime in startTime until endTime }
        override suspend fun getTagsForBehaviorsSync(behaviorIds: List<Long>): List<BehaviorTagRow> = emptyList()
        override fun getActivityStatsSync(activityId: Long): Flow<ActivityStatsRow> = flowOf(ActivityStatsRow())
        override fun getTotalDurationAllBehaviors(): Flow<Long> = flowOf(0L)
        override suspend fun getEarliestStartTime(): Long? =
            behaviors.filter { it.startTime > 0 }.minOfOrNull { it.startTime }
        override fun getAllActivityLastUsed(): Flow<List<com.nltimer.core.data.database.dao.LastUsedRow>> = flowOf(emptyList())
        override fun getAllTagLastUsed(): Flow<List<com.nltimer.core.data.database.dao.LastUsedRow>> = flowOf(emptyList())
    }

    private class FakeActivityDao : ActivityDao {
        val activities = mutableListOf<ActivityEntity>()

        override suspend fun insert(activity: ActivityEntity): Long = 1
        override suspend fun update(activity: ActivityEntity) {}
        override suspend fun delete(activity: ActivityEntity) {}
        override fun getAllActive(): Flow<List<ActivityEntity>> = flowOf(activities)
        override fun getAll(): Flow<List<ActivityEntity>> = flowOf(activities)
        override suspend fun getById(id: Long): ActivityEntity? = activities.find { it.id == id }
        override suspend fun getByName(name: String): ActivityEntity? = null
        override suspend fun setArchived(id: Long, archived: Boolean) {}
        override fun search(query: String): Flow<List<ActivityEntity>> = flowOf(emptyList())
        override fun getUncategorized(): Flow<List<ActivityEntity>> = flowOf(emptyList())
        override fun getByGroup(groupId: Long): Flow<List<ActivityEntity>> = flowOf(emptyList())
        override fun getAllPresets(): Flow<List<ActivityEntity>> = flowOf(emptyList())
        override suspend fun moveToGroup(activityId: Long, groupId: Long?) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun deleteAll() {}
        override suspend fun getAllPresetsSync(): List<ActivityEntity> = emptyList()
        override suspend fun getTagIdsForActivitySync(activityId: Long): List<Long> = emptyList()
        override suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity) {}
        override suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>) {}
        override suspend fun deleteActivityTagBindingsForActivity(activityId: Long) {}
        override suspend fun getAllActiveSync(): List<ActivityEntity> = activities.filter { !it.isArchived }
        override suspend fun insertAll(activities: List<ActivityEntity>) {}
    }

    private class FakeTagDao : TagDao {
        val tagsForBehaviorSync = mutableListOf<TagEntity>()

        override suspend fun insert(tag: TagEntity): Long = 1
        override suspend fun update(tag: TagEntity) {}
        override suspend fun delete(tag: TagEntity) {}
        override fun getAllActive(): Flow<List<TagEntity>> = flowOf(emptyList())
        override fun getAll(): Flow<List<TagEntity>> = flowOf(emptyList())
        override suspend fun getById(id: Long): TagEntity? = null
        override suspend fun getByName(name: String): TagEntity? = null
        override suspend fun setArchived(id: Long, archived: Boolean) {}
        override fun search(query: String): Flow<List<TagEntity>> = flowOf(emptyList())
        override fun getByActivityId(activityId: Long): Flow<List<TagEntity>> = flowOf(emptyList())
        override fun getByCategory(category: String): Flow<List<TagEntity>> = flowOf(emptyList())
        override fun getDistinctCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun renameCategory(oldName: String, newName: String) {}
        override suspend fun resetCategory(category: String) {}
        override suspend fun getTagsForBehaviorSync(behaviorId: Long): List<TagEntity> = tagsForBehaviorSync
        override suspend fun deleteAll() {}
        override suspend fun getDistinctCategoriesSync(): List<String> = emptyList()
        override suspend fun getActivityIdsForTagSync(tagId: Long): List<Long> = emptyList()
        override suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity) {}
        override suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>) {}
        override suspend fun deleteActivityTagBindingsForTag(tagId: Long) {}
        override suspend fun getAllDistinctSync(): List<TagEntity> = emptyList()
    }

}
