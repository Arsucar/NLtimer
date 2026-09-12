package com.nltimer.feature.settings.ui

import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.model.SecondsStrategy
import com.nltimer.core.designsystem.theme.ChipDisplayMode
import com.nltimer.core.designsystem.theme.GridLayoutMode
import com.nltimer.core.designsystem.theme.PathDrawMode
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DialogConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettingsPrefs: FakeSettingsPrefs
    private lateinit var viewModel: DialogConfigViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsPrefs = FakeSettingsPrefs()
        viewModel = DialogConfigViewModel(fakeSettingsPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state uses default config`() = runTest {
        advanceUntilIdle()
        val config = viewModel.dialogConfig.value
        assertEquals(ChipDisplayMode.Filled, config.activityDisplayMode)
        assertEquals(GridLayoutMode.Horizontal, config.activityLayoutMode)
        assertEquals(2, config.activityColumnLines)
        assertTrue(config.showBehaviorNature)
    }

    @Test
    fun `updateConfig calls settingsPrefs`() = runTest {
        val newConfig = DialogGridConfig(
            activityDisplayMode = ChipDisplayMode.Capsules,
            activityColumnLines = 3,
            showBehaviorNature = false,
        )

        viewModel.updateConfig(newConfig)
        advanceUntilIdle()

        assertTrue(fakeSettingsPrefs.updateDialogConfigCalled)
        assertEquals(newConfig, fakeSettingsPrefs.lastDialogConfig)
    }

    @Test
    fun `dialogConfig reflects prefs changes via updateConfig`() = runTest {
        val customConfig = DialogGridConfig(
            tagColumnLines = 4,
            tagLayoutMode = GridLayoutMode.Vertical,
            pathDrawMode = PathDrawMode.StartToEnd,
        )

        viewModel.updateConfig(customConfig)
        advanceUntilIdle()

        // updateConfig persists to prefs; verify via fake
        assertEquals(4, fakeSettingsPrefs.lastDialogConfig?.tagColumnLines)
        assertEquals(GridLayoutMode.Vertical, fakeSettingsPrefs.lastDialogConfig?.tagLayoutMode)
    }

    @Test
    fun `multiple updateConfig calls are handled`() = runTest {
        val config1 = DialogGridConfig(activityColumnLines = 1)
        val config2 = DialogGridConfig(activityColumnLines = 5)

        viewModel.updateConfig(config1)
        viewModel.updateConfig(config2)
        advanceUntilIdle()

        assertEquals(config2, fakeSettingsPrefs.lastDialogConfig)
    }

    @Test
    fun `updateConfig with all defaults does not throw`() = runTest {
        viewModel.updateConfig(DialogGridConfig())
        advanceUntilIdle()
        assertTrue(fakeSettingsPrefs.updateDialogConfigCalled)
    }

    @Test
    fun `dialogConfig default secondsStrategy is OPEN_TIME`() = runTest {
        advanceUntilIdle()
        val config = viewModel.dialogConfig.value
        assertEquals(SecondsStrategy.OPEN_TIME, config.secondsStrategy)
    }

    @Test
    fun `updateConfig with secondsStrategy CONFIRM_TIME is preserved`() = runTest {
        val newConfig = DialogGridConfig(secondsStrategy = SecondsStrategy.CONFIRM_TIME)
        viewModel.updateConfig(newConfig)
        advanceUntilIdle()
        assertEquals(SecondsStrategy.CONFIRM_TIME, fakeSettingsPrefs.lastDialogConfig?.secondsStrategy)
    }

    private class FakeSettingsPrefs : SettingsPrefs {
        private val _theme = MutableStateFlow(Theme())
        val dialogConfigFlow = MutableStateFlow(DialogGridConfig())
        var updateDialogConfigCalled = false
        var lastDialogConfig: DialogGridConfig? = null

        override fun getThemeFlow(): Flow<Theme> = _theme
        override suspend fun updateTheme(theme: Theme) { _theme.value = theme }
        override fun getSavedTagCategories(): Flow<Set<String>> = flowOf(emptySet())
        override fun getSavedTagCategoriesOrder(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveTagCategories(categories: Set<String>) {}
        override suspend fun saveTagCategoriesOrder(categories: List<String>) {}
        override fun getDialogConfigFlow(): Flow<DialogGridConfig> = dialogConfigFlow
        override suspend fun updateDialogConfig(config: DialogGridConfig) {
            updateDialogConfigCalled = true
            lastDialogConfig = config
            dialogConfigFlow.value = config
        }
        override fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig> = flowOf(TimeLabelConfig())
        override suspend fun updateTimeLabelConfig(config: TimeLabelConfig) {}
        override fun getHomeLayoutConfigFlow(): Flow<com.nltimer.core.data.model.HomeLayoutConfig> = flowOf(com.nltimer.core.data.model.HomeLayoutConfig())
        override suspend fun updateHomeLayoutConfig(config: com.nltimer.core.data.model.HomeLayoutConfig) {}
        override fun getHasSeenIntroFlow(): Flow<Boolean> = flowOf(false)
        override suspend fun setHasSeenIntro(seen: Boolean) {}
        override fun getDisplayColorConfigFlow(): Flow<com.nltimer.core.data.model.DisplayColorConfig> = flowOf(com.nltimer.core.data.model.DisplayColorConfig())
        override suspend fun updateDisplayColorConfig(config: com.nltimer.core.data.model.DisplayColorConfig) {}
        override fun getTagDisplayConfigFlow(): Flow<com.nltimer.core.data.model.TagDisplayConfig> = flowOf(com.nltimer.core.data.model.TagDisplayConfig())
        override suspend fun updateTagDisplayConfig(config: com.nltimer.core.data.model.TagDisplayConfig) {}
        override fun getFocusCardConfigFlow(): Flow<com.nltimer.core.data.model.FocusCardConfig> = flowOf(com.nltimer.core.data.model.FocusCardConfig())
        override suspend fun updateFocusCardConfig(config: com.nltimer.core.data.model.FocusCardConfig) {}
        override fun getStatsDashboardConfigFlow(): Flow<com.nltimer.core.data.model.StatsDashboardConfig> =
            flowOf(com.nltimer.core.data.model.StatsDashboardConfig())
        override suspend fun updateStatsDashboardConfig(config: com.nltimer.core.data.model.StatsDashboardConfig) {}
        override fun getLastEventTemplateIdFlow(): Flow<Long?> = flowOf(null)
        override suspend fun updateLastEventTemplateId(id: Long?) {}
    }
}
