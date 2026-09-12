package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.util.ClockService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateEventUseCaseTest {

    private lateinit var behaviorEventRepository: BehaviorEventRepository
    private lateinit var eventTemplateRepository: EventTemplateRepository
    private lateinit var clockService: ClockService
    private lateinit var useCase: UpdateEventUseCase

    private val fixedNow = 1700000000000L

    private val oldTemplateId = 1L
    private val newTemplateId = 2L

    private val oldFields = listOf(
        EventTemplateField(id = 10, templateId = oldTemplateId, name = "强度", type = EventFieldType.NUMBER, sortOrder = 0),
        EventTemplateField(id = 11, templateId = oldTemplateId, name = "备注", type = EventFieldType.TEXT, sortOrder = 1),
    )

    private val newFields = listOf(
        EventTemplateField(id = 20, templateId = newTemplateId, name = "强度", type = EventFieldType.NUMBER, sortOrder = 0),
        EventTemplateField(id = 22, templateId = newTemplateId, name = "心情", type = EventFieldType.SELECT, options = listOf("好", "一般"), sortOrder = 1),
    )

    private val existingEvent = BehaviorEvent(
        id = 5L,
        behaviorId = 3L,
        activityId = 7L,
        templateId = oldTemplateId,
        timestamp = 1000L,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private val existingTemplate = EventTemplate(id = oldTemplateId, name = "旧模板", createdAt = fixedNow)
    private val newTemplate = EventTemplate(id = newTemplateId, name = "新模板", createdAt = fixedNow)

    @Before
    fun setup() {
        behaviorEventRepository = mockk(relaxed = true)
        eventTemplateRepository = mockk(relaxed = true)
        clockService = mockk()
        every { clockService.currentTimeMillis() } returns fixedNow
        useCase = UpdateEventUseCase(behaviorEventRepository, eventTemplateRepository, clockService)
    }

    private fun stubOldValues(values: List<BehaviorEventValue>) {
        coEvery { behaviorEventRepository.getEventWithValues(5L) } returns
            BehaviorEventWithValues(event = existingEvent, values = values)
    }

    /** 模板切换场景：旧模板字段 + 新模板字段 */
    private fun stubTemplateSwitch() {
        coEvery { eventTemplateRepository.getTemplateById(oldTemplateId) } returns existingTemplate
        coEvery { eventTemplateRepository.getTemplateById(newTemplateId) } returns newTemplate
        coEvery { eventTemplateRepository.getFieldsByTemplateSync(oldTemplateId) } returns oldFields
        coEvery { eventTemplateRepository.getFieldsByTemplateSync(newTemplateId) } returns newFields
    }

    /** 同模板编辑场景：旧字段即新字段 */
    private fun stubSameTemplate() {
        coEvery { eventTemplateRepository.getTemplateById(oldTemplateId) } returns existingTemplate
        coEvery { eventTemplateRepository.getFieldsByTemplateSync(oldTemplateId) } returns oldFields
    }

    @Test
    fun `missing event returns NotFound`() = runTest {
        coEvery { behaviorEventRepository.getEventWithValues(5L) } returns null

        val result = useCase(eventId = 5L, templateId = oldTemplateId)

        assertTrue(result is UpdateEventUseCase.Result.NotFound)
    }

    @Test
    fun `missing target template returns ValidationError`() = runTest {
        stubOldValues(emptyList())
        coEvery { eventTemplateRepository.getTemplateById(oldTemplateId) } returns existingTemplate
        coEvery { eventTemplateRepository.getTemplateById(newTemplateId) } returns null

        val result = useCase(eventId = 5L, templateId = newTemplateId)

        assertTrue(result is UpdateEventUseCase.Result.ValidationError)
    }

    @Test
    fun `same name same type field values migrate to new template`() = runTest {
        stubOldValues(
            listOf(
                BehaviorEventValue(eventId = 5L, fieldId = 10L, valueNumber = 3.0),
                BehaviorEventValue(eventId = 5L, fieldId = 11L, valueText = "旧备注"),
            ),
        )
        stubTemplateSwitch()
        val eventSlot = slot<BehaviorEvent>()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.updateEvent(capture(eventSlot), capture(valuesSlot)) } returns Unit

        val result = useCase(eventId = 5L, templateId = newTemplateId)

        assertTrue(result is UpdateEventUseCase.Result.Success)
        // 旧“强度”(f10) 数值迁移到新“强度”(f20)；“备注”(f11) 无同名同类型新字段 → 清空并提示
        val success = result as UpdateEventUseCase.Result.Success
        assertEquals(listOf(20L), success.values.map { it.fieldId })
        assertEquals(3.0, success.values.single().valueNumber)
        assertEquals(listOf("备注"), success.droppedValueNames)
        assertEquals(newTemplateId, eventSlot.captured.templateId)
        assertEquals(5L, eventSlot.captured.id)
    }

    @Test
    fun `explicit values override migrated ones`() = runTest {
        stubOldValues(listOf(BehaviorEventValue(eventId = 5L, fieldId = 10L, valueNumber = 3.0)))
        stubTemplateSwitch()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.updateEvent(any(), capture(valuesSlot)) } returns Unit

        val result = useCase(
            eventId = 5L,
            templateId = newTemplateId,
            values = listOf(BehaviorEventValue(fieldId = 20L, valueNumber = 8.0)),
        )

        val saved = (result as UpdateEventUseCase.Result.Success).values.single { it.fieldId == 20L }
        assertEquals(8.0, saved.valueNumber)
    }

    @Test
    fun `invalid explicit values are filtered out`() = runTest {
        stubOldValues(emptyList())
        stubTemplateSwitch()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.updateEvent(any(), capture(valuesSlot)) } returns Unit

        useCase(
            eventId = 5L,
            templateId = newTemplateId,
            values = listOf(BehaviorEventValue(fieldId = 999L, valueText = "幽灵")),
        )

        assertTrue(valuesSlot.captured.isEmpty())
    }

    @Test
    fun `timestamp null keeps original and updatedAt refreshes`() = runTest {
        stubOldValues(emptyList())
        stubTemplateSwitch()
        val eventSlot = slot<BehaviorEvent>()
        coEvery { behaviorEventRepository.updateEvent(capture(eventSlot), any()) } returns Unit

        useCase(eventId = 5L, templateId = newTemplateId, timestamp = null)

        assertEquals(1000L, eventSlot.captured.timestamp)
        assertEquals(fixedNow, eventSlot.captured.updatedAt)
        assertEquals(7L, eventSlot.captured.activityId)
        assertEquals(3L, eventSlot.captured.behaviorId)
    }

    @Test
    fun `same template edit keeps unmigrated same name type values`() = runTest {
        val existingValues = listOf(
            BehaviorEventValue(eventId = 5L, fieldId = 10L, valueNumber = 3.0),
            BehaviorEventValue(eventId = 5L, fieldId = 11L, valueText = "原来"),
        )
        stubOldValues(existingValues)
        stubSameTemplate()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.updateEvent(any(), capture(valuesSlot)) } returns Unit

        val result = useCase(eventId = 5L, templateId = oldTemplateId, values = listOf(BehaviorEventValue(fieldId = 10L, valueNumber = 9.0)))

        val success = result as UpdateEventUseCase.Result.Success
        // 显式新值覆盖“强度”；“备注”同名同类型保留；同名旧“强度”被覆盖
        assertEquals(9.0, success.values.single { it.fieldId == 10L }.valueNumber)
        assertTrue(success.values.any { it.fieldId == 11L && it.valueText == "原来" })
        assertTrue(success.values.none { it.fieldId == 10L && it.valueNumber == 3.0 })
        assertEquals(emptyList<String>(), success.droppedValueNames)
        coVerify { eventTemplateRepository.getTemplateById(oldTemplateId) }
    }
}
