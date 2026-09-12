package com.nltimer.feature.categories.viewmodel

import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.model.DialogGridConfig
import com.nltimer.core.data.repository.CategoryRepository
import com.nltimer.core.designsystem.theme.Theme
import com.nltimer.core.designsystem.theme.TimeLabelConfig
import com.nltimer.feature.categories.model.CategoriesUiState
import com.nltimer.feature.categories.model.DialogState
import com.nltimer.feature.categories.model.SectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityCategoriesFlow: MutableStateFlow<List<String>>
    private lateinit var tagCategoriesFlow: MutableStateFlow<List<String>>
    private lateinit var repository: FakeCategoryRepository
    private lateinit var settingsPrefs: FakeSettingsPrefs
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        activityCategoriesFlow = MutableStateFlow(emptyList())
        tagCategoriesFlow = MutableStateFlow(emptyList())
        repository = FakeCategoryRepository(activityCategoriesFlow, tagCategoriesFlow)
        settingsPrefs = FakeSettingsPrefs()
        viewModel = CategoriesViewModel(repository, settingsPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyGroupsWithBothExpanded() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.groups.size)
        assertTrue(state.groups.all { it.items.isEmpty() })
        assertTrue(state.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_ACTIVITY))
        assertTrue(state.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_TAG))
        assertNull(state.dialogState)
    }

    @Test
    fun groupsPopulate_fromRepositoryFlows() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动", "学习")
        tagCategoriesFlow.value = listOf("优先级", "状态")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.groups.size)
        val activityGroup = state.groups.first { it.type == SectionType.ACTIVITY }
        val tagGroup = state.groups.first { it.type == SectionType.TAG }
        assertEquals(listOf("学习", "运动"), activityGroup.items.map { it.name })
        assertEquals(listOf("优先级", "状态"), tagGroup.items.map { it.name })
    }

    @Test
    fun showAddCategoryDialog_setsCorrectDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showAddCategoryDialog(SectionType.ACTIVITY)
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.dialogState as? DialogState.AddCategory
        assertNotNull(dialog)
        assertEquals(SectionType.ACTIVITY, dialog!!.sectionType)
    }

    @Test
    fun showAddCategoryDialog_forTag_setsCorrectDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showAddCategoryDialog(SectionType.TAG)
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.dialogState as? DialogState.AddCategory
        assertNotNull(dialog)
        assertEquals(SectionType.TAG, dialog!!.sectionType)
    }

    @Test
    fun showRenameCategoryDialog_setsCorrectDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.dialogState as? DialogState.RenameCategory
        assertNotNull(dialog)
        assertEquals("运动", dialog!!.oldName)
        assertEquals(SectionType.ACTIVITY, dialog.sectionType)
    }

    @Test
    fun showRenameCategoryDialog_forTag_setsCorrectDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.TAG, "标签A")
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.dialogState as? DialogState.RenameCategory
        assertNotNull(dialog)
        assertEquals("标签A", dialog!!.oldName)
        assertEquals(SectionType.TAG, dialog.sectionType)
    }

    @Test
    fun showDeleteCategoryDialog_setsCorrectDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showDeleteCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.dialogState as? DialogState.DeleteCategory
        assertNotNull(dialog)
        assertEquals("运动", dialog!!.category)
        assertEquals(SectionType.ACTIVITY, dialog.sectionType)
    }

    @Test
    fun showDeleteCategoryDialog_forTag_setsCorrectDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showDeleteCategoryDialog(SectionType.TAG, "优先级")
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.dialogState as? DialogState.DeleteCategory
        assertNotNull(dialog)
        assertEquals("优先级", dialog!!.category)
        assertEquals(SectionType.TAG, dialog.sectionType)
    }

    @Test
    fun dismissDialog_clearsDialogState() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showAddCategoryDialog(SectionType.ACTIVITY)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.dialogState)
        viewModel.dismissDialog()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dialogState)
    }

    @Test
    fun confirmAddCategory_emptyName_dismissesWithoutCallingRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showAddCategoryDialog(SectionType.ACTIVITY)
        advanceUntilIdle()
        viewModel.confirmAddCategory(SectionType.ACTIVITY, "  ")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dialogState)
        assertFalse(repository.addActivityCategoryCalled)
    }

    @Test
    fun confirmAddCategory_forActivity_callsRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.confirmAddCategory(SectionType.ACTIVITY, "新分类")
        advanceUntilIdle()
        assertTrue(repository.addActivityCategoryCalled)
        assertEquals("新分类", repository.lastAddedActivityCategory)
        assertNull(viewModel.uiState.value.dialogState)
    }

    @Test
    fun confirmAddCategory_forTag_updatesSettingsPrefs() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.confirmAddCategory(SectionType.TAG, "我的标签")
        advanceUntilIdle()
        assertTrue(settingsPrefs.saveTagCategoriesCalled)
        assertEquals(setOf("我的标签"), settingsPrefs.lastSavedTagCategories)
        val tagGroup = viewModel.uiState.value.groups.first { it.type == SectionType.TAG }
        assertTrue(tagGroup.items.any { it.name == "我的标签" })
    }

    @Test
    fun confirmRenameCategory_sameName_dismissesWithoutCallingRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.ACTIVITY, "运动", "运动")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dialogState)
        assertFalse(repository.renameActivityCategoryCalled)
    }

    @Test
    fun confirmRenameCategory_withConflict_setsRenameConflict() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动", "学习")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.ACTIVITY, "运动", "学习")
        advanceUntilIdle()
        assertEquals("学习", viewModel.renameConflict.value)
        assertTrue(repository.renameActivityCategoryCalled.not())
    }

    @Test
    fun confirmRenameCategory_withoutConflict_callsRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.ACTIVITY, "运动", "体育")
        advanceUntilIdle()
        assertTrue(repository.renameActivityCategoryCalled)
        assertEquals("运动" to "体育", repository.lastRenameActivityPair)
        assertNull(viewModel.uiState.value.dialogState)
    }

    @Test
    fun confirmRenameCategory_forTag_withLocalTag_updatesSettingsPrefs() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        settingsPrefs.savedTagCategories = setOf("旧标签")
        viewModel = CategoriesViewModel(repository, settingsPrefs)
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.TAG, "旧标签")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.TAG, "旧标签", "新标签")
        advanceUntilIdle()
        assertTrue(settingsPrefs.saveTagCategoriesCalled)
        assertEquals(setOf("新标签"), settingsPrefs.lastSavedTagCategories)
        assertTrue(repository.renameTagCategoryCalled)
    }

    @Test
    fun confirmRenameCategory_forTag_conflict_setsConflictFlag() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        tagCategoriesFlow.value = listOf("标签A", "标签B")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.TAG, "标签A")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.TAG, "标签A", "标签B")
        advanceUntilIdle()
        assertEquals("标签B", viewModel.renameConflict.value)
    }

    @Test
    fun confirmRenameCategory_forTag_noConflict_callsRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        tagCategoriesFlow.value = listOf("标签A")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.TAG, "标签A")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.TAG, "标签A", "新标签")
        advanceUntilIdle()
        assertTrue(repository.renameTagCategoryCalled)
        assertNull(viewModel.uiState.value.dialogState)
    }

    @Test
    fun confirmDeleteCategory_forActivity_callsRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.confirmDeleteCategory(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        assertTrue(repository.resetActivityCategoryCalled)
        assertEquals("运动", repository.lastResetActivityCategory)
    }

    @Test
    fun confirmDeleteCategory_forTag_updatesSettingsPrefsAndCallsRepository() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        settingsPrefs.savedTagCategories = setOf("待删除", "保留")
        viewModel = CategoriesViewModel(repository, settingsPrefs)
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        viewModel.confirmDeleteCategory(SectionType.TAG, "待删除")
        advanceUntilIdle()
        assertTrue(repository.resetTagCategoryCalled)
        assertTrue(settingsPrefs.saveTagCategoriesCalled)
        assertEquals(setOf("保留"), settingsPrefs.lastSavedTagCategories)
    }

    @Test
    fun clearConflict_resetsRenameConflict() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动", "学习")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.ACTIVITY, "运动", "学习")
        advanceUntilIdle()
        assertEquals("学习", viewModel.renameConflict.value)
        viewModel.clearConflict()
        advanceUntilIdle()
        assertNull(viewModel.renameConflict.value)
    }

    @Test
    fun toggleGroupExpand_addsAndRemovesGroupIds() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_ACTIVITY))
        viewModel.toggleGroupExpand(CategoriesViewModel.GROUP_ID_ACTIVITY)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_ACTIVITY))
        assertTrue(viewModel.uiState.value.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_TAG))
        viewModel.toggleGroupExpand(CategoriesViewModel.GROUP_ID_ACTIVITY)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_ACTIVITY))
    }

    @Test
    fun setAllGroupsExpanded_true_setsAllGroupIds() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动")
        tagCategoriesFlow.value = listOf("优先级")
        advanceUntilIdle()
        viewModel.toggleGroupExpand(CategoriesViewModel.GROUP_ID_ACTIVITY)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.expandedGroupIds.contains(CategoriesViewModel.GROUP_ID_ACTIVITY))
        viewModel.setAllGroupsExpanded(true)
        advanceUntilIdle()
        val expanded = viewModel.uiState.value.expandedGroupIds
        assertTrue(expanded.contains(CategoriesViewModel.GROUP_ID_ACTIVITY))
        assertTrue(expanded.contains(CategoriesViewModel.GROUP_ID_TAG))
    }

    @Test
    fun setAllGroupsExpanded_false_clearsAllGroupIds() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedGroupIds.isNotEmpty())
        viewModel.setAllGroupsExpanded(false)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedGroupIds.isEmpty())
    }

    @Test
    fun dismissDialog_clearsRenameConflict() = runTest {
        viewModel.uiState.launchIn(backgroundScope)
        activityCategoriesFlow.value = listOf("运动", "学习")
        advanceUntilIdle()
        viewModel.showRenameCategoryDialog(SectionType.ACTIVITY, "运动")
        advanceUntilIdle()
        viewModel.confirmRenameCategory(SectionType.ACTIVITY, "运动", "学习")
        advanceUntilIdle()
        assertEquals("学习", viewModel.renameConflict.value)
        viewModel.dismissDialog()
        advanceUntilIdle()
        assertNull(viewModel.renameConflict.value)
    }

    private class FakeSettingsPrefs : SettingsPrefs {

        var savedTagCategories: Set<String> = emptySet()
        var lastSavedTagCategories: Set<String>? = null
        var saveTagCategoriesCalled = false

        override fun getThemeFlow(): Flow<Theme> = flowOf(Theme())
        override suspend fun updateTheme(theme: Theme) {}
        override fun getSavedTagCategories(): Flow<Set<String>> = MutableStateFlow(savedTagCategories)
        override fun getSavedTagCategoriesOrder(): Flow<List<String>> = MutableStateFlow(savedTagCategories.toList())
        override suspend fun saveTagCategories(categories: Set<String>) {
            saveTagCategoriesCalled = true
            lastSavedTagCategories = categories
        }
        override suspend fun saveTagCategoriesOrder(categories: List<String>) {
            saveTagCategoriesCalled = true
            lastSavedTagCategories = categories.toSet()
        }
        override fun getDialogConfigFlow(): Flow<DialogGridConfig> = flowOf(DialogGridConfig())
        override suspend fun updateDialogConfig(config: DialogGridConfig) {}
        override fun getTimeLabelConfigFlow(): Flow<TimeLabelConfig> = flowOf(TimeLabelConfig())
        override suspend fun updateTimeLabelConfig(config: TimeLabelConfig) {}
        override fun getHasSeenIntroFlow(): Flow<Boolean> = flowOf(false)
        override suspend fun setHasSeenIntro(seen: Boolean) {}
        override fun getHomeLayoutConfigFlow(): Flow<com.nltimer.core.data.model.HomeLayoutConfig> = flowOf(com.nltimer.core.data.model.HomeLayoutConfig())
        override suspend fun updateHomeLayoutConfig(config: com.nltimer.core.data.model.HomeLayoutConfig) {}
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

    private class FakeCategoryRepository(
        private val activityCategories: MutableStateFlow<List<String>>,
        private val tagCategories: MutableStateFlow<List<String>>,
    ) : CategoryRepository {

        var addActivityCategoryCalled = false
        var renameActivityCategoryCalled = false
        var renameTagCategoryCalled = false
        var resetActivityCategoryCalled = false
        var resetTagCategoryCalled = false
        var lastAddedActivityCategory: String? = null
        var lastRenameActivityPair: Pair<String, String>? = null
        var lastResetActivityCategory: String? = null

        override fun getDistinctActivityCategories(parent: String?) =
            activityCategories.map { it.sorted() }
        override fun getDistinctTagCategories(parent: String?) = tagCategories

        override suspend fun addActivityCategory(name: String) {
            addActivityCategoryCalled = true
            lastAddedActivityCategory = name
        }

        override suspend fun renameActivityCategory(oldName: String, newName: String, parent: String?) {
            renameActivityCategoryCalled = true
            lastRenameActivityPair = oldName to newName
        }

        override suspend fun resetActivityCategory(category: String) {
            resetActivityCategoryCalled = true
            lastResetActivityCategory = category
        }

        override suspend fun renameTagCategory(oldName: String, newName: String, parent: String?) {
            renameTagCategoryCalled = true
        }

        override suspend fun resetTagCategory(category: String) {
            resetTagCategoryCalled = true
        }
        override suspend fun addTagCategoryStub(name: String): Boolean = true
    }
}
