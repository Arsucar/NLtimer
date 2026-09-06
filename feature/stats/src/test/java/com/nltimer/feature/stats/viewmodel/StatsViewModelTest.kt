package com.nltimer.feature.stats.viewmodel

import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorWithDetails
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.StatsDashboardConfig
import com.nltimer.core.data.model.StatsPanelConfig
import com.nltimer.core.data.model.StatsPanelType
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.model.defaultStatsDashboardConfig
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.usecase.StatsQueryUseCase
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsPrefs: FakeSettingsPrefs
    private lateinit var viewModel: StatsViewModel

    private val initialPanels = persistentListOf(
        StatsPanelConfig(id = "a", type = StatsPanelType.METRIC_CARD, title = "A", colSpan = 2),
        StatsPanelConfig(id = "b", type = StatsPanelType.METRIC_CARD, title = "B", colSpan = 2),
        StatsPanelConfig(id = "c", type = StatsPanelType.PIE_CHART, title = "C", colSpan = 4),
    )
    private val initialConfig = StatsDashboardConfig(panels = initialPanels)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsPrefs = FakeSettingsPrefs(initialConfig)
        viewModel = StatsViewModel(
            statsQueryUseCase = StatsQueryUseCase(FakeBehaviorRepository()),
            settingsPrefs = settingsPrefs,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun movePanel_updatesUiStateBeforePersist_andStaleConfigDoesNotClobber() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals(listOf("a", "b", "c"), panelIds())

        viewModel.movePanel(0, 2)
        assertEquals(listOf("b", "a", "c"), panelIds())
        assertEquals(0, settingsPrefs.persistCount)

        settingsPrefs.dashboardFlow.value = StatsDashboardConfig(
            panels = persistentListOf(initialPanels.first()),
        )
        advanceUntilIdle()
        assertEquals(listOf("b", "a", "c"), panelIds())
    }

    @Test
    fun persistPanels_reEnablesDataStoreCollect() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.movePanel(0, 2)
        viewModel.persistPanels()
        advanceUntilIdle()
        assertEquals(listOf("b", "a", "c"), panelIds())

        settingsPrefs.dashboardFlow.value = StatsDashboardConfig(
            panels = persistentListOf(initialPanels.last()),
        )
        advanceUntilIdle()
        assertEquals(listOf("c"), panelIds())
    }

    @Test
    fun persistPanels_writesOptimisticOrderOnce() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        viewModel.movePanel(2, 0)
        assertEquals(listOf("c", "a", "b"), panelIds())
        viewModel.persistPanels()
        advanceUntilIdle()

        assertEquals(1, settingsPrefs.persistCount)
        assertEquals(listOf("c", "a", "b"), settingsPrefs.lastPersisted?.panels?.map { it.id })
        assertEquals(listOf("c", "a", "b"), panelIds())
    }

    @Test
    fun removePanel_optimisticAndPersists() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        viewModel.removePanel("b")
        assertEquals(listOf("a", "c"), panelIds())
        advanceUntilIdle()
        assertEquals(listOf("a", "c"), settingsPrefs.lastPersisted?.panels?.map { it.id })
    }

    @Test
    fun addPanel_optimisticAndPersists() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        viewModel.addPanel(StatsPanelType.BAR_CHART)
        assertEquals(4, panelIds().size)
        assertEquals("柱状图", viewModel.uiState.value.dashboardConfig.panels.last().title)
        advanceUntilIdle()
        assertEquals(4, settingsPrefs.lastPersisted?.panels?.size)
    }

    @Test
    fun resetToDefault_replacesPanels() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        viewModel.resetToDefault()
        advanceUntilIdle()
        assertEquals(
            defaultStatsDashboardConfig().panels.map { it.id },
            panelIds(),
        )
        assertEquals(
            defaultStatsDashboardConfig().panels.map { it.id },
            settingsPrefs.lastPersisted?.panels?.map { it.id },
        )
    }

    @Test
    fun movePanel_sameSlot_isNoOp() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.movePanel(1, 1)
        viewModel.movePanel(1, 2)
        assertEquals(listOf("a", "b", "c"), panelIds())
        assertEquals(0, settingsPrefs.persistCount)
    }

    @Test
    fun toggleEditMode_flipsFlag() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.isEditMode)
        viewModel.toggleEditMode()
        assertTrue(viewModel.uiState.value.isEditMode)
        viewModel.toggleEditMode()
        assertTrue(!viewModel.uiState.value.isEditMode)
    }

    private fun panelIds(): List<String> =
        viewModel.uiState.value.dashboardConfig.panels.map { it.id }

    private class FakeSettingsPrefs(
        initial: StatsDashboardConfig,
    ) : SettingsPrefs {
        val dashboardFlow = MutableStateFlow(initial)
        var lastPersisted: StatsDashboardConfig? = null
        var persistCount: Int = 0

        override fun getThemeFlow(): Flow<Theme> = flowOf(Theme())
        override suspend fun updateTheme(theme: Theme) {}
        override fun getSavedTagCategories(): Flow<Set<String>> = flowOf(emptySet())
        override fun getSavedTagCategoriesOrder(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveTagCategories(categories: Set<String>) {}
        override suspend fun saveTagCategoriesOrder(categories: List<String>) {}
        override fun getDialogConfigFlow(): Flow<DialogGridConfig> = flowOf(DialogGridConfig())
        override suspend fun updateDialogConfig(config: DialogGridConfig) {}
        override fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig> = flowOf(TimeLabelConfig())
        override suspend fun updateTimeLabelConfig(config: TimeLabelConfig) {}
        override fun getHomeLayoutConfigFlow(): Flow<com.nltimer.core.data.model.HomeLayoutConfig> =
            flowOf(com.nltimer.core.data.model.HomeLayoutConfig())
        override suspend fun updateHomeLayoutConfig(config: com.nltimer.core.data.model.HomeLayoutConfig) {}
        override fun getHasSeenIntroFlow(): Flow<Boolean> = flowOf(false)
        override suspend fun setHasSeenIntro(seen: Boolean) {}
        override fun getDisplayColorConfigFlow(): Flow<com.nltimer.core.data.model.DisplayColorConfig> =
            flowOf(com.nltimer.core.data.model.DisplayColorConfig())
        override suspend fun updateDisplayColorConfig(config: com.nltimer.core.data.model.DisplayColorConfig) {}
        override fun getTagDisplayConfigFlow(): Flow<com.nltimer.core.data.model.TagDisplayConfig> =
            flowOf(com.nltimer.core.data.model.TagDisplayConfig())
        override suspend fun updateTagDisplayConfig(config: com.nltimer.core.data.model.TagDisplayConfig) {}
        override fun getFocusCardConfigFlow(): Flow<com.nltimer.core.data.model.FocusCardConfig> =
            flowOf(com.nltimer.core.data.model.FocusCardConfig())
        override suspend fun updateFocusCardConfig(config: com.nltimer.core.data.model.FocusCardConfig) {}
        override fun getStatsDashboardConfigFlow(): Flow<StatsDashboardConfig> = dashboardFlow
        override suspend fun updateStatsDashboardConfig(config: StatsDashboardConfig) {
            persistCount += 1
            lastPersisted = config
            dashboardFlow.value = config
        }
    }

    private class FakeBehaviorRepository : BehaviorRepository {
        override fun getByDayRange(dayStart: Long, dayEnd: Long) = flowOf(emptyList<Behavior>())
        override fun getCurrentBehavior() = flowOf(null)
        override fun getHomeBehaviors(dayStart: Long, dayEnd: Long) = flowOf(emptyList<Behavior>())
        override fun getTagsForBehavior(behaviorId: Long) = flowOf(emptyList<Tag>())
        override fun getPendingBehaviors() = flowOf(emptyList<Behavior>())
        override suspend fun getBehaviorWithDetails(behaviorId: Long): BehaviorWithDetails? = null
        override suspend fun getNextPending(): Behavior? = null
        override suspend fun getMaxSequence(): Int = 0
        override suspend fun getEarliestBehaviorDate(): java.time.LocalDate? = null
        override suspend fun insert(behavior: Behavior, tagIds: List<Long>): Long = 1L
        override suspend fun setEndTime(id: Long, endTime: Long) {}
        override suspend fun setStatus(id: Long, status: String) {}
        override suspend fun setStartTime(id: Long, startTime: Long) {}
        override suspend fun setActualDuration(id: Long, duration: Long) {}
        override suspend fun setAchievementLevel(id: Long, level: Int) {}
        override suspend fun setSequence(id: Long, sequence: Int) {}
        override suspend fun setNote(id: Long, note: String?) {}
        override suspend fun endCurrentBehavior(endTime: Long) {}
        override suspend fun completeCurrentAndStartNext(currentId: Long, idleMode: Boolean): Behavior? = null
        override suspend fun reorderGoals(orderedIds: List<Long>) {}
        override suspend fun delete(id: Long) {}
        override suspend fun settleDay(dayStart: Long, dayEnd: Long) {}
        override suspend fun updateBehavior(
            id: Long,
            activityId: Long,
            startTime: Long,
            endTime: Long?,
            status: String,
            note: String?,
        ) {}
        override suspend fun updateTagsForBehavior(behaviorId: Long, tagIds: List<Long>) {}
        override fun getBehaviorsOverlappingRange(rangeStart: Long, rangeEnd: Long) = flowOf(emptyList<Behavior>())
        override suspend fun getTagsForBehaviors(behaviorIds: List<Long>): Map<Long, List<Tag>> = emptyMap()
        override fun getBehaviorsWithDetailsByTimeRange(startTime: Long, endTime: Long) =
            flowOf(emptyList<BehaviorWithDetails>())
        override fun getBehaviorsWithDetailsOverlappingTimeRange(startTime: Long, endTime: Long) =
            flowOf(emptyList<BehaviorWithDetails>())
        override suspend fun getBehaviorsWithDetailsByTimeRangeSync(
            startTime: Long,
            endTime: Long,
        ): List<BehaviorWithDetails> = emptyList()
        override fun getTotalDurationAllBehaviors() = flowOf(0L)
        override fun getAllActivityLastUsed() = flowOf(emptyMap<Long, Long?>())
        override fun getAllTagLastUsed() = flowOf(emptyMap<Long, Long?>())
    }
}
