package com.nltimer.feature.management_activities.viewmodel

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.ActivityStats
import com.nltimer.core.data.repository.ActivityManagementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityArchiveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeActivityManagementRepository
    private lateinit var viewModel: ActivityArchiveViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeActivityManagementRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load archived list maps groupId to group name`() = runTest {
        repository.emitGroups(listOf(ActivityGroup(id = 10L, name = "学习")))
        repository.emitArchived(
            listOf(
                Activity(
                    id = 1L,
                    name = "阅读",
                    groupId = 10L,
                    isArchived = true,
                    archivedAt = 1000L,
                ),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(1, uiState.groups.size)
        assertEquals("学习", uiState.groups[0].name)
        assertEquals("阅读", uiState.groups[0].items[0].activity.name)
        assertEquals("学习", uiState.groups[0].items[0].groupName)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `null groupId maps to uncategorized`() = runTest {
        repository.emitGroups(listOf(ActivityGroup(id = 10L, name = "学习")))
        repository.emitArchived(
            listOf(
                Activity(
                    id = 1L,
                    name = "未分组活动",
                    groupId = null,
                    isArchived = true,
                    archivedAt = 1000L,
                ),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        val group = viewModel.uiState.value.groups.single()
        assertEquals("未分类", group.name)
        assertEquals("未分组活动", group.items.single().activity.name)
    }

    @Test
    fun `missing group maps to uncategorized`() = runTest {
        repository.emitGroups(emptyList())
        repository.emitArchived(
            listOf(
                Activity(
                    id = 1L,
                    name = "孤儿活动",
                    groupId = 99L,
                    isArchived = true,
                    archivedAt = 1000L,
                ),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        assertEquals("未分类", viewModel.uiState.value.groups.single().name)
    }

    @Test
    fun `two activities same group appear in one group`() = runTest {
        repository.emitGroups(listOf(ActivityGroup(id = 10L, name = "学习")))
        repository.emitArchived(
            listOf(
                Activity(id = 1L, name = "阅读", groupId = 10L, isArchived = true, archivedAt = 100L),
                Activity(id = 2L, name = "笔记", groupId = 10L, isArchived = true, archivedAt = 200L),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        val group = viewModel.uiState.value.groups.single()
        assertEquals("学习", group.name)
        assertEquals(listOf("笔记", "阅读"), group.items.map { it.activity.name })
    }

    @Test
    fun `different groups do not mix`() = runTest {
        repository.emitGroups(
            listOf(
                ActivityGroup(id = 10L, name = "学习"),
                ActivityGroup(id = 20L, name = "生活"),
            ),
        )
        repository.emitArchived(
            listOf(
                Activity(id = 1L, name = "阅读", groupId = 10L, isArchived = true, archivedAt = 10L),
                Activity(id = 2L, name = "散步", groupId = 20L, isArchived = true, archivedAt = 20L),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        val groups = viewModel.uiState.value.groups
        assertEquals(listOf("生活", "学习"), groups.map { it.name })
        assertEquals(listOf("散步"), groups[0].items.map { it.activity.name })
        assertEquals(listOf("阅读"), groups[1].items.map { it.activity.name })
    }

    @Test
    fun `within group later archivedAt comes first and nulls last`() = runTest {
        repository.emitGroups(listOf(ActivityGroup(id = 10L, name = "学习")))
        repository.emitArchived(
            listOf(
                Activity(id = 1L, name = "旧", groupId = 10L, isArchived = true, archivedAt = 100L),
                Activity(id = 2L, name = "无日期", groupId = 10L, isArchived = true, archivedAt = null),
                Activity(id = 3L, name = "新", groupId = 10L, isArchived = true, archivedAt = 300L),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        assertEquals(
            listOf("新", "旧", "无日期"),
            viewModel.uiState.value.groups.single().items.map { it.activity.name },
        )
    }

    @Test
    fun `未分类 group is first then others by newest archive`() = runTest {
        repository.emitGroups(
            listOf(
                ActivityGroup(id = 10L, name = "学习"),
                ActivityGroup(id = 20L, name = "生活"),
            ),
        )
        repository.emitArchived(
            listOf(
                Activity(id = 1L, name = "未分组", groupId = null, isArchived = true, archivedAt = 1L),
                Activity(id = 2L, name = "阅读", groupId = 10L, isArchived = true, archivedAt = 50L),
                Activity(id = 3L, name = "散步", groupId = 20L, isArchived = true, archivedAt = 100L),
            ),
        )
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        assertEquals(
            listOf("未分类", "生活", "学习"),
            viewModel.uiState.value.groups.map { it.name },
        )
    }

    @Test
    fun `empty archived list`() = runTest {
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.groups.isEmpty())
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `restore calls setArchived false`() = runTest {
        val activity = Activity(id = 2L, name = "阅读", isArchived = true, archivedAt = 1000L)
        repository.emitArchived(listOf(activity))
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        viewModel.restore(2L)
        advanceUntilIdle()

        assertEquals(2L, repository.archivedId)
        assertEquals(false, repository.archivedValue)
        assertEquals("已恢复「阅读」", viewModel.snackbarMessage.value)
    }

    @Test
    fun `restore missing id shows failure snackbar`() = runTest {
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        viewModel.restore(999L)
        advanceUntilIdle()

        assertNull(repository.archivedId)
        assertEquals("恢复失败", viewModel.snackbarMessage.value)
    }

    @Test
    fun `error path sets error message`() = runTest {
        repository.failLoad = true
        viewModel = ActivityArchiveViewModel(repository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("加载失败", uiState.errorMessage)
    }

    private class FakeActivityManagementRepository : ActivityManagementRepository {
        private val _archived = MutableStateFlow<List<Activity>>(emptyList())
        private val _groups = MutableStateFlow<List<ActivityGroup>>(emptyList())
        var failLoad = false
        var archivedId: Long? = null
        var archivedValue: Boolean? = null

        fun emitArchived(items: List<Activity>) {
            _archived.value = items
        }

        fun emitGroups(items: List<ActivityGroup>) {
            _groups.value = items
        }

        override fun getAllActivities(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getArchived(): Flow<List<Activity>> =
            if (failLoad) flow { throw RuntimeException("boom") } else _archived
        override fun getUncategorizedActivities(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getActivitiesByGroup(groupId: Long): Flow<List<Activity>> = flowOf(emptyList())
        override fun getAllGroups(): Flow<List<ActivityGroup>> = _groups
        override fun getActivityStats(activityId: Long): Flow<ActivityStats> = flowOf(ActivityStats())
        override suspend fun addActivity(activity: Activity): Long = 1L
        override suspend fun updateActivity(activity: Activity) {}
        override suspend fun setArchived(id: Long, archived: Boolean) {
            archivedId = id
            archivedValue = archived
            if (!archived) {
                _archived.value = _archived.value.filterNot { it.id == id }
            }
        }
        override suspend fun deleteActivity(id: Long) {}
        override suspend fun moveActivityToGroup(activityId: Long, groupId: Long?) {}
        override suspend fun addGroup(name: String): Long = 1L
        override suspend fun renameGroup(id: Long, newName: String) {}
        override suspend fun deleteGroup(id: Long) {}
        override suspend fun reorderGroups(orderedIds: List<Long>) {}
        override suspend fun initializePresets() {}
        override suspend fun getTagIdsForActivity(activityId: Long): List<Long> = emptyList()
        override suspend fun setActivityTagBindings(activityId: Long, tagIds: List<Long>) {}
        override suspend fun getAllActivitiesSync(): List<Activity> = emptyList()
    }
}
