package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.EventTemplateEntity

/**
 * EventTemplate 事件打点模板领域模型
 * 用户自定义结构化事件模板；字段结构见 [EventTemplateField]
 */
@Immutable
data class EventTemplate(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val sortOrder: Int = 0,
) {
    fun toEntity() = EventTemplateEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        sortOrder = sortOrder,
    )

    companion object {
        fun fromEntity(entity: EventTemplateEntity) = EventTemplate(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdAt = entity.createdAt,
            sortOrder = entity.sortOrder,
        )
    }
}
