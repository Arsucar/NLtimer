package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.util.ClockService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AddEventUseCase 新增结构化事件（打点）
 * timestamp 缺省取 ClockService 当前时间；字段值仅保留模板内存在的字段，
 * 并按字段类型对齐取值列（数值/星级写 valueNumber，文本/单选写 valueText）
 */
@Singleton
class AddEventUseCase @Inject constructor(
    private val behaviorEventRepository: BehaviorEventRepository,
    private val eventTemplateRepository: EventTemplateRepository,
    private val clockService: ClockService,
) {
    sealed class Result {
        data class Success(val eventId: Long) : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend operator fun invoke(
        templateId: Long,
        behaviorId: Long? = null,
        activityId: Long? = null,
        timestamp: Long? = null,
        values: List<BehaviorEventValue> = emptyList(),
    ): Result {
        eventTemplateRepository.getTemplateById(templateId)
            ?: return Result.ValidationError("打点模板不存在")
        val fields = eventTemplateRepository.getFieldsByTemplateSync(templateId)

        val now = clockService.currentTimeMillis()
        val event = BehaviorEvent(
            id = 0,
            behaviorId = behaviorId,
            activityId = activityId,
            templateId = templateId,
            timestamp = timestamp ?: now,
            createdAt = now,
            updatedAt = now,
        )
        val eventId = behaviorEventRepository.addEvent(event, filterValidValues(fields, values))
        return Result.Success(eventId)
    }

    /** 仅保留模板内字段，并按字段类型对齐取值列；无效行静默剔除 */
    private fun filterValidValues(
        fields: List<EventTemplateField>,
        values: List<BehaviorEventValue>,
    ): List<BehaviorEventValue> {
        val fieldsById = fields.associateBy { it.id }
        return values.mapNotNull { value ->
            val field = fieldsById[value.fieldId] ?: return@mapNotNull null
            val aligned = when (field.type) {
                EventFieldType.NUMBER, EventFieldType.RATING ->
                    if (value.valueNumber != null) value.copy(valueText = null) else return@mapNotNull null
                EventFieldType.TEXT, EventFieldType.SELECT ->
                    if (value.valueText != null) value.copy(valueNumber = null) else return@mapNotNull null
            }
            aligned.copy(id = 0, eventId = 0)
        }
    }
}
