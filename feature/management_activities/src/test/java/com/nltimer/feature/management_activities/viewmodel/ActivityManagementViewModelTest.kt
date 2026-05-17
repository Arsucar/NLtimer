package com.nltimer.feature.management_activities.viewmodel

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.ActivityStats
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.data.usecase.AddActivityUseCase
import com.nltimer.feature.management_activities.model.DialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeActivityManagementRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: ActivityManagementViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeActivityManagementRepository()
        tagRepository = FakeTagRepository()
        viewModel = ActivityManagementViewModel(repository, AddActivityUseCase(repository), tagRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty data updates state after initial load`() = runTest {
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.uncategorizedActivities.isEmpty())
        assertTrue(uiState.groups.isEmpty())
        assertNull(uiState.dialogState)
    }

    @Test
    fun `loadData updates uiState with uncategorized activities and groups`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        val group = ActivityGroup(id = 1L, name = "分组A")
        repository.emitUncategorized(listOf(activity))
        repository.emitGroups(listOf(group))

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(1, uiState.uncategorizedActivities.size)
        assertEquals("活动A", uiState.uncategorizedActivities[0].name)
        assertEquals(1, uiState.groups.size)
        assertEquals("分组A", uiState.groups[0].group.name)
    }

    @Test
    fun `uncategorized changes do not clear loaded group activities`() = runTest {
        val groupActivity = Activity(id = 1L, name = "组内活动", groupId = 1L)
        val uncategorized = Activity(id = 2L, name = "未分组")
        val group = ActivityGroup(id = 1L, name = "分组A")

        repository.emitGroups(listOf(group))
        repository.emitActivitiesForGroup(1L, listOf(groupActivity))
        advanceUntilIdle()

        repository.emitUncategorized(listOf(uncategorized))
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(listOf(groupActivity), uiState.groups.single().activities)
        assertEquals(listOf(uncategorized), uiState.uncategorizedActivities)
    }

    @Test
    fun `initializePresets called in init`() = runTest {
        advanceUntilIdle()
        assertTrue(repository.initializePresetsCalled)
    }

    @Test
    fun `toggleGroupExpand adds groupId`() = runTest {
        viewModel.toggleGroupExpand(1L)
        assertTrue(viewModel.uiState.value.expandedGroupIds.contains(1L))
    }

    @Test
    fun `toggleGroupExpand removes groupId`() = runTest {
        viewModel.toggleGroupExpand(1L)
        viewModel.toggleGroupExpand(1L)
        assertFalse(viewModel.uiState.value.expandedGroupIds.contains(1L))
    }

    @Test
    fun `showAddActivityDialog updates dialogState`() = runTest {
        viewModel.showAddActivityDialog()
        assertTrue(viewModel.uiState.value.dialogState is DialogState.AddActivity)
    }

    @Test
    fun `showAddActivityToGroupDialog updates dialogState`() = runTest {
        val group = ActivityGroup(id = 1L, name = "分组A")
        viewModel.showAddActivityToGroupDialog(group)
        val state = viewModel.uiState.value.dialogState as? DialogState.AddActivityToGroup
        assertNotNull(state)
        assertEquals("分组A", state?.group?.name)
    }

    @Test
    fun `showActivityDetail updates dialogState and selectedActivityId`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        viewModel.showActivityDetail(activity)
        val state = viewModel.uiState.value.dialogState as? DialogState.ActivityDetail
        assertNotNull(state)
        assertEquals("活动A", state?.activity?.name)
    }

    @Test
    fun `showEditActivityDialog updates dialogState`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        viewModel.showEditActivityDialog(activity)
        advanceUntilIdle()
        val state = viewModel.uiState.value.dialogState as? DialogState.EditActivity
        assertNotNull(state)
        assertEquals("活动A", state?.activity?.name)
    }

    @Test
    fun `addActivity calls repository with trimmed name`() = runTest {
        viewModel.addActivity("  新活动  ", "emoji", 0xFF0000FF, null, null, emptyList())
        advanceUntilIdle()

        assertEquals(1, repository.addedActivities.size)
        assertEquals("新活动", repository.addedActivities[0].name)
        assertEquals("emoji", repository.addedActivities[0].iconKey)
        assertNull(repository.addedActivities[0].groupId)
    }

    @Test
    fun `addActivity with groupId passes groupId`() = runTest {
        viewModel.addActivity("新活动", null, null, 2L, null, emptyList())
        advanceUntilIdle()

        assertEquals(2L, repository.addedActivities[0].groupId)
    }

    @Test
    fun `updateActivity calls repository`() = runTest {
        val activity = Activity(id = 1L, name = "更新后")
        viewModel.updateActivity(activity, emptyList())
        advanceUntilIdle()

        assertEquals(1, repository.updatedActivities.size)
        assertEquals("更新后", repository.updatedActivities[0].name)
    }

    @Test
    fun `deleteActivity calls repository`() = runTest {
        viewModel.deleteActivity(1L)
        advanceUntilIdle()

        assertEquals(1L, repository.deletedActivityId)
    }

    @Test
    fun `moveActivityToGroup calls repository`() = runTest {
        viewModel.moveActivityToGroup(1L, 2L)
        advanceUntilIdle()

        assertEquals(1L to 2L, repository.lastMoveToGroup)
    }

    @Test
    fun `moveActivityToGroup with null groupId passes null`() = runTest {
        viewModel.moveActivityToGroup(1L, null)
        advanceUntilIdle()

        assertEquals(1L to null, repository.lastMoveToGroup)
    }

    @Test
    fun `addGroup calls repository with trimmed name`() = runTest {
        viewModel.addGroup("  新分组  ")
        advanceUntilIdle()

        assertEquals("新分组", repository.addedGroupName)
    }

    @Test
    fun `renameGroup calls repository`() = runTest {
        viewModel.renameGroup(1L, "  新名称  ")
        advanceUntilIdle()

        assertEquals(1L to "新名称", repository.lastRenameGroup)
    }

    @Test
    fun `deleteGroup calls repository`() = runTest {
        viewModel.deleteGroup(1L)
        advanceUntilIdle()

        assertEquals(1L, repository.deletedGroupId)
    }

    @Test
    fun `showDeleteGroupDialog updates dialogState`() = runTest {
        val group = ActivityGroup(id = 1L, name = "分组A")
        viewModel.showDeleteGroupDialog(group)
        assertTrue(viewModel.uiState.value.dialogState is DialogState.DeleteGroup)
    }

    @Test
    fun `showRenameGroupDialog updates dialogState`() = runTest {
        val group = ActivityGroup(id = 1L, name = "分组A")
        viewModel.showRenameGroupDialog(group)
        assertTrue(viewModel.uiState.value.dialogState is DialogState.RenameGroup)
    }

    @Test
    fun `showDeleteActivityDialog updates dialogState`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        viewModel.showDeleteActivityDialog(activity)
        assertTrue(viewModel.uiState.value.dialogState is DialogState.DeleteActivity)
    }

    @Test
    fun `showMoveToGroupDialog updates dialogState`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        viewModel.showMoveToGroupDialog(activity)
        assertTrue(viewModel.uiState.value.dialogState is DialogState.MoveToGroup)
    }

    @Test
    fun `dismissDialog clears dialogState and selectedActivityId`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        viewModel.showActivityDetail(activity)
        assertNotNull(viewModel.uiState.value.dialogState)

        viewModel.dismissDialog()
        assertNull(viewModel.uiState.value.dialogState)
    }

    private class FakeActivityManagementRepository : ActivityManagementRepository {
        private val _uncategorized = MutableStateFlow<List<Activity>>(emptyList())
        private val _groups = MutableStateFlow<List<ActivityGroup>>(emptyList())
        private val activitiesByGroupId = mutableMapOf<Long, MutableStateFlow<List<Activity>>>()

        var initializePresetsCalled = false
        val addedActivities = mutableListOf<Activity>()
        val updatedActivities = mutableListOf<Activity>()
        var deletedActivityId: Long? = null
        var lastMoveToGroup: Pair<Long, Long?>? = null
        var addedGroupName: String? = null
        var lastRenameGroup: Pair<Long, String>? = null
        var deletedGroupId: Long? = null

        fun emitUncategorized(activities: List<Activity>) {
            _uncategorized.value = activities
        }

        fun emitGroups(groups: List<ActivityGroup>) {
            _groups.value = groups
        }

        fun emitActivitiesForGroup(groupId: Long, activities: List<Activity>) {
            activitiesFlowForGroup(groupId).value = activities
        }

        private fun activitiesFlowForGroup(groupId: Long): MutableStateFlow<List<Activity>> =
            activitiesByGroupId.getOrPut(groupId) { MutableStateFlow(emptyList()) }

        override fun getAllActivities(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getUncategorizedActivities(): Flow<List<Activity>> = _uncategorized
        override fun getAllGroups(): Flow<List<ActivityGroup>> = _groups
        override fun getActivitiesByGroup(groupId: Long): Flow<List<Activity>> = activitiesFlowForGroup(groupId)
        override fun getActivityStats(activityId: Long): Flow<ActivityStats> = flowOf(ActivityStats())
        override suspend fun addActivity(activity: Activity): Long {
            addedActivities.add(activity)
            return 1L
        }
        override suspend fun updateActivity(activity: Activity) {
            updatedActivities.add(activity)
        }
        override suspend fun deleteActivity(id: Long) {
            deletedActivityId = id
        }
        override suspend fun moveActivityToGroup(activityId: Long, groupId: Long?) {
            lastMoveToGroup = activityId to groupId
        }
        override suspend fun addGroup(name: String): Long {
            addedGroupName = name
            return 1L
        }
        override suspend fun renameGroup(id: Long, newName: String) {
            lastRenameGroup = id to newName
        }
        override suspend fun deleteGroup(id: Long) {
            deletedGroupId = id
        }
        override suspend fun reorderGroups(orderedIds: List<Long>) {
            val order = orderedIds.withIndex().associate { it.value to it.index }
            _groups.value = _groups.value
                .map { group -> group.copy(sortOrder = order[group.id] ?: group.sortOrder) }
                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
        }
        override suspend fun initializePresets() {
            initializePresetsCalled = true
        }
        override suspend fun getTagIdsForActivity(activityId: Long): List<Long> = emptyList()
        override suspend fun setActivityTagBindings(activityId: Long, tagIds: List<Long>) {}
        override suspend fun getAllActivitiesSync(): List<Activity> = emptyList()
    }

    private class FakeTagRepository : TagRepository {
        override fun getAllActive(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getAll(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getByCategory(category: String): Flow<List<Tag>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<Tag>> = flowOf(emptyList())
        override fun getByActivityId(activityId: Long): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun getById(id: Long): Tag? = null
        override suspend fun getByName(name: String): Tag? = null
        override suspend fun insert(tag: Tag): Long = 1L
        override suspend fun update(tag: Tag) {}
        override suspend fun setArchived(id: Long, archived: Boolean) {}
        override fun getDistinctCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun renameCategory(oldName: String, newName: String) {}
        override suspend fun resetCategory(category: String) {}
        override suspend fun getActivityIdsForTag(tagId: Long): List<Long> = emptyList()
        override suspend fun setActivityTagBindings(tagId: Long, activityIds: List<Long>) {}
    }

    @Test
    fun `showAddGroupDialog updates dialogState`() = runTest {
        viewModel.showAddGroupDialog()
        assertTrue(viewModel.uiState.value.dialogState is DialogState.AddGroup)
    }

    @Test
    fun `currentActivityStats returns default when no activity selected`() = runTest {
        advanceUntilIdle()
        val stats = viewModel.currentActivityStats.value
        assertEquals(0, stats.usageCount)
    }

    @Test
    fun `dismissDialog resets currentActivityStats to default`() = runTest {
        val activity = Activity(id = 1L, name = "活动A")
        viewModel.showActivityDetail(activity)
        viewModel.dismissDialog()
        advanceUntilIdle()
        val stats = viewModel.currentActivityStats.value
        assertEquals(0, stats.usageCount)
    }
}
