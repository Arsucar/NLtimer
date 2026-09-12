package com.nltimer.feature.settings.ui.eventtemplate

import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.usecase.CreateOrUpdateTemplateUseCase
import com.nltimer.core.data.usecase.DeleteTemplateUseCase
import com.nltimer.core.data.util.ClockService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventTemplateViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeEventTemplateRepository
    private lateinit var viewModel: EventTemplateViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeEventTemplateRepository()
        val clock = object : ClockService {
            override fun currentTimeMillis(): Long = 1L
        }
        viewModel = EventTemplateViewModel(
            repository,
            CreateOrUpdateTemplateUseCase(repository, clock),
            DeleteTemplateUseCase(repository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prepareEdit assigns unique uid per field`() = runTest {
        repository.addTemplate(
            EventTemplate(id = 1, name = "晨会", createdAt = 1),
            listOf(
                EventTemplateField(id = 10, templateId = 1, name = "强度", type = EventFieldType.NUMBER),
                EventTemplateField(id = 11, templateId = 1, name = "备注", type = EventFieldType.TEXT),
            ),
        )

        viewModel.prepareEdit(1L)
        advanceUntilIdle()

        val uids = viewModel.editState.value.fields.map { it.uid }
        assertEquals(2, uids.size)
        assertEquals(uids.toSet().size, uids.size)
        assertNotEquals(uids[0], uids[1])
    }

    @Test
    fun `removeField only removes the matching uid`() = runTest {
        repository.addTemplate(
            EventTemplate(id = 1, name = "晨会", createdAt = 1),
            listOf(
                EventTemplateField(id = 10, templateId = 1, name = "强度", type = EventFieldType.NUMBER),
                EventTemplateField(id = 11, templateId = 1, name = "备注", type = EventFieldType.TEXT),
            ),
        )

        viewModel.prepareEdit(1L)
        advanceUntilIdle()

        val firstUid = viewModel.editState.value.fields.first().uid
        viewModel.removeField(firstUid)

        val remaining = viewModel.editState.value.fields
        assertEquals(1, remaining.size)
        assertEquals(11L, remaining.single().fieldId)
        assertNotEquals(firstUid, remaining.single().uid)
    }

    private class FakeEventTemplateRepository : EventTemplateRepository {
        private val templates = mutableListOf<EventTemplate>()
        private val fieldsByTemplateId = mutableMapOf<Long, List<EventTemplateField>>()

        fun addTemplate(template: EventTemplate, fields: List<EventTemplateField>) {
            templates.add(template)
            fieldsByTemplateId[template.id] = fields
        }

        override fun observeAll(): Flow<List<EventTemplate>> = MutableStateFlow(templates.toList())
        override fun observeTemplatesByTag(tagId: Long): Flow<List<EventTemplate>> = MutableStateFlow(emptyList())
        override suspend fun getTemplateById(id: Long): EventTemplate? = templates.firstOrNull { it.id == id }
        override suspend fun getTemplateByName(name: String): EventTemplate? = templates.firstOrNull { it.name == name }
        override suspend fun getMaxSortOrder(): Int = templates.maxOfOrNull { it.sortOrder } ?: -1
        override suspend fun getFieldsByTemplateSync(templateId: Long): List<EventTemplateField> =
            fieldsByTemplateId[templateId].orEmpty()
        override suspend fun getFieldsForTemplatesSync(templateIds: List<Long>): Map<Long, List<EventTemplateField>> =
            templateIds.mapNotNull { id -> fieldsByTemplateId[id]?.let { id to it } }.toMap()
        override suspend fun getTagIdsForTemplateSync(templateId: Long): List<Long> = emptyList()
        override suspend fun insertTemplate(template: EventTemplate): Long = template.id
        override suspend fun updateTemplate(template: EventTemplate) {}
        override suspend fun deleteTemplate(id: Long) {}
        override suspend fun saveTemplateFields(templateId: Long, fields: List<EventTemplateField>) {}
        override suspend fun saveTemplateBindings(templateId: Long, tagIds: List<Long>) {}
        override suspend fun addTagBinding(templateId: Long, tagId: Long) {}
        override suspend fun removeTagBinding(templateId: Long, tagId: Long) {}
        override suspend fun matchTemplateByTags(tagIds: List<Long>): EventTemplate? = null
    }
}
