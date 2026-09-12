package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.BehaviorEventEntity

/**
 * BehaviorEvent 行为事件领域模型
 * 一次结构化打点记录。behaviorId 可空：null 表示独立事件（无活跃计时也可记录）
 */
@Immutable
data class BehaviorEvent(
    val id: Long,
    val behaviorId: Long?,
    val activityId: Long?,
    val templateId: Long,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toEntity() = BehaviorEventEntity(
        id = id,
        behaviorId = behaviorId,
        activityId = activityId,
        templateId = templateId,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromEntity(entity: BehaviorEventEntity) = BehaviorEvent(
            id = entity.id,
            behaviorId = entity.behaviorId,
            activityId = entity.activityId,
            templateId = entity.templateId,
            timestamp = entity.timestamp,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}

/**
 * BehaviorEventWithValues 事件详情包装领域模型
 * 事件本体 + 其字段值列表（EAV 行），供详情弹窗 / 事件列表卡组装展示
 */
@Immutable
data class BehaviorEventWithValues(
    val event: BehaviorEvent,
    val values: List<BehaviorEventValue> = emptyList(),
)
