package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.EventTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * EventTemplateDao 事件打点模板数据访问对象
 * 提供 event_template 表的基础 CRUD 与按标签绑定查询
 */
@Dao
interface EventTemplateDao {
    @Insert
    suspend fun insert(template: EventTemplateEntity): Long

    @Update
    suspend fun update(template: EventTemplateEntity)

    @Query("DELETE FROM event_template WHERE id = :id")
    suspend fun delete(id: Long)

    /** 全部模板（按 sortOrder 升序） */
    @Query("SELECT * FROM event_template ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<EventTemplateEntity>>

    @Query("SELECT * FROM event_template WHERE id = :id")
    suspend fun getById(id: Long): EventTemplateEntity?

    @Query("SELECT * FROM event_template WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): EventTemplateEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM event_template")
    suspend fun getMaxSortOrder(): Int

    /** 查询绑定指定标签的模板（模板 → 绑定表反向 JOIN） */
    @Query(
        """
        SELECT t.* FROM event_template t
        INNER JOIN event_template_tag_binding b ON t.id = b.templateId
        WHERE b.tagId = :tagId
        ORDER BY t.sortOrder ASC
        """
    )
    fun observeByTag(tagId: Long): Flow<List<EventTemplateEntity>>
}
