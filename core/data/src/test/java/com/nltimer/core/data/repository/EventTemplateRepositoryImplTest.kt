package com.nltimer.core.data.repository

import androidx.room.withTransaction
import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.EventTemplateDao
import com.nltimer.core.data.database.dao.EventTemplateFieldDao
import com.nltimer.core.data.database.dao.EventTemplateTagBindingDao
import com.nltimer.core.data.database.dao.TemplateTagMatchRow
import com.nltimer.core.data.database.entity.EventTemplateEntity
import com.nltimer.core.data.database.entity.EventTemplateFieldEntity
import com.nltimer.core.data.database.entity.EventTemplateTagBindingEntity
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.impl.EventTemplateRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventTemplateRepositoryImplTest {

    private lateinit var templates: MutableList<EventTemplateEntity>
    private lateinit var fields: MutableList<EventTemplateFieldEntity>
    private lateinit var bindings: MutableList<EventTemplateTagBindingEntity>
    private lateinit var fakeTemplateDao: FakeEventTemplateDao
    private lateinit var fakeFieldDao: FakeEventTemplateFieldDao
    private lateinit var fakeBindingDao: FakeEventTemplateTagBindingDao
    private lateinit var fakeDatabase: NLtimerDatabase
    private lateinit var repository: EventTemplateRepositoryImpl

    @Before
    fun setup() {
        templates = mutableListOf()
        fields = mutableListOf()
        bindings = mutableListOf()
        fakeTemplateDao = FakeEventTemplateDao(templates, bindings)
        fakeFieldDao = FakeEventTemplateFieldDao(fields)
        fakeBindingDao = FakeEventTemplateTagBindingDao(bindings, templates)
        fakeDatabase = mockk<NLtimerDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { fakeDatabase.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            // args[0] = Receiver（mock 数据库本体），args[1] = 事务 block
            (args[1] as suspend () -> Unit).invoke()
        }
        repository = EventTemplateRepositoryImpl(fakeTemplateDao, fakeFieldDao, fakeBindingDao, fakeDatabase)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --- observeAll ---

    @Test
    fun `observeAll maps entities sorted by sortOrder`() = runTest {
        templates.add(EventTemplateEntity(id = 1, name = "模板B", createdAt = 1, sortOrder = 2))
        templates.add(EventTemplateEntity(id = 2, name = "模板A", createdAt = 2, sortOrder = 0))

        val result = repository.observeAll().first()

        assertEquals(2, result.size)
        assertEquals(2L, result[0].id)
        assertEquals("模板A", result[0].name)
        assertEquals(1L, result[1].id)
    }

    // --- getById / getByName ---

    @Test
    fun `getById returns null when absent`() = runTest {
        assertNull(repository.getTemplateById(9L))
    }

    @Test
    fun `getTemplateByName finds template`() = runTest {
        templates.add(EventTemplateEntity(id = 1, name = "晨会", createdAt = 1))

        val result = repository.getTemplateByName("晨会")

        assertEquals(1L, result?.id)
    }

    // --- saveTemplateFields ---

    @Test
    fun `saveTemplateFields replaces whole field list`() = runTest {
        fields.add(EventTemplateFieldEntity(id = 10, templateId = 1, name = "旧字段", type = "text", sortOrder = 0))

        repository.saveTemplateFields(
            1L,
            listOf(
                EventTemplateField(templateId = 1, name = "新字段", type = EventFieldType.NUMBER, sortOrder = 0),
                EventTemplateField(templateId = 1, name = "选项", type = EventFieldType.SELECT, options = listOf("A"), sortOrder = 1),
            ),
        )

        val saved = fields.filter { it.templateId == 1L }
        assertEquals(2, saved.size)
        assertTrue(saved.none { it.name == "旧字段" })
        assertEquals("number", saved.first { it.name == "新字段" }.type)
        assertEquals("""["A"]""", saved.first { it.name == "选项" }.optionsJson)
    }

    @Test
    fun `saveTemplateFields encodes options from model`() = runTest {
        repository.saveTemplateFields(
            1L,
            listOf(
                EventTemplateField(
                    templateId = 1,
                    name = "种类",
                    type = EventFieldType.SELECT,
                    options = listOf("咖啡", "茶"),
                    sortOrder = 0,
                ),
            ),
        )

        val saved = fields.single()
        assertEquals("""["咖啡","茶"]""", saved.optionsJson)
    }

    // --- saveTemplateBindings ---

    @Test
    fun `saveTemplateBindings replaces bindings`() = runTest {
        bindings.add(EventTemplateTagBindingEntity(templateId = 1, tagId = 99))
        bindings.add(EventTemplateTagBindingEntity(templateId = 2, tagId = 99))

        repository.saveTemplateBindings(1L, listOf(1L, 2L))

        assertEquals(3, bindings.size)
        assertTrue(bindings.none { it.templateId == 1L && it.tagId == 99L })
        assertTrue(bindings.any { it.templateId == 2L && it.tagId == 99L })
        assertTrue(bindings.any { it.templateId == 1L && it.tagId == 1L })
        assertTrue(bindings.any { it.templateId == 1L && it.tagId == 2L })
    }

    @Test
    fun `saveTemplateBindings with empty tags clears bindings`() = runTest {
        bindings.add(EventTemplateTagBindingEntity(templateId = 1, tagId = 5))

        repository.saveTemplateBindings(1L, emptyList())

        assertTrue(bindings.none { it.templateId == 1L })
    }

    // --- matchTemplateByTags ---

    @Test
    fun `matchTemplateByTags ranks by matched tag count then sortOrder`() = runTest {
        templates.add(EventTemplateEntity(id = 1, name = "模板一", createdAt = 1, sortOrder = 0))
        templates.add(EventTemplateEntity(id = 2, name = "模板二", createdAt = 2, sortOrder = 1))
        bindings.add(EventTemplateTagBindingEntity(templateId = 1, tagId = 1))
        bindings.add(EventTemplateTagBindingEntity(templateId = 2, tagId = 1))
        bindings.add(EventTemplateTagBindingEntity(templateId = 2, tagId = 2))

        val matched = repository.matchTemplateByTags(listOf(1L, 2L))

        assertEquals(2L, matched?.id)
    }

    @Test
    fun `matchTemplateByTags empty tags returns null`() = runTest {
        assertNull(repository.matchTemplateByTags(emptyList()))
    }

    @Test
    fun `matchTemplateByTags no binding hit returns null`() = runTest {
        templates.add(EventTemplateEntity(id = 1, name = "模板一", createdAt = 1, sortOrder = 0))
        bindings.add(EventTemplateTagBindingEntity(templateId = 1, tagId = 3))

        val matched = repository.matchTemplateByTags(listOf(1L, 2L))

        assertNull(matched)
    }

    // --- binding single ops ---

    @Test
    fun `addTagBinding and removeTagBinding update state`() = runTest {
        repository.addTagBinding(1L, 7L)
        assertTrue(bindings.any { it.templateId == 1L && it.tagId == 7L })

        repository.removeTagBinding(1L, 7L)
        assertTrue(bindings.none { it.templateId == 1L })
    }
}

/** 内存版 EventTemplateDao：observeByTag 依赖共享 bindings 列表模拟绑定表 JOIN */
private class FakeEventTemplateDao(
    private val templates: MutableList<EventTemplateEntity>,
    private val bindings: MutableList<EventTemplateTagBindingEntity>,
) : EventTemplateDao {
    private var nextId = 1L

    private fun insertEntity(template: EventTemplateEntity): EventTemplateEntity {
        val id = if (template.id == 0L) nextId++ else template.id
        val entity = template.copy(id = id)
        templates.removeAll { it.id == id }
        templates.add(entity)
        return entity
    }

    override suspend fun insert(template: EventTemplateEntity): Long {
        val entity = insertEntity(template)
        if (entity.id >= nextId) nextId = entity.id + 1
        return entity.id
    }

    override suspend fun update(template: EventTemplateEntity) {
        val index = templates.indexOfFirst { it.id == template.id }
        if (index >= 0) templates[index] = template
    }

    override suspend fun delete(id: Long) {
        templates.removeAll { it.id == id }
    }

    override fun observeAll(): Flow<List<EventTemplateEntity>> =
        flowOf(templates.sortedBy { it.sortOrder })

    override suspend fun getById(id: Long): EventTemplateEntity? =
        templates.firstOrNull { it.id == id }

    override suspend fun getByName(name: String): EventTemplateEntity? =
        templates.firstOrNull { it.name == name }

    override suspend fun getMaxSortOrder(): Int =
        templates.maxOfOrNull { it.sortOrder } ?: -1

    override fun observeByTag(tagId: Long): Flow<List<EventTemplateEntity>> =
        flowOf(
            bindings.filter { it.tagId == tagId }
                .mapNotNull { binding -> templates.firstOrNull { it.id == binding.templateId } }
                .sortedBy { it.sortOrder }
        )
}

private class FakeEventTemplateFieldDao(
    private val fields: MutableList<EventTemplateFieldEntity>,
) : EventTemplateFieldDao {
    private var nextId = 1L

    override suspend fun insert(field: EventTemplateFieldEntity): Long {
        val id = if (field.id == 0L) nextId++ else field.id
        val entity = field.copy(id = id)
        fields.removeAll { it.id == id }
        fields.add(entity)
        return id
    }

    override suspend fun insertAll(fields: List<EventTemplateFieldEntity>) {
        fields.forEach { insert(it) }
    }

    override suspend fun update(field: EventTemplateFieldEntity) {
        val index = fields.indexOfFirst { it.id == field.id }
        if (index >= 0) fields[index] = field
    }

    override suspend fun delete(id: Long) {
        fields.removeAll { it.id == id }
    }

    override suspend fun deleteByTemplate(templateId: Long) {
        fields.removeAll { it.templateId == templateId }
    }

    override suspend fun deleteExceptIds(templateId: Long, keepIds: List<Long>) {
        if (keepIds.isEmpty()) return
        fields.removeAll { it.templateId == templateId && it.id !in keepIds }
    }

    override fun observeByTemplate(templateId: Long): Flow<List<EventTemplateFieldEntity>> =
        flowOf(fields.filter { it.templateId == templateId }.sortedBy { it.sortOrder })

    override suspend fun getByTemplateSync(templateId: Long): List<EventTemplateFieldEntity> =
        fields.filter { it.templateId == templateId }.sortedBy { it.sortOrder }

    override suspend fun getByTemplateIdsSync(templateIds: List<Long>): List<EventTemplateFieldEntity> =
        fields.filter { it.templateId in templateIds }.sortedBy { it.sortOrder }

    override suspend fun getMaxSortOrder(templateId: Long): Int =
        fields.filter { it.templateId == templateId }.maxOfOrNull { it.sortOrder } ?: -1
}

private class FakeEventTemplateTagBindingDao(
    private val bindings: MutableList<EventTemplateTagBindingEntity>,
    private val templates: MutableList<EventTemplateEntity>,
) : EventTemplateTagBindingDao {

    override suspend fun insert(binding: EventTemplateTagBindingEntity) {
        if (bindings.none { it == binding }) bindings.add(binding)
    }

    override suspend fun insertAll(bindings: List<EventTemplateTagBindingEntity>) {
        bindings.forEach { insert(it) }
    }

    override suspend fun delete(templateId: Long, tagId: Long) {
        bindings.removeAll { it.templateId == templateId && it.tagId == tagId }
    }

    override suspend fun deleteByTemplate(templateId: Long) {
        bindings.removeAll { it.templateId == templateId }
    }

    override suspend fun deleteByTag(tagId: Long) {
        bindings.removeAll { it.tagId == tagId }
    }

    override fun observeAll(): Flow<List<EventTemplateTagBindingEntity>> =
        flowOf(bindings.toList())

    override suspend fun getByTemplateSync(templateId: Long): List<EventTemplateTagBindingEntity> =
        bindings.filter { it.templateId == templateId }

    override suspend fun getByTagIdsSync(tagIds: List<Long>): List<EventTemplateTagBindingEntity> =
        bindings.filter { it.tagId in tagIds }

    override suspend fun matchTemplatesByTags(tagIds: List<Long>): List<TemplateTagMatchRow> {
        return bindings.filter { it.tagId in tagIds }
            .groupBy { it.templateId }
            .mapNotNull { (templateId, rows) ->
                val template = templates.firstOrNull { it.id == templateId } ?: return@mapNotNull null
                TemplateTagMatchRow(
                    id = template.id,
                    name = template.name,
                    description = template.description,
                    createdAt = template.createdAt,
                    sortOrder = template.sortOrder,
                    matchedTagCount = rows.size,
                )
            }
            .sortedWith(
                compareByDescending<TemplateTagMatchRow> { it.matchedTagCount }
                    .thenBy { it.sortOrder }
            )
    }
}
