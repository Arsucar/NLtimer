package com.nltimer.core.data.repository

import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import kotlinx.coroutines.flow.Flow

/**
 * EventTemplateRepository 事件打点模板仓库接口
 * 提供模板 CRUD、字段列表整体替换（事务内）、标签绑定管理与按标签匹配模板
 */
interface EventTemplateRepository {
    fun observeAll(): Flow<List<EventTemplate>>
    fun observeTemplatesByTag(tagId: Long): Flow<List<EventTemplate>>

    suspend fun getTemplateById(id: Long): EventTemplate?
    suspend fun getTemplateByName(name: String): EventTemplate?
    suspend fun getMaxSortOrder(): Int
    suspend fun getFieldsByTemplateSync(templateId: Long): List<EventTemplateField>
    suspend fun getFieldsForTemplatesSync(templateIds: List<Long>): Map<Long, List<EventTemplateField>>
    suspend fun getTagIdsForTemplateSync(templateId: Long): List<Long>

    suspend fun insertTemplate(template: EventTemplate): Long
    suspend fun updateTemplate(template: EventTemplate)
    suspend fun deleteTemplate(id: Long)

    /** 整体替换模板字段列表（事务内先删后插；旧字段删除时其事件值随外键级联删除） */
    suspend fun saveTemplateFields(templateId: Long, fields: List<EventTemplateField>)

    /** 整体替换模板的标签绑定（事务内先删后插） */
    suspend fun saveTemplateBindings(templateId: Long, tagIds: List<Long>)

    suspend fun addTagBinding(templateId: Long, tagId: Long)
    suspend fun removeTagBinding(templateId: Long, tagId: Long)

    /**
     * 按标签集合匹配模板：命中标签数多者优先，其后按模板 sortOrder 升序；
     * 无标签或无绑定命中返回 null（由调用方回退上次使用模板 / 通用表单）
     */
    suspend fun matchTemplateByTags(tagIds: List<Long>): EventTemplate?
}
