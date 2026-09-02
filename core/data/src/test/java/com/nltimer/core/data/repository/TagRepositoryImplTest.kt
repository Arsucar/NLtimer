package com.nltimer.core.data.repository

import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.TagEntity
import com.nltimer.core.data.database.NLtimerDatabase
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.impl.TagRepositoryImpl
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
class TagRepositoryImplTest {

    private val tagEntities = mutableListOf<TagEntity>()
    private val tagFlow = MutableStateFlow<List<TagEntity>>(emptyList())

    private val fakeTagDao = object : TagDao {
        override suspend fun insert(tag: TagEntity): Long {
            val id = tagEntities.size.toLong() + 1
            tagEntities.add(tag.copy(id = id))
            tagFlow.value = tagEntities.toList()
            return id
        }

        override suspend fun update(tag: TagEntity) {
            tagEntities.replaceAll { if (it.id == tag.id) tag else it }
            tagFlow.value = tagEntities.toList()
        }

        override suspend fun delete(tag: TagEntity) {
            tagEntities.removeAll { it.id == tag.id }
            tagFlow.value = tagEntities.toList()
        }

        override fun getAllActive(): Flow<List<TagEntity>> =
            tagFlow.map { list -> list.filter { !it.isArchived } }

        override fun getAll(): Flow<List<TagEntity>> = tagFlow

        override fun getArchived(): Flow<List<TagEntity>> =
            tagFlow.map { list ->
                list.filter { it.isArchived }
                    .sortedWith(compareByDescending<TagEntity> { it.archivedAt ?: Long.MIN_VALUE }.thenBy { it.name })
            }

        override fun getByCategory(category: String): Flow<List<TagEntity>> =
            tagFlow.map { list -> list.filter { it.category == category } }

        override fun search(query: String): Flow<List<TagEntity>> =
            tagFlow.map { list -> list.filter { it.name.contains(query) } }

        override fun getByActivityId(activityId: Long): Flow<List<TagEntity>> = flowOf(emptyList())

        override suspend fun getById(id: Long): TagEntity? =
            tagEntities.find { it.id == id }

        override suspend fun getByName(name: String): TagEntity? =
            tagEntities.find { it.name == name }

        override suspend fun setArchived(id: Long, archived: Boolean, now: Long) {
            tagEntities.replaceAll {
                if (it.id == id) {
                    it.copy(isArchived = archived, archivedAt = if (archived) now else null)
                } else {
                    it
                }
            }
            tagFlow.value = tagEntities.toList()
        }

        override fun getDistinctCategories(): Flow<List<String>> =
            tagFlow.map { entities ->
                entities.mapNotNull { it.category }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            }

        override suspend fun renameCategory(oldName: String, newName: String) {
            tagEntities.replaceAll {
                if (it.category == oldName) it.copy(category = newName) else it
            }
            tagFlow.value = tagEntities.toList()
        }

        override suspend fun resetCategory(category: String) {
            tagEntities.replaceAll {
                if (it.category == category) it.copy(category = null) else it
            }
            tagFlow.value = tagEntities.toList()
        }

        override suspend fun getTagsForBehaviorSync(behaviorId: Long): List<TagEntity> = emptyList()

        override suspend fun deleteAll() {
            tagEntities.clear()
            tagFlow.value = emptyList()
        }

        override suspend fun getDistinctCategoriesSync(): List<String> = emptyList()
        override suspend fun getActivityIdsForTagSync(tagId: Long): List<Long> = emptyList()
        override suspend fun insertActivityTagBinding(binding: ActivityTagBindingEntity) {}
        override suspend fun insertActivityTagBindings(bindings: List<ActivityTagBindingEntity>) {}
        override suspend fun deleteActivityTagBindingsForTag(tagId: Long) {}
        override suspend fun getAllDistinctSync(): List<TagEntity> = emptyList()
    }

    private val fakeDatabase: NLtimerDatabase = mockk<NLtimerDatabase>(relaxed = true).also {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { it.withTransaction(any<suspend () -> Unit>()) } coAnswers { (args[0] as suspend () -> Unit).invoke() }
    }

    private val repository = TagRepositoryImpl(fakeTagDao, fakeDatabase)

    @Test
    fun `getAllActive returns only non-archived tags`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", isArchived = false))
        fakeTagDao.insert(TagEntity(name = "标签B", isArchived = true))

        val result = repository.getAllActive().first()

        assertEquals(1, result.size)
        assertEquals("标签A", result[0].name)
    }

    @Test
    fun `getAll returns all tags including archived`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", isArchived = false))
        fakeTagDao.insert(TagEntity(name = "标签B", isArchived = true))

        val result = repository.getAll().first()

        assertEquals(2, result.size)
    }

    @Test
    fun `getByCategory filters by category`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", category = "分类A"))
        fakeTagDao.insert(TagEntity(name = "标签B", category = "分类B"))

        val result = repository.getByCategory("分类A").first()

        assertEquals(1, result.size)
        assertEquals("标签A", result[0].name)
    }

    @Test
    fun `search filters by query`() = runTest {
        fakeTagDao.insert(TagEntity(name = "搜索测试"))
        fakeTagDao.insert(TagEntity(name = "其他"))

        val result = repository.search("搜索").first()

        assertEquals(1, result.size)
        assertEquals("搜索测试", result[0].name)
    }

    @Test
    fun `getById returns tag when found`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A"))

        val result = repository.getById(1L)

        assertEquals("标签A", result?.name)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        val result = repository.getById(999L)
        assertNull(result)
    }

    @Test
    fun `getByName returns tag when found`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A"))

        val result = repository.getByName("标签A")

        assertEquals("标签A", result?.name)
    }

    @Test
    fun `getByName returns null when not found`() = runTest {
        val result = repository.getByName("不存在")
        assertNull(result)
    }

    @Test
    fun `insert adds tag and returns id`() = runTest {
        val tag = Tag(
            id = 0,
            name = "新标签",
            color = null,
            iconKey = null,
            category = null,
            groupId = null,
            priority = 0,
            usageCount = 0,
            sortOrder = 0,
            keywords = null,
            isArchived = false,
        )

        val id = repository.insert(tag)

        assertEquals(1L, id)
        val all = repository.getAll().first()
        assertEquals(1, all.size)
        assertEquals("新标签", all[0].name)
    }

    @Test
    fun `update modifies existing tag`() = runTest {
        fakeTagDao.insert(TagEntity(name = "旧名称"))
        val updated = Tag(
            id = 1L,
            name = "新名称",
            color = null,
            iconKey = null,
            category = null,
            groupId = null,
            priority = 0,
            usageCount = 0,
            sortOrder = 0,
            keywords = null,
            isArchived = false,
        )

        repository.update(updated)

        val result = repository.getById(1L)
        assertEquals("新名称", result?.name)
    }

    @Test
    fun `setArchived updates archive status`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", isArchived = false))

        repository.setArchived(1L, true)

        val result = repository.getById(1L)
        assertTrue(result?.isArchived == true)
        assertTrue(result?.archivedAt != null)
    }

    @Test
    fun `getArchived returns only archived tags`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", isArchived = false))
        fakeTagDao.insert(TagEntity(name = "标签B", isArchived = true, archivedAt = 2000L))

        val result = repository.getArchived().first()

        assertEquals(1, result.size)
        assertEquals("标签B", result[0].name)
    }

    @Test
    fun `setArchived true sets archivedAt non-null`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", isArchived = false, archivedAt = null))

        repository.setArchived(1L, true)

        val result = repository.getById(1L)
        assertTrue(result?.isArchived == true)
        assertTrue(result?.archivedAt != null)
    }

    @Test
    fun `setArchived false clears archivedAt and item leaves getArchived`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", isArchived = true, archivedAt = 1000L))

        repository.setArchived(1L, false)

        val restored = repository.getById(1L)
        assertFalse(restored?.isArchived == true)
        assertNull(restored?.archivedAt)
        assertTrue(repository.getArchived().first().isEmpty())
    }

    @Test
    fun `getDistinctCategories returns sorted unique categories`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", category = "分类B"))
        fakeTagDao.insert(TagEntity(name = "标签B", category = "分类A"))
        fakeTagDao.insert(TagEntity(name = "标签C", category = "分类B"))
        fakeTagDao.insert(TagEntity(name = "标签D", category = null))
        fakeTagDao.insert(TagEntity(name = "标签E", category = ""))

        val result = repository.getDistinctCategories().first()

        assertEquals(listOf("分类A", "分类B"), result)
    }

    @Test
    fun `renameCategory updates all matching tags`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", category = "旧分类"))
        fakeTagDao.insert(TagEntity(name = "标签B", category = "旧分类"))
        fakeTagDao.insert(TagEntity(name = "标签C", category = "其他"))

        repository.renameCategory("旧分类", "新分类")

        val result = repository.getAll().first()
        assertEquals("新分类", result[0].category)
        assertEquals("新分类", result[1].category)
        assertEquals("其他", result[2].category)
    }

    @Test
    fun `resetCategory sets category to null`() = runTest {
        fakeTagDao.insert(TagEntity(name = "标签A", category = "待重置"))
        fakeTagDao.insert(TagEntity(name = "标签B", category = "待重置"))

        repository.resetCategory("待重置")

        val result = repository.getAll().first()
        assertNull(result[0].category)
        assertNull(result[1].category)
    }

    @Test
    fun `entity to model mapping preserves all fields`() = runTest {
        fakeTagDao.insert(
            TagEntity(
                name = "完整标签",
                color = 0xFF0000FF,
                iconKey = "icon",
                category = "分类",
                priority = 5,
                usageCount = 10,
                sortOrder = 3,
                isArchived = true,
            )
        )

        val result = repository.getById(1L)

        assertEquals(1L, result?.id)
        assertEquals("完整标签", result?.name)
        assertEquals(0xFF0000FF, result?.color)
        assertEquals("icon", result?.iconKey)
        assertEquals("分类", result?.category)
        assertEquals(5, result?.priority)
        assertEquals(10, result?.usageCount)
        assertEquals(3, result?.sortOrder)
        assertTrue(result?.isArchived == true)
    }
}
