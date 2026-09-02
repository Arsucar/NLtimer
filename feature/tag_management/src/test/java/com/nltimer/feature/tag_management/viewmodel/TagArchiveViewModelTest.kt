package com.nltimer.feature.tag_management.viewmodel

import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.TagRepository
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
class TagArchiveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: TagArchiveViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tagRepository = FakeTagRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load archived list keeps category and archivedAt`() = runTest {
        tagRepository.emitArchived(
            listOf(
                sampleTag(
                    id = 1L,
                    name = "已归档标签",
                    isArchived = true,
                    category = "专注",
                    archivedAt = 1_700_000_000_000L,
                ),
            ),
        )
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(1, uiState.groups.size)
        val item = uiState.groups.single().items.single()
        assertEquals("已归档标签", item.name)
        assertEquals("专注", item.category)
        assertEquals(1_700_000_000_000L, item.archivedAt)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `blank category groups under 默认`() = runTest {
        tagRepository.emitArchived(
            listOf(
                sampleTag(id = 1L, name = "无分类", isArchived = true, category = null, archivedAt = null),
                sampleTag(id = 2L, name = "空分类", isArchived = true, category = "  ", archivedAt = 10L),
            ),
        )
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        val group = viewModel.uiState.value.groups.single()
        assertEquals("默认", group.name)
        assertEquals(listOf("空分类", "无分类"), group.items.map { it.name })
    }

    @Test
    fun `same category clustered in one group`() = runTest {
        tagRepository.emitArchived(
            listOf(
                sampleTag(id = 1L, name = "文学A", isArchived = true, category = "轻小说", archivedAt = 1L),
                sampleTag(id = 2L, name = "标记A", isArchived = true, category = "标记", archivedAt = 2L),
                sampleTag(id = 3L, name = "文学B", isArchived = true, category = "轻小说", archivedAt = 3L),
            ),
        )
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        val groups = viewModel.uiState.value.groups
        assertEquals(listOf("轻小说", "标记"), groups.map { it.name })
        assertEquals(listOf("文学B", "文学A"), groups[0].items.map { it.name })
        assertEquals(listOf("标记A"), groups[1].items.map { it.name })
    }

    @Test
    fun `within group later archivedAt comes first and nulls last`() = runTest {
        tagRepository.emitArchived(
            listOf(
                sampleTag(id = 1L, name = "旧", isArchived = true, category = "记录", archivedAt = 100L),
                sampleTag(id = 2L, name = "无日期", isArchived = true, category = "记录", archivedAt = null),
                sampleTag(id = 3L, name = "新", isArchived = true, category = "记录", archivedAt = 300L),
            ),
        )
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        assertEquals(
            listOf("新", "旧", "无日期"),
            viewModel.uiState.value.groups.single().items.map { it.name },
        )
    }

    @Test
    fun `默认 group is first then others by newest archive`() = runTest {
        tagRepository.emitArchived(
            listOf(
                sampleTag(id = 1L, name = "默认旧", isArchived = true, category = null, archivedAt = 1L),
                sampleTag(id = 2L, name = "简单事件", isArchived = true, category = "简单事件", archivedAt = 50L),
                sampleTag(id = 3L, name = "轻小说", isArchived = true, category = "轻小说", archivedAt = 100L),
            ),
        )
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        assertEquals(
            listOf("默认", "轻小说", "简单事件"),
            viewModel.uiState.value.groups.map { it.name },
        )
    }

    @Test
    fun `empty archived list`() = runTest {
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.groups.isEmpty())
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `restore calls setArchived false`() = runTest {
        tagRepository.emitArchived(listOf(sampleTag(id = 3L, name = "专注", isArchived = true)))
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        viewModel.restore(3L)
        advanceUntilIdle()

        assertEquals(3L, tagRepository.archivedId)
        assertEquals(false, tagRepository.archivedValue)
        assertEquals("已恢复「专注」", viewModel.snackbarMessage.value)
    }

    @Test
    fun `restore missing id shows failure snackbar`() = runTest {
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        viewModel.restore(999L)
        advanceUntilIdle()

        assertNull(tagRepository.archivedId)
        assertEquals("恢复失败", viewModel.snackbarMessage.value)
    }

    @Test
    fun `error path sets error message`() = runTest {
        tagRepository.failLoad = true
        viewModel = TagArchiveViewModel(tagRepository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("加载失败", uiState.errorMessage)
    }

    private fun sampleTag(
        id: Long,
        name: String,
        isArchived: Boolean,
        category: String? = null,
        archivedAt: Long? = if (isArchived) 1000L else null,
        archiveNote: String? = null,
    ) = Tag(
        id = id,
        name = name,
        color = null,
        iconKey = null,
        category = category,
        groupId = null,
        priority = 0,
        usageCount = 0,
        sortOrder = 0,
        keywords = null,
        isArchived = isArchived,
        archivedAt = archivedAt,
        archiveNote = archiveNote,
    )

    private class FakeTagRepository : TagRepository {
        private val _archived = MutableStateFlow<List<Tag>>(emptyList())
        var failLoad = false
        var archivedId: Long? = null
        var archivedValue: Boolean? = null

        fun emitArchived(items: List<Tag>) {
            _archived.value = items
        }

        override fun getAllActive(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getAll(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getArchived(): Flow<List<Tag>> =
            if (failLoad) flow { throw RuntimeException("boom") } else _archived
        override fun getByCategory(category: String): Flow<List<Tag>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<Tag>> = flowOf(emptyList())
        override fun getByActivityId(activityId: Long): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun getById(id: Long): Tag? = null
        override suspend fun getByName(name: String): Tag? = null
        override suspend fun insert(tag: Tag): Long = 1L
        override suspend fun update(tag: Tag) {}
        override suspend fun setArchived(id: Long, archived: Boolean) {
            archivedId = id
            archivedValue = archived
            if (!archived) {
                _archived.value = _archived.value.filterNot { it.id == id }
            }
        }
        override fun getDistinctCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun renameCategory(oldName: String, newName: String) {}
        override suspend fun resetCategory(category: String) {}
        override suspend fun getActivityIdsForTag(tagId: Long): List<Long> = emptyList()
        override suspend fun setActivityTagBindings(tagId: Long, activityIds: List<Long>) {}
    }
}
