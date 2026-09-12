package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * EventTemplateFieldEntity 事件打点模板字段实体
 * 对应 event_template_field 表，描述模板的字段结构（名称 / 类型 / 单选选项 / 排序）
 * 通过 templateId 外键关联 event_template 表，模板删除时级联删除字段
 *
 * @property type 字段类型，存储 [com.nltimer.core.data.model.EventFieldType.key]
 *   （select / text / number / rating，TEXT 存法，无 TypeConverter）
 * @property optionsJson 单选字段的选项 JSON 数组字符串，如 ["选项A","选项B"]；
 *   非单选字段为 null（手工 JSON 字符串存法，同 feature/ai toolCallsJson 先例）
 */
@Entity(
    tableName = "event_template_field",
    foreignKeys = [
        ForeignKey(
            entity = EventTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId")],
)
data class EventTemplateFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val name: String,
    val type: String,
    val optionsJson: String? = null,
    val sortOrder: Int = 0,
)
