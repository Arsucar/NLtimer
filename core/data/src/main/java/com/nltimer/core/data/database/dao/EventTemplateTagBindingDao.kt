package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nltimer.core.data.database.entity.EventTemplateTagBindingEntity
import com.nltimer.core.data.model.EventTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 模板-标签匹配联表扁平结果行
 * 模板列 + 命中标签数，用于按标签集合匹配模板
 */
data class TemplateTagMatchRow(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val sortOrder: Int,
    val matchedTagCount: Int,
)

/** 将联表匹配结果 TemplateTagMatchRow 转换为领域模型 EventTemplate（同 BehaviorTagRow.toTag 范式） */
fun TemplateTagMatchRow.toEventTemplate() = EventTemplate(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    sortOrder = sortOrder,
)

/**
 * EventTemplateTagBindingDao 模板-标签绑定数据访问对象
 * 提供 event_template_tag_binding 表的绑定增删查与模板匹配查询
 */
@Dao
interface EventTemplateTagBindingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(binding: EventTemplateTagBindingEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(bindings: List<EventTemplateTagBindingEntity>)

    @Query("DELETE FROM event_template_tag_binding WHERE templateId = :templateId AND tagId = :tagId")
    suspend fun delete(templateId: Long, tagId: Long)

    /** 删除模板的全部绑定（需在仓库层事务内调用） */
    @Query("DELETE FROM event_template_tag_binding WHERE templateId = :templateId")
    suspend fun deleteByTemplate(templateId: Long)

    /** 删除标签的全部绑定（标签删除时由外键级联触发，亦供手动清理） */
    @Query("DELETE FROM event_template_tag_binding WHERE tagId = :tagId")
    suspend fun deleteByTag(tagId: Long)

    @Query("SELECT * FROM event_template_tag_binding")
    fun observeAll(): Flow<List<EventTemplateTagBindingEntity>>

    @Query("SELECT * FROM event_template_tag_binding WHERE templateId = :templateId")
    suspend fun getByTemplateSync(templateId: Long): List<EventTemplateTagBindingEntity>

    @Query("SELECT * FROM event_template_tag_binding WHERE tagId IN (:tagIds)")
    suspend fun getByTagIdsSync(tagIds: List<Long>): List<EventTemplateTagBindingEntity>

    /**
     * 按标签集合匹配模板：命中标签数多者优先，其后按模板 sortOrder 升序
     * （多标签多模板时按绑定优先级取一，规则在仓库层封装）
     */
    @Query(
        """
        SELECT t.id, t.name, t.description, t.createdAt, t.sortOrder, COUNT(b.tagId) AS matchedTagCount
        FROM event_template t
        INNER JOIN event_template_tag_binding b ON t.id = b.templateId
        WHERE b.tagId IN (:tagIds)
        GROUP BY t.id
        ORDER BY matchedTagCount DESC, t.sortOrder ASC
        """
    )
    suspend fun matchTemplatesByTags(tagIds: List<Long>): List<TemplateTagMatchRow>
}
