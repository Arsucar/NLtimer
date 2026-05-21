package com.nltimer.core.data.repository

import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.repository.impl.ActivityRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityRepositoryImplTest {

    private val activityEntities = mutableListOf<ActivityEntity>()
    private val groupEntities = mutableListOf<ActivityGroupEntity>()
    private val activityFlow = MutableStateFlow<List<ActivityEntity>>(emptyList())
    private val groupFlow = MutableStateFlow<List<ActivityGroupEntity>>(emptyList())

    private val fakeActivityDao = object : ActivityDao {
        override suspend fun insert(activity: ActivityEntity): Long {
            val id = activityEntities.size.toLong() + 1
            activityEntities.add(activity.copy(id = id))
            activityFlow.value = activityEntities.toList()
            return id
        }

        override suspend fun update(activity: ActivityEntity) {
            activityEntities.replaceAll { if (it.id == activity.id) activity else it }
            activityFlow.value = activityEntities.toList()
        }

        override suspend fun delete(activity: ActivityEntity) {
            activityEntities.removeAll { it.id == activity.id }
            activityFlow.value = activityEntities.toList()
        }

        override fun getAllActive(): Flow<List<ActivityEntity>> =
            activityFlow.map { list -> list.filter { !it.isArchived } }

        override fun getAll(): Flow<List<ActivityEntity>> = activityFlow

        override suspend fun getById(id: Long): ActivityEntity? =
            activityEntities.find { it.id == id }

        override suspend fun getByIds(ids: List<Long>): List<ActivityEntity> =
            activityEntities.filter { it.id in ids }

        override suspend fun getByName(name: String): ActivityEntity? =
            activityEntities.find { it.name == name }

        override suspend fun setArchived(id: Long, archived: Boolean) {
            activityEntities.replaceAll {
                if (it.id == id) it.copy(isArchived = archived) else it
            }
            activityFlow.value = activityEntities.toList()
        }

        override fun search(query: String): Flow<List<ActivityEntity>> =
            activityFlow.map { list ->
                list.filter { it.name.contains(query) && !it.isArchived }
            }

        override fun getUncategorized(): Flow<List<ActivityEntity>> = flowOf(emptyList())

        override fun getByGroup(groupId: Long): Flow<List<ActivityEntity>> = flowOf(emptyList())

        override fun getAllPresets(): Flow<List<ActivityEntity>> = flowOf(emptyList())

        override suspend fun moveToGroup(activityId: Long, groupId: Long?) {}

        override suspend fun deleteById(id: Long) {
            activityEntities.removeAll { it.id == id }
            activityFlow.value = activityEntities.toList()
        }

        override suspend fun deleteAll() {
            activityEntities.clear()
            activityFlow.value = emptyList()
        }

        override suspend fun getAllPresetsSync(): List<ActivityEntity> = emptyList()
        override suspend fun getTagIdsForActivitySync(activityId: Long): List<Long> = emptyList()
        override suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity) {}
        override suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>) {}
        override suspend fun deleteActivityTagBindingsForActivity(activityId: Long) {}
        override suspend fun getAllActiveSync(): List<ActivityEntity> = activityEntities.filter { !it.isArchived }
        override suspend fun insertAll(activities: List<ActivityEntity>) {}
    }

    private val fakeGroupDao = object : ActivityGroupDao {
        override fun getAll(): Flow<List<ActivityGroupEntity>> = groupFlow

        override suspend fun insert(group: ActivityGroupEntity): Long {
            val id = groupEntities.size.toLong() + 1
            groupEntities.add(group.copy(id = id))
            groupFlow.value = groupEntities.toList()
            return id
        }

        override suspend fun update(group: ActivityGroupEntity) {
            groupEntities.replaceAll { if (it.id == group.id) group else it }
            groupFlow.value = groupEntities.toList()
        }

        override suspend fun delete(group: ActivityGroupEntity) {
            groupEntities.removeAll { it.id == group.id }
            groupFlow.value = groupEntities.toList()
        }

        override suspend fun ungroupAllActivities(groupId: Long) {}

        override suspend fun getByName(name: String): ActivityGroupEntity? =
            groupEntities.find { it.name == name }

        override suspend fun renameByName(oldName: String, newName: String) {
            groupEntities.replaceAll {
                if (it.name == oldName) it.copy(name = newName) else it
            }
            groupFlow.value = groupEntities.toList()
        }

        override suspend fun deleteByName(name: String) {
            groupEntities.removeAll { it.name == name }
            groupFlow.value = groupEntities.toList()
        }

        override suspend fun deleteAll() {
            groupEntities.clear()
            groupFlow.value = emptyList()
        }

        override suspend fun getMaxSortOrder(): Int? = groupEntities.maxOfOrNull { it.sortOrder }
        override suspend fun getById(id: Long): ActivityGroupEntity? = groupEntities.find { it.id == id }
        override suspend fun getAllSync(): List<ActivityGroupEntity> = emptyList()
        override suspend fun updateSortOrder(id: Long, sortOrder: Int) {}
    }

    private val repository = ActivityRepositoryImpl(fakeActivityDao, fakeGroupDao)

    @Test
    fun `getAllActive returns only non-archived activities`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "活动A", isArchived = false))
        fakeActivityDao.insert(ActivityEntity(name = "活动B", isArchived = true))

        val result = repository.getAllActive().first()

        assertEquals(1, result.size)
        assertEquals("活动A", result[0].name)
    }

    @Test
    fun `getAll returns all activities including archived`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "活动A", isArchived = false))
        fakeActivityDao.insert(ActivityEntity(name = "活动B", isArchived = true))

        val result = repository.getAll().first()

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllGroups returns mapped groups`() = runTest {
        fakeGroupDao.insert(ActivityGroupEntity(name = "分组A", sortOrder = 0))

        val result = repository.getAllGroups().first()

        assertEquals(1, result.size)
        assertEquals("分组A", result[0].name)
        assertEquals(0, result[0].sortOrder)
    }

    @Test
    fun `search filters by query and excludes archived`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "搜索测试", isArchived = false))
        fakeActivityDao.insert(ActivityEntity(name = "其他", isArchived = false))
        fakeActivityDao.insert(ActivityEntity(name = "已归档搜索", isArchived = true))

        val result = repository.search("搜索").first()

        assertEquals(1, result.size)
        assertEquals("搜索测试", result[0].name)
    }

    @Test
    fun `getById returns activity when found`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "活动A"))

        val result = repository.getById(1L)

        assertEquals("活动A", result?.name)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        val result = repository.getById(999L)
        assertNull(result)
    }

    @Test
    fun `getByName returns activity when found`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "活动A"))

        val result = repository.getByName("活动A")

        assertEquals("活动A", result?.name)
    }

    @Test
    fun `getByName returns null when not found`() = runTest {
        val result = repository.getByName("不存在")
        assertNull(result)
    }

    @Test
    fun `insert adds activity and returns id`() = runTest {
        val activity = Activity(id = 0, name = "新活动")

        val id = repository.insert(activity)

        assertEquals(1L, id)
        val all = repository.getAll().first()
        assertEquals(1, all.size)
        assertEquals("新活动", all[0].name)
    }

    @Test
    fun `update modifies existing activity`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "旧名称"))
        val updated = Activity(id = 1L, name = "新名称")

        repository.update(updated)

        val result = repository.getById(1L)
        assertEquals("新名称", result?.name)
    }

    @Test
    fun `setArchived updates archive status`() = runTest {
        fakeActivityDao.insert(ActivityEntity(name = "活动A", isArchived = false))

        repository.setArchived(1L, true)

        val result = repository.getById(1L)
        assertTrue(result?.isArchived == true)
    }

    @Test
    fun `entity to model mapping preserves all fields`() = runTest {
        fakeActivityDao.insert(
            ActivityEntity(
                name = "完整活动",
                iconKey = "icon",
                groupId = 2L,
                isPreset = true,
                isArchived = false,
                color = 0xFF0000FF,
            )
        )

        val result = repository.getById(1L)

        assertEquals(1L, result?.id)
        assertEquals("完整活动", result?.name)
        assertEquals("icon", result?.iconKey)
        assertEquals(2L, result?.groupId)
        assertTrue(result?.isPreset == true)
        assertFalse(result?.isArchived == true)
        assertEquals(0xFF0000FF, result?.color)
    }

    @Test
    fun `group entity to model mapping preserves all fields`() = runTest {
        fakeGroupDao.insert(ActivityGroupEntity(name = "分组A", sortOrder = 5))

        val result = repository.getAllGroups().first()

        assertEquals(1L, result[0].id)
        assertEquals("分组A", result[0].name)
        assertEquals(5, result[0].sortOrder)
    }
}
