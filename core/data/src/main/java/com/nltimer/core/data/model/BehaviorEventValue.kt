package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.BehaviorEventValueEntity

/**
 * BehaviorEventValue 行为事件字段值领域模型（EAV 行）
 * 数值/星级字段填 [valueNumber]，文本/单选字段填 [valueText]
 */
@Immutable
data class BehaviorEventValue(
    val id: Long = 0,
    val eventId: Long = 0,
    val fieldId: Long,
    val valueText: String? = null,
    val valueNumber: Double? = null,
) {
    fun toEntity() = BehaviorEventValueEntity(
        id = id,
        eventId = eventId,
        fieldId = fieldId,
        valueText = valueText,
        valueNumber = valueNumber,
    )

    companion object {
        fun fromEntity(entity: BehaviorEventValueEntity) = BehaviorEventValue(
            id = entity.id,
            eventId = entity.eventId,
            fieldId = entity.fieldId,
            valueText = entity.valueText,
            valueNumber = entity.valueNumber,
        )
    }
}
