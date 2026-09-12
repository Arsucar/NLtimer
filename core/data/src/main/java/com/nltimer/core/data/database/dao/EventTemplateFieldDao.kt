package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.EventTemplateFieldEntity
import kotlinx.coroutines.flow.Flow

/**
 * EventTemplateFieldDao 事件打点模板字段数据访问对象
 * 提供 event_template_field 表的字段 CRUD 与按模板查询（维护字段列表整体替换语义）
 */
@Dao
interface EventTemplateFieldDao {
    @Insert
    suspend fun insert(field: EventTemplateFieldEntity): Long

    @Insert
    suspend fun insertAll(fields: List<EventTemplateFieldEntity>)

    @Update
    suspend fun update(field: EventTemplateFieldEntity)

    @Query("DELETE FROM event_template_field WHERE id = :id")
    suspend fun delete(id: Long)

    /** 删除模板的全部字段（事件值随外键级联删除，需在仓库层事务内调用） */
    @Query("DELETE FROM event_template_field WHERE templateId = :templateId")
    suspend fun deleteByTemplate(templateId: Long)

    /** 差量删除字段：仅移除 keepIds 之外的字段行（保留字段的事件值不被级联清空，需在仓库层事务内调用） */
    @Query("DELETE FROM event_template_field WHERE templateId = :templateId AND id NOT IN (:keepIds)")
    suspend fun deleteExceptIds(templateId: Long, keepIds: List<Long>)

    /** 模板字段列表（按 sortOrder 升序） */
    @Query("SELECT * FROM event_template_field WHERE templateId = :templateId ORDER BY sortOrder ASC")
    fun observeByTemplate(templateId: Long): Flow<List<EventTemplateFieldEntity>>

    @Query("SELECT * FROM event_template_field WHERE templateId = :templateId ORDER BY sortOrder ASC")
    suspend fun getByTemplateSync(templateId: Long): List<EventTemplateFieldEntity>

    /** 批量查询多个模板的字段（详情/展示组装用，调用方按 templateId 分组） */
    @Query("SELECT * FROM event_template_field WHERE templateId IN (:templateIds) ORDER BY sortOrder ASC")
    suspend fun getByTemplateIdsSync(templateIds: List<Long>): List<EventTemplateFieldEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM event_template_field WHERE templateId = :templateId")
    suspend fun getMaxSortOrder(templateId: Long): Int
}
