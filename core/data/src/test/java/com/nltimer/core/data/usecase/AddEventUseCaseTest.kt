package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventValue
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
class AddEventUseCaseTest {

    private lateinit var behaviorEventRepository: BehaviorEventRepository
    private lateinit var eventTemplateRepository: EventTemplateRepository
    private lateinit var clockService: ClockService
    private lateinit var useCase: AddEventUseCase

    private val fixedNow = 1700000000000L

    private val templateFields = listOf(
        EventTemplateField(id = 10, templateId = 1, name = "强度", type = EventFieldType.NUMBER, sortOrder = 0),
        EventTemplateField(id = 11, templateId = 1, name = "备注", type = EventFieldType.TEXT, sortOrder = 1),
        EventTemplateField(id = 12, templateId = 1, name = "分类", type = EventFieldType.SELECT, options = listOf("开会", "通勤"), sortOrder = 2),
        EventTemplateField(id = 13, templateId = 1, name = "评分", type = EventFieldType.RATING, sortOrder = 3),
    )

    @Before
    fun setup() {
        behaviorEventRepository = mockk(relaxed = true)
        eventTemplateRepository = mockk(relaxed = true)
        clockService = mockk()
        every { clockService.currentTimeMillis() } returns fixedNow
        useCase = AddEventUseCase(behaviorEventRepository, eventTemplateRepository, clockService)
    }

    private fun stubTemplate(templateId: Long = 1L) {
        coEvery { eventTemplateRepository.getTemplateById(templateId) } returns
            EventTemplate(id = templateId, name = "模板", createdAt = fixedNow)
        coEvery { eventTemplateRepository.getFieldsByTemplateSync(templateId) } returns templateFields
    }

    @Test
    fun `missing template returns ValidationError`() = runTest {
        coEvery { eventTemplateRepository.getTemplateById(1L) } returns null

        val result = useCase(templateId = 1L)

        assertTrue(result is AddEventUseCase.Result.ValidationError)
        assertEquals("打点模板不存在", (result as AddEventUseCase.Result.ValidationError).message)
    }

    @Test
    fun `success without timestamp stamps clock now`() = runTest {
        stubTemplate()
        val eventSlot = slot<BehaviorEvent>()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.addEvent(capture(eventSlot), capture(valuesSlot)) } returns 77L

        val result = useCase(templateId = 1L, activityId = 7L, timestamp = null)

        assertTrue(result is AddEventUseCase.Result.Success)
        assertEquals(77L, (result as AddEventUseCase.Result.Success).eventId)
        assertEquals(fixedNow, eventSlot.captured.timestamp)
        assertEquals(fixedNow, eventSlot.captured.createdAt)
        assertEquals(fixedNow, eventSlot.captured.updatedAt)
        assertNullBehavior(eventSlot.captured)
        assertEquals(7L, eventSlot.captured.activityId)
        coVerify { behaviorEventRepository.addEvent(any(), any()) }
    }

    private fun assertNullBehavior(event: BehaviorEvent) {
        assertEquals(null, event.behaviorId)
    }

    @Test
    fun `explicit timestamp overrides clock`() = runTest {
        stubTemplate()
        val eventSlot = slot<BehaviorEvent>()
        coEvery { behaviorEventRepository.addEvent(capture(eventSlot), any()) } returns 1L

        useCase(templateId = 1L, timestamp = fixedNow - 1000)

        assertEquals(fixedNow - 1000, eventSlot.captured.timestamp)
    }

    @Test
    fun `values with unknown fieldId are dropped`() = runTest {
        stubTemplate()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.addEvent(any(), capture(valuesSlot)) } returns 1L

        useCase(
            templateId = 1L,
            values = listOf(
                BehaviorEventValue(fieldId = 99, valueText = "幽灵字段"),
                BehaviorEventValue(fieldId = 10, valueNumber = 3.0),
            ),
        )

        assertEquals(1, valuesSlot.captured.size)
        assertEquals(10L, valuesSlot.captured.single().fieldId)
        assertEquals(3.0, valuesSlot.captured.single().valueNumber)
        assertEquals(null, valuesSlot.captured.single().valueText)
    }

    @Test
    fun `value rows aligned by field type`() = runTest {
        stubTemplate()
        val valuesSlot = slot<List<BehaviorEventValue>>()
        coEvery { behaviorEventRepository.addEvent(any(), capture(valuesSlot)) } returns 1L

        useCase(
            templateId = 1L,
            values = listOf(
                // 数值字段只带 valueNumber → 保留；文本字段只带 valueNumber → 剔除
                BehaviorEventValue(fieldId = 10, valueNumber = 4.0),
                BehaviorEventValue(fieldId = 11, valueNumber = 9.0),
                // 单选/文本字段只带 valueText → 保留；评分字段带 valueText → 剔除
                BehaviorEventValue(fieldId = 12, valueText = "开会"),
                BehaviorEventValue(fieldId = 13, valueText = "5"),
            ),
        )

        val saved = valuesSlot.captured
        assertEquals(2, saved.size)
        assertTrue(saved.any { it.fieldId == 10L && it.valueNumber == 4.0 && it.valueText == null })
        assertTrue(saved.any { it.fieldId == 12L && it.valueText == "开会" && it.valueNumber == null })
    }

    @Test
    fun `independent event keeps null behaviorId and activityId`() = runTest {
        stubTemplate()
        val eventSlot = slot<BehaviorEvent>()
        coEvery { behaviorEventRepository.addEvent(capture(eventSlot), any()) } returns 1L

        useCase(templateId = 1L, behaviorId = null, activityId = null)

        assertEquals(null, eventSlot.captured.activityId)
        assertEquals(null, eventSlot.captured.behaviorId)
    }
}
