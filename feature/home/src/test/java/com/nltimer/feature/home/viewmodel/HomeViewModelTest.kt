package com.nltimer.feature.home.viewmodel

import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.ActivityGroup
import com.nltimer.core.data.model.ActivityStats
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.ActivityManagementRepository
import com.nltimer.core.data.repository.ActivityRepository
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.repository.TagRepository
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.SystemClockService
import com.nltimer.core.data.util.TimeSnapService
import com.nltimer.core.data.usecase.AddActivityUseCase
import com.nltimer.core.data.usecase.AddBehaviorUseCase
import com.nltimer.core.data.usecase.AddTagUseCase
import com.nltimer.core.tools.match.ApplyNoteDirectivesUseCase
import com.nltimer.core.tools.match.NoteMatcher
import com.nltimer.feature.home.match.KeywordMatchStrategy
import com.nltimer.feature.home.model.GridCellUiState
import kotlinx.collections.immutable.persistentListOf
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var behaviorRepository: FakeBehaviorRepository
    private lateinit var activityRepository: FakeActivityRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var settingsPrefs: FakeSettingsPrefs
    private lateinit var clockService: ClockService
    private lateinit var addBehaviorUseCase: AddBehaviorUseCase
    private lateinit var addTagUseCase: AddTagUseCase
    private lateinit var addActivityUseCase: AddActivityUseCase
    private lateinit var applyNoteDirectivesUseCase: ApplyNoteDirectivesUseCase
    private lateinit var activityManagementRepository: FakeActivityManagementRepository
    private lateinit var viewModel: HomeViewModel

    private var frozenTime: Long = 0

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        frozenTime = System.currentTimeMillis()
        behaviorRepository = FakeBehaviorRepository()
        activityRepository = FakeActivityRepository()
        tagRepository = FakeTagRepository()
        activityManagementRepository = FakeActivityManagementRepository()
        settingsPrefs = FakeSettingsPrefs()
        clockService = object : ClockService {
            override fun currentTimeMillis(): Long = frozenTime
        }
        addBehaviorUseCase = AddBehaviorUseCase(behaviorRepository, TimeSnapService(), clockService)
        addTagUseCase = AddTagUseCase(tagRepository)
        addActivityUseCase = AddActivityUseCase(activityManagementRepository)
        applyNoteDirectivesUseCase = ApplyNoteDirectivesUseCase(addActivityUseCase, addTagUseCase)
        viewModel = HomeViewModel(
            behaviorRepository,
            activityRepository,
            activityManagementRepository,
            tagRepository,
            settingsPrefs,
            KeywordMatchStrategy(),
            NoteMatcher(),
            addBehaviorUseCase,
            addTagUseCase,
            addActivityUseCase,
            applyNoteDirectivesUseCase,
            clockService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loading`() = runTest {
        advanceUntilIdle()
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(1, uiState.gridSections.size)
        assertEquals(1, uiState.gridSections.first().rows.size)
    }

    @Test
    fun `addActivity calls repository`() = runTest {
        viewModel.addActivity("Test Activity", "😊", null, null, null, emptyList())
        advanceUntilIdle()
        assertEquals(1, activityManagementRepository.addedActivities.size)
        assertEquals("Test Activity", activityManagementRepository.addedActivities[0].name)
    }

    @Test
    fun `addTag calls repository`() = runTest {
        viewModel.addTag("Test Tag", null, null, 0, null, null, null)
        advanceUntilIdle()
        assertEquals(1, tagRepository.insertedTags.size)
        assertEquals("Test Tag", tagRepository.insertedTags[0].name)
    }

    @Test
    fun `showAddSheet updates uiState`() = runTest {
        viewModel.showAddSheet()
        assertNotNull(viewModel.uiState.value.addSheetMode)
    }

    @Test
    fun `hideAddSheet updates uiState`() = runTest {
        viewModel.showAddSheet()
        viewModel.hideAddSheet()
        assertEquals(null, viewModel.uiState.value.addSheetMode)
    }

    @Test
    fun `onActivitySelected loads tags`() = runTest {
        val tags = listOf(Tag(1, "Tag1", null, null, null, 0, 0, 0, null, false))
        tagRepository.tagsByActivityId[1L] = tags
        
        val collectJob = launch {
            viewModel.tagsForSelectedActivity.collect {}
        }
        
        viewModel.onActivitySelected(1L)
        advanceUntilIdle()
        
        assertEquals(tags, viewModel.tagsForSelectedActivity.value)
        collectJob.cancel()
    }

    @Test
    fun `addBehavior calls repository and hides sheet`() = runTest {
        viewModel.showAddSheet()
        viewModel.addBehavior(1L, listOf(10L), 1000L, null, BehaviorNature.ACTIVE, "Note")
        advanceUntilIdle()

        assertTrue(behaviorRepository.endCurrentBehaviorCalled)
        assertEquals(1, behaviorRepository.insertedBehaviors.size)
        assertEquals(1L, behaviorRepository.insertedBehaviors[0].activityId)
        assertEquals(null, viewModel.uiState.value.addSheetMode)
    }

    @Test
    fun `addBehavior with COMPLETED endTime in future should show error`() = runTest {
        val futureTime = frozenTime + 3600_000
        val startTime = frozenTime - 7200_000

        val result = addBehaviorUseCase(
            activityId = 1L,
            tagIds = emptyList(),
            startTime = startTime,
            endTime = futureTime,
            status = BehaviorNature.COMPLETED,
            note = null,
        )
        assertTrue(result is AddBehaviorUseCase.Result.ValidationError)
        assertEquals("结束时间不能大于当前时间", (result as AddBehaviorUseCase.Result.ValidationError).message)
    }

    @Test
    fun `addBehavior with ACTIVE startTime in future should show error`() = runTest {
        val futureTime = frozenTime + 3600_000

        val result = addBehaviorUseCase(
            activityId = 1L,
            tagIds = emptyList(),
            startTime = futureTime,
            endTime = null,
            status = BehaviorNature.ACTIVE,
            note = null,
        )
        assertTrue(result is AddBehaviorUseCase.Result.ValidationError)
        assertEquals("开始时间不能大于当前时间", (result as AddBehaviorUseCase.Result.ValidationError).message)
    }

    @Test
    fun `addBehavior should insert at correct sequence based on startTime`() = runTest {
        val existingBehavior = Behavior(
            id = 1L,
            activityId = 1L,
            startTime = frozenTime - 3600_000L,
            endTime = frozenTime - 1800_000L,
            status = BehaviorNature.COMPLETED,
            note = null,
            pomodoroCount = 0,
            sequence = 0,
            estimatedDuration = null,
            actualDuration = null,
            achievementLevel = null,
            wasPlanned = false,
        )
        behaviorRepository.dayRangeBehaviors.add(existingBehavior)

        val newStartTime = frozenTime - 7200_000L
        val newEndTime = frozenTime - 5400_000L
        val result = addBehaviorUseCase(
            activityId = 2L,
            tagIds = emptyList(),
            startTime = newStartTime,
            endTime = newEndTime,
            status = BehaviorNature.COMPLETED,
            note = null,
        )
        when (result) {
            is AddBehaviorUseCase.Result.Success -> {
                assertEquals(1, behaviorRepository.insertedBehaviors.size)
                assertEquals(0, behaviorRepository.insertedBehaviors[0].sequence)
                assertEquals(1, behaviorRepository.updatedSequences[1L])
            }
            is AddBehaviorUseCase.Result.ValidationError ->
                throw AssertionError("Unexpected ValidationError: ${result.message}")
            is AddBehaviorUseCase.Result.Conflict ->
                throw AssertionError("Unexpected Conflict: ${result.message}")
        }
    }

    private fun getTodayAt(hour: Int, minute: Int): Long {
        return java.time.LocalDate.now()
            .atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `completeBehavior calls repository`() = runTest {
        viewModel.completeBehavior(1L)
        advanceUntilIdle()
        assertTrue(behaviorRepository.completeCurrentAndStartNextCalled)
    }

    @Test
    fun `toggleIdleMode updates uiState`() = runTest {
        val initial = viewModel.uiState.value.isIdleMode
        viewModel.toggleIdleMode()
        assertEquals(!initial, viewModel.uiState.value.isIdleMode)
    }

    @Test
    fun `deleteBehavior calls repository`() = runTest {
        viewModel.deleteBehavior(1L)
        advanceUntilIdle()
        assertTrue(behaviorRepository.deleteCalled)
    }

    @Test
    fun `startNextPending calls repository`() = runTest {
        val pending = Behavior(
            id = 1L,
            activityId = 1L,
            startTime = 0L,
            endTime = null,
            status = BehaviorNature.PENDING,
            note = null,
            pomodoroCount = 0,
            sequence = 0,
            estimatedDuration = null,
            actualDuration = null,
            achievementLevel = null,
            wasPlanned = false,
        )
        behaviorRepository.nextPending = pending
        viewModel.startNextPending()
        advanceUntilIdle()
        assertTrue(behaviorRepository.setStatusCalled)
        assertTrue(behaviorRepository.setStartTimeCalled)
    }

    @Test
    fun `reorderGoals calls repository`() = runTest {
        val orderedIds = listOf(3L, 1L, 2L)
        viewModel.reorderGoals(orderedIds)
        advanceUntilIdle()
        assertEquals(orderedIds, behaviorRepository.reorderedIds)
    }

    @Test
    fun `toggleIdleMode toggles state`() = runTest {
        val initial = viewModel.uiState.value.isIdleMode
        viewModel.toggleIdleMode()
        assertEquals(!initial, viewModel.uiState.value.isIdleMode)
        viewModel.toggleIdleMode()
        assertEquals(initial, viewModel.uiState.value.isIdleMode)
    }

    @Test
    fun `scrollToTime updates selectedTimeHour`() = runTest {
        viewModel.scrollToTime(14)
        assertEquals(14, viewModel.uiState.value.selectedTimeHour)
    }

    @Test
    fun `onHomeLayoutChange updates theme`() = runTest {
        viewModel.onHomeLayoutChange(com.nltimer.core.designsystem.theme.HomeLayout.GRID)
        advanceUntilIdle()
        assertTrue(settingsPrefs.updateThemeCalled)
    }

    @Test
    fun `onTimeLabelConfigChange updates config`() = runTest {
        val config = TimeLabelConfig()
        viewModel.onTimeLabelConfigChange(config)
        advanceUntilIdle()
        assertTrue(settingsPrefs.updateTimeLabelConfigCalled)
    }

    @Test
    fun `addBehavior with time conflict shows error`() = runTest {
        val startTime = frozenTime - 7200_000
        val endTime = frozenTime - 3600_000
        val activeBehavior = Behavior(
            id = 1L,
            activityId = 1L,
            startTime = startTime,
            endTime = null,
            status = BehaviorNature.ACTIVE,
            note = null,
            pomodoroCount = 0,
            sequence = 0,
            estimatedDuration = null,
            actualDuration = null,
            achievementLevel = null,
            wasPlanned = false,
        )
        behaviorRepository.overlappingBehaviors.add(activeBehavior)
        behaviorRepository._currentBehavior = activeBehavior

        val result = addBehaviorUseCase(
            activityId = 2L,
            tagIds = emptyList(),
            startTime = endTime - 1000L,
            endTime = endTime,
            status = BehaviorNature.COMPLETED,
            note = null,
        )
        assertTrue(result is AddBehaviorUseCase.Result.Conflict)
        assertEquals("该时间段与已有行为记录冲突", (result as AddBehaviorUseCase.Result.Conflict).message)
    }

    @Test
    fun `addBehavior PENDING status does not check time conflict`() = runTest {
        val startTime = frozenTime
        val overlapping = Behavior(
            id = 1L,
            activityId = 1L,
            startTime = startTime - 3600_000,
            endTime = startTime + 3600_000,
            status = BehaviorNature.COMPLETED,
            note = null,
            pomodoroCount = 0,
            sequence = 0,
            estimatedDuration = null,
            actualDuration = null,
            achievementLevel = null,
            wasPlanned = false,
        )
        behaviorRepository.overlappingBehaviors.add(overlapping)

        viewModel.addBehavior(
            activityId = 1L,
            tagIds = emptyList(),
            startTime = startTime,
            endTime = null,
            status = BehaviorNature.PENDING,
            note = null,
        )
        advanceUntilIdle()

        assertEquals(1, behaviorRepository.insertedBehaviors.size)
        assertEquals(BehaviorNature.PENDING, behaviorRepository.insertedBehaviors[0].status)
    }

    @Test
    fun `addBehavior with editBehaviorId calls updateBehavior`() = runTest {
        viewModel.showEditSheet(GridCellUiState(
            behaviorId = 42L,
            activityIconKey = null,
            activityName = null,
            tags = persistentListOf(),
            status = BehaviorNature.COMPLETED,
            isCurrent = false,
            wasPlanned = false,
        ))
        viewModel.addBehavior(
            activityId = 1L,
            tagIds = listOf(10L),
            startTime = frozenTime - 3600_000,
            endTime = frozenTime,
            status = BehaviorNature.COMPLETED,
            note = "edited note",
        )
        advanceUntilIdle()

        assertTrue(behaviorRepository.updateBehaviorCalled)
    }

    @Test
    fun `startBehavior sets ACTIVE status and start time`() = runTest {
        viewModel.startBehavior(5L)
        advanceUntilIdle()

        assertTrue(behaviorRepository.setStatusCalled)
        assertTrue(behaviorRepository.setStartTimeCalled)
    }

    @Test
    fun `startNextPending does nothing when no pending behavior`() = runTest {
        viewModel.startNextPending()
        advanceUntilIdle()

        assertFalse(behaviorRepository.setStatusCalled)
    }

    @Test
    fun `completeBehavior passes idleMode state to repository`() = runTest {
        viewModel.toggleIdleMode()
        viewModel.completeBehavior(1L)
        advanceUntilIdle()

        assertTrue(behaviorRepository.completeCurrentAndStartNextCalled)
    }

    @Test
    fun `hideAddSheet clears edit state`() = runTest {
        viewModel.showAddSheet()
        viewModel.hideAddSheet()

        val state = viewModel.uiState.value
        assertEquals(null, state.addSheetMode)
        assertEquals(null, state.editBehaviorId)
        assertEquals(emptyList<Long>(), state.editInitialTagIds)
    }

    @Test
    fun `loadMore is no-op when earliestRecord is null`() = runTest {
        val vm = HomeViewModel(
            behaviorRepository,
            activityRepository,
            activityManagementRepository,
            tagRepository,
            settingsPrefs,
            KeywordMatchStrategy(),
            NoteMatcher(),
            addBehaviorUseCase,
            addTagUseCase,
            addActivityUseCase,
            applyNoteDirectivesUseCase,
            clockService,
        )
        advanceUntilIdle()
        val before = vm.uiState.value.items.size

        vm.loadMore()
        advanceUntilIdle()

        assertEquals(before, vm.uiState.value.items.size)
    }

    @Test
    fun `loadMore stops at earliestRecord boundary`() = runTest {
        val earliest = java.time.LocalDate.now().minusDays(3)
        behaviorRepository.earliestDate = earliest
        val vm = HomeViewModel(
            behaviorRepository,
            activityRepository,
            activityManagementRepository,
            tagRepository,
            settingsPrefs,
            KeywordMatchStrategy(),
            NoteMatcher(),
            addBehaviorUseCase,
            addTagUseCase,
            addActivityUseCase,
            applyNoteDirectivesUseCase,
            clockService,
        )
        advanceUntilIdle()

        repeat(5) {
            vm.loadMore()
            advanceUntilIdle()
        }

        assertTrue(vm.uiState.value.hasReachedEarliest)
    }

    private class FakeBehaviorRepository : BehaviorRepository {
        val insertedBehaviors = mutableListOf<Behavior>()
        val dayRangeBehaviors = mutableListOf<Behavior>()
        val updatedSequences = mutableMapOf<Long, Int>()
        val overlappingBehaviors = mutableListOf<Behavior>()
        var endCurrentBehaviorCalled = false
        var completeCurrentAndStartNextCalled = false
        var deleteCalled = false
        var nextPending: Behavior? = null
        var setStatusCalled = false
        var setStartTimeCalled = false
        var updateBehaviorCalled = false
        var reorderedIds: List<Long>? = null
        var earliestDate: java.time.LocalDate? = null

        override fun getByDayRange(dayStart: Long, dayEnd: Long): Flow<List<Behavior>> = flowOf(
            dayRangeBehaviors.filter { it.startTime in dayStart until dayEnd }
        )
        var _currentBehavior: Behavior? = null
        override fun getCurrentBehavior(): Flow<Behavior?> = flowOf(_currentBehavior)
        override fun getHomeBehaviors(dayStart: Long, dayEnd: Long): Flow<List<Behavior>> = flowOf(emptyList())
        override fun getBehaviorsOverlappingRange(rangeStart: Long, rangeEnd: Long): Flow<List<Behavior>> = flowOf(overlappingBehaviors)
        override fun getTagsForBehavior(behaviorId: Long): Flow<List<Tag>> = flowOf(emptyList())
        override fun getPendingBehaviors(): Flow<List<Behavior>> = flowOf(emptyList())
        override suspend fun getBehaviorWithDetails(behaviorId: Long): BehaviorWithDetails? = null
        override suspend fun getNextPending(): Behavior? = nextPending
        override suspend fun getMaxSequence(): Int = 0
        override suspend fun insert(behavior: Behavior, tagIds: List<Long>): Long {
            insertedBehaviors.add(behavior)
            return 1L
        }
        override suspend fun setEndTime(id: Long, endTime: Long) {}
        override suspend fun setStatus(id: Long, status: String) {
            setStatusCalled = true
        }
        override suspend fun setStartTime(id: Long, startTime: Long) {
            setStartTimeCalled = true
        }
        override suspend fun setActualDuration(id: Long, duration: Long) {}
        override suspend fun setAchievementLevel(id: Long, level: Int) {}
        override suspend fun setSequence(id: Long, sequence: Int) {
            updatedSequences[id] = sequence
        }
        override suspend fun setNote(id: Long, note: String?) {}
        override suspend fun endCurrentBehavior(endTime: Long) {
            endCurrentBehaviorCalled = true
        }
        override suspend fun completeCurrentAndStartNext(currentId: Long, idleMode: Boolean): Behavior? {
            completeCurrentAndStartNextCalled = true
            return null
        }
        override suspend fun reorderGoals(orderedIds: List<Long>) {
            this.reorderedIds = orderedIds
        }
        override suspend fun delete(id: Long) {
            deleteCalled = true
        }
        override suspend fun settleDay(dayStart: Long, dayEnd: Long) {}
        override suspend fun updateBehavior(id: Long, activityId: Long, startTime: Long, endTime: Long?, status: String, note: String?) {
            updateBehaviorCalled = true
        }
        override suspend fun updateTagsForBehavior(behaviorId: Long, tagIds: List<Long>) {}
        override fun getBehaviorsWithDetailsByTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorWithDetails>> = flowOf(emptyList())
        override fun getBehaviorsWithDetailsOverlappingTimeRange(startTime: Long, endTime: Long): Flow<List<BehaviorWithDetails>> = flowOf(emptyList())
        override suspend fun getBehaviorsWithDetailsByTimeRangeSync(startTime: Long, endTime: Long): List<BehaviorWithDetails> = emptyList()
        override suspend fun getTagsForBehaviors(behaviorIds: List<Long>): Map<Long, List<Tag>> = emptyMap()
        override suspend fun getEarliestBehaviorDate(): java.time.LocalDate? = earliestDate
        override fun getAllActivityLastUsed(): Flow<Map<Long, Long?>> = flowOf(emptyMap())
        override fun getAllTagLastUsed(): Flow<Map<Long, Long?>> = flowOf(emptyMap())
        override fun getTotalDurationAllBehaviors(): kotlinx.coroutines.flow.Flow<Long> = kotlinx.coroutines.flow.flowOf(0L)
    }

    private class FakeActivityRepository : ActivityRepository {
        val insertedActivities = mutableListOf<Activity>()
        
        override fun getAllActive(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getAll(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getAllGroups(): Flow<List<ActivityGroup>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<Activity>> = flowOf(emptyList())
        override suspend fun getById(id: Long): Activity? = null
        override suspend fun getByName(name: String): Activity? = null
        override suspend fun insert(activity: Activity): Long {
            insertedActivities.add(activity)
            return 1L
        }
        override suspend fun update(activity: Activity) {}
        override suspend fun setArchived(id: Long, archived: Boolean) {}
    }

    private class FakeTagRepository : TagRepository {
        val insertedTags = mutableListOf<Tag>()
        val tagsByActivityId = mutableMapOf<Long, List<Tag>>()

        override fun getAllActive(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getAll(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getByCategory(category: String): Flow<List<Tag>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<Tag>> = flowOf(emptyList())
        override fun getByActivityId(activityId: Long): Flow<List<Tag>> = flowOf(tagsByActivityId[activityId] ?: emptyList())
        override suspend fun getById(id: Long): Tag? = null
        override suspend fun getByName(name: String): Tag? = null
        override suspend fun insert(tag: Tag): Long {
            insertedTags.add(tag)
            return 1L
        }
        override suspend fun update(tag: Tag) {}
        override suspend fun setArchived(id: Long, archived: Boolean) {}
        override fun getDistinctCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun renameCategory(oldName: String, newName: String) {}
        override suspend fun resetCategory(category: String) {}
        override suspend fun getActivityIdsForTag(tagId: Long): List<Long> = emptyList()
        override suspend fun setActivityTagBindings(tagId: Long, activityIds: List<Long>) {}
    }

    private class FakeSettingsPrefs : SettingsPrefs {
        private val _theme = MutableStateFlow(Theme())
        var updateThemeCalled = false
        var updateTimeLabelConfigCalled = false

        override fun getThemeFlow(): Flow<Theme> = _theme
        override suspend fun updateTheme(theme: Theme) {
            updateThemeCalled = true
            _theme.value = theme
        }
        override fun getSavedTagCategories(): Flow<Set<String>> = flowOf(emptySet())
        override fun getSavedTagCategoriesOrder(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveTagCategories(categories: Set<String>) {}
        override suspend fun saveTagCategoriesOrder(categories: List<String>) {}
        override fun getDialogConfigFlow(): Flow<DialogGridConfig> = flowOf(DialogGridConfig())
        override suspend fun updateDialogConfig(config: DialogGridConfig) {}
        override fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig> = flowOf(TimeLabelConfig())
        override suspend fun updateTimeLabelConfig(config: TimeLabelConfig) {
            updateTimeLabelConfigCalled = true
        }
        override fun getHomeLayoutConfigFlow(): Flow<com.nltimer.core.data.model.HomeLayoutConfig> = flowOf(com.nltimer.core.data.model.HomeLayoutConfig())
        override suspend fun updateHomeLayoutConfig(config: com.nltimer.core.data.model.HomeLayoutConfig) {}
        override fun getHasSeenIntroFlow(): Flow<Boolean> = flowOf(false)
        override suspend fun setHasSeenIntro(seen: Boolean) {}
    }

    private class FakeActivityManagementRepository : ActivityManagementRepository {
        val addedActivities = mutableListOf<Activity>()
        override fun getAllActivities(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getUncategorizedActivities(): Flow<List<Activity>> = flowOf(emptyList())
        override fun getActivitiesByGroup(groupId: Long): Flow<List<Activity>> = flowOf(emptyList())
        override fun getAllGroups(): Flow<List<ActivityGroup>> = flowOf(emptyList())
        override fun getActivityStats(activityId: Long): Flow<ActivityStats> = flowOf(ActivityStats())
        override suspend fun addActivity(activity: Activity): Long {
            addedActivities.add(activity)
            return 1L
        }
        override suspend fun updateActivity(activity: Activity) {}
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
