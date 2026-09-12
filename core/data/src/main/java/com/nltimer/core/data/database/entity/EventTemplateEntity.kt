package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EventTemplateEntity 事件打点模板实体
 * 对应 event_template 表，用户自定义结构化事件模板（模板名 + 说明 + 排序）
 * 模板字段见 [EventTemplateFieldEntity]，标签绑定见 [EventTemplateTagBindingEntity]
 */
@Entity(tableName = "event_template")
data class EventTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val sortOrder: Int = 0,
)
