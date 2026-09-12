package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.BehaviorEventValue
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.BehaviorEventRepository
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.util.ClockService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UpdateEventUseCase 更新已有事件（可切换模板）
 * 切换模板时字段值按“同名同类型尽量映射，其余清空并提示”迁移；
 * timestamp 缺省保留原值，updatedAt 刷新为当前时间
 */
@Singleton
class UpdateEventUseCase @Inject constructor(
    private val behaviorEventRepository: BehaviorEventRepository,
    private val eventTemplateRepository: EventTemplateRepository,
    private val clockService: ClockService,
) {
    sealed class Result {
        data class Success(
            val eventId: Long,
            val values: List<BehaviorEventValue>,
            /** 新模板中不再存在的旧字段名（其值被清空），供 UI 提示 */
            val droppedValueNames: List<String>,
        ) : Result()

        data object NotFound : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend operator fun invoke(
        eventId: Long,
        templateId: Long,
        timestamp: Long? = null,
        values: List<BehaviorEventValue> = emptyList(),
    ): Result {
        val existing = behaviorEventRepository.getEventWithValues(eventId)
            ?: return Result.NotFound
        val oldFields = eventTemplateRepository.getFieldsByTemplateSync(existing.event.templateId)
        eventTemplateRepository.getTemplateById(templateId)
            ?: return Result.ValidationError("打点模板不存在")
        val newFields = eventTemplateRepository.getFieldsByTemplateSync(templateId)

        val now = clockService.currentTimeMillis()

        // 显式传入的新值：仅保留模板内字段并按类型对齐
        val explicitByFieldId = filterValidValues(newFields, values).associateBy { it.fieldId }

        val oldFieldsById = oldFields.associateBy { it.id }
        val newFieldsByNameAndType = newFields.associateBy { it.name to it.type }
        val migrated = mutableMapOf<Long, BehaviorEventValue>()
        val droppedValueNames = mutableSetOf<String>()
        for (oldValue in existing.values) {
            val oldField = oldFieldsById[oldValue.fieldId] ?: continue
            val newField = newFieldsByNameAndType[oldField.name to oldField.type]
            if (newField == null) {
                droppedValueNames += oldField.name
                continue
            }
            if (newField.id !in explicitByFieldId && newField.id !in migrated) {
                migrated[newField.id] = oldValue.copy(id = 0, eventId = 0, fieldId = newField.id)
            }
        }

        // 显式值优先，迁移值补位；按新模板字段顺序输出
        val merged = explicitByFieldId.toMutableMap().apply { putAll(migrated) }
        val finalValues = newFields.mapNotNull { field -> merged[field.id] }

        val updatedEvent = existing.event.copy(
            templateId = templateId,
            timestamp = timestamp ?: existing.event.timestamp,
            updatedAt = now,
        )
        behaviorEventRepository.updateEvent(updatedEvent, finalValues)
        return Result.Success(
            eventId = updatedEvent.id,
            values = finalValues,
            droppedValueNames = droppedValueNames.toList(),
        )
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
