package com.nltimer.feature.settings.ui

import androidx.compose.ui.graphics.Color
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.designsystem.theme.AppTheme
import com.nltimer.core.designsystem.theme.Fonts
import com.nltimer.core.designsystem.theme.HomeLayout
import com.nltimer.core.designsystem.theme.PaletteStyle
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settingsPrefs: FakeSettingsPrefs
    private lateinit var viewModel: ThemeSettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsPrefs = FakeSettingsPrefs()
        viewModel = ThemeSettingsViewModel(settingsPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `theme state reflects settingsPrefs`() = runTest {
        val initialTheme = Theme(isAmoled = false, appTheme = AppTheme.SYSTEM)
        settingsPrefs.currentTheme.value = initialTheme
        
        // Start collection
        val job = launch(UnconfinedTestDispatcher()) { viewModel.theme.collect() }
        
        val newTheme = Theme(isAmoled = true, appTheme = AppTheme.DARK)
        settingsPrefs.currentTheme.value = newTheme
        
        advanceUntilIdle()
        assertEquals(newTheme, viewModel.theme.value)
        job.cancel()
    }

    @Test
    fun `onSeedColorChange updates theme`() = runTest {
        val job = launch(UnconfinedTestDispatcher()) { viewModel.theme.collect() }
        
        val color = Color.Red
        viewModel.onSeedColorChange(color)
        advanceUntilIdle()
        assertEquals(color, viewModel.theme.value.seedColor)
        job.cancel()
    }

    @Test
    fun `onThemeSwitch updates theme`() = runTest {
        viewModel.onThemeSwitch(AppTheme.DARK)
        advanceUntilIdle()
        assertEquals(AppTheme.DARK, settingsPrefs.currentTheme.value.appTheme)
    }

    @Test
    fun `onAmoledSwitch updates theme`() = runTest {
        viewModel.onAmoledSwitch(true)
        advanceUntilIdle()
        assertEquals(true, settingsPrefs.currentTheme.value.isAmoled)
    }

    @Test
    fun `onPaletteChange updates theme`() = runTest {
        viewModel.onPaletteChange(PaletteStyle.VIBRANT)
        advanceUntilIdle()
        assertEquals(PaletteStyle.VIBRANT, settingsPrefs.currentTheme.value.paletteStyle)
    }

    @Test
    fun `onMaterialYouToggle updates theme`() = runTest {
        viewModel.onMaterialYouToggle(true)
        advanceUntilIdle()
        assertEquals(true, settingsPrefs.currentTheme.value.isMaterialYou)
    }

    @Test
    fun `onFontChange updates theme`() = runTest {
        viewModel.onFontChange(Fonts.SYSTEM_DEFAULT)
        advanceUntilIdle()
        assertEquals(Fonts.SYSTEM_DEFAULT, settingsPrefs.currentTheme.value.font)
    }

    @Test
    fun `onShowBordersToggle updates theme`() = runTest {
        viewModel.onShowBordersToggle(false)
        advanceUntilIdle()
        assertEquals(false, settingsPrefs.currentTheme.value.showBorders)
    }

    @Test
    fun `onHomeLayoutChange updates theme`() = runTest {
        viewModel.onHomeLayoutChange(HomeLayout.TIMELINE_REVERSE)
        advanceUntilIdle()
        assertEquals(HomeLayout.TIMELINE_REVERSE, settingsPrefs.currentTheme.value.homeLayout)
    }

    @Test
    fun `onShowTimeSideBarToggle updates theme`() = runTest {
        viewModel.onShowTimeSideBarToggle(true)
        advanceUntilIdle()
        assertEquals(true, settingsPrefs.currentTheme.value.showTimeSideBar)
    }

    @Test
    fun `theme state initial value is default Theme`() = runTest {
        val job = launch(UnconfinedTestDispatcher()) { viewModel.theme.collect() }
        advanceUntilIdle()
        assertEquals(Theme(), viewModel.theme.value)
        job.cancel()
    }

    private class FakeSettingsPrefs : SettingsPrefs {
        val currentTheme = MutableStateFlow(Theme())

        override fun getThemeFlow(): Flow<Theme> = currentTheme
        override suspend fun updateTheme(theme: Theme) {
            currentTheme.value = theme
        }
        override fun getSavedTagCategories(): Flow<Set<String>> = flowOf(emptySet())
        override fun getSavedTagCategoriesOrder(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun saveTagCategories(categories: Set<String>) {}
        override suspend fun saveTagCategoriesOrder(categories: List<String>) {}
        override fun getDialogConfigFlow(): Flow<DialogGridConfig> = flowOf(DialogGridConfig())

        override suspend fun updateDialogConfig(config: DialogGridConfig) {}

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
