package com.nltimer.core.data.repository

import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.dao.ActivityStatsRow
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.BehaviorTagRow
import com.nltimer.core.data.database.dao.LastUsedRow
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
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
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.repository.impl.ActivityManagementRepositoryImpl
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
class ActivityManagementRepositoryImplTest {

    private lateinit var fakeActivityDao: FakeActivityDao
    private lateinit var fakeGroupDao: FakeGroupDao
    private lateinit var fakeBehaviorDao: FakeBehaviorDao
    private lateinit var fakeDatabase: NLtimerDatabase
    private lateinit var repository: ActivityManagementRepositoryImpl

    @Before
    fun setup() {
        fakeActivityDao = FakeActivityDao()
        fakeGroupDao = FakeGroupDao()
        fakeBehaviorDao = FakeBehaviorDao()
        fakeDatabase = mockk<NLtimerDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { fakeDatabase.withTransaction(any<suspend () -> Any>()) } coAnswers {
            (args[0] as suspend () -> Any).invoke()
        }
        repository = ActivityManagementRepositoryImpl(fakeActivityDao, fakeGroupDao, fakeBehaviorDao, fakeDatabase)
    }

    // --- getAllActivities ---

    @Test
    fun `getAllActivities returns only non-archived`() = runTest {
        fakeActivityDao.activities.add(ActivityEntity(id = 1, name = "活动A", isArchived = false))
        fakeActivityDao.activities.add(ActivityEntity(id = 2, name = "活动B", isArchived = true))

        val result = repository.getAllActivities().first()

        assertEquals(1, result.size)
        assertEquals("活动A", result[0].name)
    }

    // --- getUncategorizedActivities ---

    @Test
    fun `getUncategorizedActivities returns uncategorized`() = runTest {
        fakeActivityDao.uncategorizedActivities.add(ActivityEntity(id = 1, name = "无分组"))

        val result = repository.getUncategorizedActivities().first()

        assertEquals(1, result.size)
    }

    // --- getActivitiesByGroup ---

    @Test
    fun `getActivitiesByGroup returns group members`() = runTest {
        fakeActivityDao.groupActivities.add(ActivityEntity(id = 1, name = "活动A", groupId = 5L))

        val result = repository.getActivitiesByGroup(5L).first()

        assertEquals(1, result.size)
    }

    // --- getAllGroups ---

    @Test
    fun `getAllGroups returns mapped groups`() = runTest {
        fakeGroupDao.groups.add(ActivityGroupEntity(id = 1, name = "分组A", sortOrder = 0))

        val result = repository.getAllGroups().first()

        assertEquals(1, result.size)
        assertEquals("分组A", result[0].name)
    }

    // --- getActivityStats ---

    @Test
    fun `getActivityStats combines three flows`() = runTest {
        fakeBehaviorDao.usageCount = 5
        fakeBehaviorDao.totalDurationMs = 120_000L
        fakeBehaviorDao.lastUsedTimestamp = 1000L

        val result = repository.getActivityStats(1L).first()

        assertEquals(5, result.usageCount)
        assertEquals(2L, result.totalDurationMinutes)
        assertEquals(1000L, result.lastUsedTimestamp)
    }

    @Test
    fun `getActivityStats handles null duration`() = runTest {
        fakeBehaviorDao.usageCount = 0
        fakeBehaviorDao.totalDurationMs = null
        fakeBehaviorDao.lastUsedTimestamp = null

        val result = repository.getActivityStats(1L).first()

        assertEquals(0, result.usageCount)
        assertEquals(0L, result.totalDurationMinutes)
        assertNull(result.lastUsedTimestamp)
    }

    // --- addActivity ---

    @Test
    fun `addActivity inserts and returns id`() = runTest {
        val activity = Activity(id = 0, name = "新活动", iconKey = "icon")

        val id = repository.addActivity(activity)

        assertEquals(1L, id)
        assertEquals(1, fakeActivityDao.insertedActivities.size)
        assertEquals("新活动", fakeActivityDao.insertedActivities[0].name)
    }

    // --- updateActivity ---

    @Test
    fun `updateActivity delegates to dao`() = runTest {
        val activity = Activity(id = 1, name = "更新活动")

        repository.updateActivity(activity)

        assertEquals(1, fakeActivityDao.updatedActivities.size)
    }

    // --- deleteActivity ---

    @Ignore("MockK cannot mock Room's inline withTransaction extension function; use instrumented test")
    @Test
    fun `deleteActivity delegates to dao`() = runTest {
        repository.deleteActivity(1L)
        assertTrue(fakeActivityDao.deletedIds.contains(1L))
    }

    @Ignore("MockK cannot mock Room's inline withTransaction extension function; use instrumented test")
    @Test
    fun `deleteActivity deletes behavior tag cross refs and behaviors before activity`() = runTest {
        repository.deleteActivity(1L)
        assertTrue(fakeBehaviorDao.deletedByActivityIds.contains(1L))
        assertTrue(fakeBehaviorDao.deletedTagCrossRefsByActivityIds.contains(1L))
        assertTrue(fakeActivityDao.deletedActivityTagBindingIds.contains(1L))
        assertTrue(fakeActivityDao.deletedIds.contains(1L))
    }

    // --- moveActivityToGroup ---

    @Test
    fun `moveActivityToGroup delegates to dao`() = runTest {
        repository.moveActivityToGroup(1L, 5L)
        assertEquals(5L, fakeActivityDao.movedToGroup[1L])
    }

    @Test
    fun `moveActivityToGroup with null group unsets group`() = runTest {
        repository.moveActivityToGroup(1L, null)
        assertNull(fakeActivityDao.movedToGroup[1L])
    }

    // --- addGroup ---

    @Test
    fun `addGroup calculates max sort order and inserts`() = runTest {
        fakeGroupDao.groups.add(ActivityGroupEntity(id = 1, name = "已有分组", sortOrder = 3))

        val id = repository.addGroup("新分组")

        assertEquals(2L, id)
        assertEquals(1, fakeGroupDao.insertedGroups.size)
        assertEquals(4, fakeGroupDao.insertedGroups[0].sortOrder)
    }

    @Test
    fun `addGroup with no existing groups starts at sort order 0`() = runTest {
        repository.addGroup("首个分组")

        assertEquals(0, fakeGroupDao.insertedGroups[0].sortOrder)
    }

    // --- renameGroup ---

    @Test
    fun `renameGroup updates group name`() = runTest {
        fakeGroupDao.groups.add(ActivityGroupEntity(id = 1, name = "旧名称", sortOrder = 0))

        repository.renameGroup(1L, "新名称")

        assertEquals(1, fakeGroupDao.updatedGroups.size)
        assertEquals("新名称", fakeGroupDao.updatedGroups[0].name)
    }

    @Test
    fun `renameGroup does nothing when group not found`() = runTest {
        repository.renameGroup(999L, "新名称")
        assertTrue(fakeGroupDao.updatedGroups.isEmpty())
    }

    // --- deleteGroup ---

    @Ignore("MockK cannot mock Room's inline withTransaction extension function; use instrumented test")
    @Test
    fun `deleteGroup ungroups activities and deletes group`() = runTest {
        fakeGroupDao.groups.add(ActivityGroupEntity(id = 1, name = "要删除", sortOrder = 0))

        repository.deleteGroup(1L)

        assertTrue(fakeGroupDao.ungroupedGroupIds.contains(1L))
        assertEquals(1, fakeGroupDao.deletedGroups.size)
    }

    @Ignore("MockK cannot mock Room's inline withTransaction extension function; use instrumented test")
    @Test
    fun `deleteGroup does not delete entity when group not found`() = runTest {
        repository.deleteGroup(999L)
        // Note: ungroupAllActivities is called unconditionally in current impl
        assertTrue(fakeGroupDao.deletedGroups.isEmpty())
    }

    // --- initializePresets ---

    @Test
    fun `initializePresets inserts preset activities when none exist`() = runTest {
        repository.initializePresets()

        assertEquals(ActivityManagementRepositoryImpl.PRESET_ACTIVITIES.size, fakeActivityDao.insertedActivities.size)
        assertTrue(fakeActivityDao.insertedActivities.all { it.isPreset })
    }

    @Test
    fun `initializePresets does nothing when presets already exist`() = runTest {
        fakeActivityDao.presetActivities.add(ActivityEntity(id = 1, name = "已有预设", isPreset = true))

        repository.initializePresets()

        assertTrue(fakeActivityDao.insertedActivities.isEmpty())
    }

    @Test
    fun `preset activities have correct icons`() = runTest {
        val presets = ActivityManagementRepositoryImpl.PRESET_ACTIVITIES
        assertTrue(presets.all { it.iconKey != null })
        assertTrue(presets.all { it.isPreset })
        assertEquals(8, presets.size)
    }

    // --- Fake DAOs ---

    private class FakeActivityDao : ActivityDao {
        val activities = mutableListOf<ActivityEntity>()
        val uncategorizedActivities = mutableListOf<ActivityEntity>()
        val groupActivities = mutableListOf<ActivityEntity>()
        val presetActivities = mutableListOf<ActivityEntity>()
        val insertedActivities = mutableListOf<ActivityEntity>()
        val updatedActivities = mutableListOf<ActivityEntity>()
        val deletedIds = mutableListOf<Long>()
        val deletedActivityTagBindingIds = mutableListOf<Long>()
        val movedToGroup = mutableMapOf<Long, Long?>()

        private val activityFlow = MutableStateFlow<List<ActivityEntity>>(emptyList())

        override suspend fun insert(activity: ActivityEntity): Long {
            val id = if (activity.id == 0L) (insertedActivities.size + 1).toLong() else activity.id
            insertedActivities.add(activity.copy(id = id))
            return id
        }

        override suspend fun update(activity: ActivityEntity) {
            updatedActivities.add(activity)
        }

        override suspend fun delete(activity: ActivityEntity) {}
        override fun getAllActive(): Flow<List<ActivityEntity>> =
            MutableStateFlow(activities.filter { !it.isArchived })
        override fun getAll(): Flow<List<ActivityEntity>> = activityFlow
        override suspend fun getById(id: Long): ActivityEntity? = activities.find { it.id == id }
        override suspend fun getByName(name: String): ActivityEntity? = null
        override suspend fun setArchived(id: Long, archived: Boolean) {}
        override fun search(query: String): Flow<List<ActivityEntity>> = flowOf(emptyList())
        override fun getUncategorized(): Flow<List<ActivityEntity>> = MutableStateFlow(uncategorizedActivities)
        override fun getByGroup(groupId: Long): Flow<List<ActivityEntity>> = MutableStateFlow(groupActivities)
        override fun getAllPresets(): Flow<List<ActivityEntity>> = MutableStateFlow(presetActivities)
        override suspend fun moveToGroup(activityId: Long, groupId: Long?) {
            movedToGroup[activityId] = groupId
        }
        override suspend fun deleteById(id: Long) { deletedIds.add(id) }
        override suspend fun deleteAll() {}
        override suspend fun getAllPresetsSync(): List<ActivityEntity> = presetActivities
        override suspend fun getTagIdsForActivitySync(activityId: Long): List<Long> = emptyList()
        override suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity) {}
        override suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>) {}
        override suspend fun deleteActivityTagBindingsForActivity(activityId: Long) { deletedActivityTagBindingIds.add(activityId) }
        override suspend fun getAllActiveSync(): List<ActivityEntity> = activities.filter { !it.isArchived }
        override suspend fun insertAll(activities: List<ActivityEntity>) {
            insertedActivities.addAll(activities)
        }
    }

    private class FakeGroupDao : ActivityGroupDao {
        val groups = mutableListOf<ActivityGroupEntity>()
        val insertedGroups = mutableListOf<ActivityGroupEntity>()
        val updatedGroups = mutableListOf<ActivityGroupEntity>()
        val deletedGroups = mutableListOf<ActivityGroupEntity>()
        val ungroupedGroupIds = mutableListOf<Long>()

        private val groupFlow = MutableStateFlow<List<ActivityGroupEntity>>(emptyList())

        override fun getAll(): Flow<List<ActivityGroupEntity>> {
            groupFlow.value = groups.toList()
            return groupFlow
        }

        override suspend fun insert(group: ActivityGroupEntity): Long {
            val id = (groups.size + insertedGroups.size + 1).toLong()
            val inserted = group.copy(id = id)
            insertedGroups.add(inserted)
            groups.add(inserted)
            groupFlow.value = groups.toList()
            return id
        }

        override suspend fun update(group: ActivityGroupEntity) {
            updatedGroups.add(group)
            groups.replaceAll { if (it.id == group.id) group else it }
        }

        override suspend fun delete(group: ActivityGroupEntity) {
            deletedGroups.add(group)
            groups.removeAll { it.id == group.id }
        }

        override suspend fun ungroupAllActivities(groupId: Long) {
            ungroupedGroupIds.add(groupId)
        }

        override suspend fun getByName(name: String): ActivityGroupEntity? = null
        override suspend fun renameByName(oldName: String, newName: String) {}
        override suspend fun deleteByName(name: String) {}
        override suspend fun deleteAll() {}
        override suspend fun getMaxSortOrder(): Int? = groups.maxOfOrNull { it.sortOrder }
        override suspend fun getById(id: Long): ActivityGroupEntity? = groups.find { it.id == id }
        override suspend fun getAllSync(): List<ActivityGroupEntity> = emptyList()
        override suspend fun updateSortOrder(id: Long, sortOrder: Int) {}
    }

    private class FakeBehaviorDao : BehaviorDao {
        var usageCount = 0
        var totalDurationMs: Long? = null
        var lastUsedTimestamp: Long? = null
        val deletedByActivityIds = mutableListOf<Long>()
        val deletedTagCrossRefsByActivityIds = mutableListOf<Long>()

        override suspend fun insert(behavior: BehaviorEntity): Long = 1
        override suspend fun setEndTime(id: Long, endTime: Long) {}
        override suspend fun setStatus(id: Long, status: String) {}
        override suspend fun setStartTime(id: Long, startTime: Long) {}
        override suspend fun setActualDuration(id: Long, duration: Long) {}
        override suspend fun setAchievementLevel(id: Long, level: Int) {}
        override suspend fun setSequence(id: Long, sequence: Int) {}
        override suspend fun setNote(id: Long, note: String?) {}
        override suspend fun delete(id: Long) {}
        override fun getByDayRange(dayStart: Long, dayEnd: Long): Flow<List<BehaviorEntity>> = flowOf(emptyList())
        override fun getCurrentBehavior(): Flow<BehaviorEntity?> = flowOf(null)
        override suspend fun endCurrentBehavior(endTime: Long) {}
        override fun getHomeBehaviors(dayStart: Long, dayEnd: Long): Flow<List<BehaviorEntity>> = flowOf(emptyList())
        override suspend fun getNextPending(): BehaviorEntity? = null
        override suspend fun getMaxSequence(): Int = -1
        override suspend fun getById(id: Long): BehaviorEntity? = null
        override fun getPendingBehaviors(): Flow<List<BehaviorEntity>> = flowOf(emptyList())
        override fun getUsageCount(activityId: Long): Flow<Int> = flowOf(usageCount)
        override fun getTotalDurationMs(activityId: Long): Flow<Long?> = flowOf(totalDurationMs)
        override fun getLastUsedTimestamp(activityId: Long): Flow<Long?> = flowOf(lastUsedTimestamp)
        override suspend fun insertTagCrossRef(crossRef: BehaviorTagCrossRefEntity) {}
        override suspend fun insertTagCrossRefs(crossRefs: List<BehaviorTagCrossRefEntity>) {}
        override suspend fun removeTagCrossRefs(behaviorId: Long, tagIds: List<Long>) {}
        override fun getTagsForBehavior(behaviorId: Long): Flow<List<TagEntity>> = flowOf(emptyList())
        override suspend fun update(id: Long, activityId: Long, startTime: Long, endTime: Long?, status: String, note: String?) {}
        override suspend fun deleteTagsForBehavior(behaviorId: Long) {}
        override suspend fun getTagsForBehaviorSync(behaviorId: Long): List<TagEntity> = emptyList()
        override suspend fun deleteByActivityId(activityId: Long) { deletedByActivityIds.add(activityId) }
        override suspend fun deleteTagCrossRefsByActivityId(activityId: Long) { deletedTagCrossRefsByActivityIds.add(activityId) }
        override suspend fun deleteAll() {}
        override suspend fun deleteAllTagCrossRefs() {}
        override suspend fun deleteAllActivityTagBindings() {}
        override suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>) {}
        override suspend fun getAllCrossRefsSync(): List<BehaviorTagCrossRefEntity> = emptyList()
        override suspend fun getAllActivityTagBindingsSync(): List<ActivityTagBindingEntity> = emptyList()
        override fun getBehaviorsOverlappingRange(rangeStart: Long, rangeEnd: Long): Flow<List<BehaviorEntity>> = flowOf(emptyList())
        override fun getByOverlappingTimeRange(rangeStart: Long, rangeEnd: Long): Flow<List<BehaviorEntity>> = flowOf(emptyList())
        override fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorEntity>> = flowOf(emptyList())
        override suspend fun getByTimeRangeSync(startTime: Long, endTime: Long): List<BehaviorEntity> = emptyList()
        override suspend fun getTagsForBehaviorsSync(behaviorIds: List<Long>): List<BehaviorTagRow> = emptyList()
        override fun getActivityStatsSync(activityId: Long): Flow<ActivityStatsRow> = flowOf(
            ActivityStatsRow(
                usageCount = usageCount,
                totalDurationMinutes = totalDurationMs?.let { it / 60000 } ?: 0L,
                lastUsedTimestamp = lastUsedTimestamp,
            )
        )
        override fun getTotalDurationAllBehaviors(): Flow<Long> = flowOf(0L)
        override suspend fun getEarliestStartTime(): Long? = null
        override fun getAllActivityLastUsed(): Flow<List<LastUsedRow>> = flowOf(emptyList())
        override fun getAllTagLastUsed(): Flow<List<LastUsedRow>> = flowOf(emptyList())
    }

}
