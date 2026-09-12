package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * EventTemplateTagBindingEntity 模板-标签绑定实体
 * 多对多关联表，连接 event_template 与 tags
 * 模板删除或标签删除时级联删除绑定（标签删除时其历史事件保留）
 */
@Entity(
    tableName = "event_template_tag_binding",
    primaryKeys = ["templateId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = EventTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId"), Index("tagId")],
)
data class EventTemplateTagBindingEntity(
    val templateId: Long,
    val tagId: Long,
)
