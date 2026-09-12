package com.nltimer.core.data.repository.impl

import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.EventTemplateDao
import com.nltimer.core.data.database.dao.EventTemplateFieldDao
import com.nltimer.core.data.database.dao.EventTemplateTagBindingDao
import com.nltimer.core.data.database.dao.toEventTemplate
import com.nltimer.core.data.database.entity.EventTemplateTagBindingEntity
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.util.mapList
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EventTemplateRepositoryImpl 事件打点模板仓库实现
 * 模板 CRUD 为单 DAO 调用；字段列表与标签绑定的整体替换收敛在 withTransaction 事务内
 * （事务方法均返回 Unit，保证 MockK JVM 测试可放行，见 BehaviorRepositoryImplTest @Ignore 教训）
 */
@Singleton
class EventTemplateRepositoryImpl @Inject constructor(
    private val templateDao: EventTemplateDao,
    private val fieldDao: EventTemplateFieldDao,
    private val bindingDao: EventTemplateTagBindingDao,
    private val database: NLtimerDatabase,
) : EventTemplateRepository {

    override fun observeAll(): Flow<List<EventTemplate>> =
        templateDao.observeAll().mapList { EventTemplate.fromEntity(it) }

    override fun observeTemplatesByTag(tagId: Long): Flow<List<EventTemplate>> =
        templateDao.observeByTag(tagId).mapList { EventTemplate.fromEntity(it) }

    override suspend fun getTemplateById(id: Long): EventTemplate? =
        templateDao.getById(id)?.let { EventTemplate.fromEntity(it) }

    override suspend fun getTemplateByName(name: String): EventTemplate? =
        templateDao.getByName(name)?.let { EventTemplate.fromEntity(it) }

    override suspend fun getFieldsByTemplateSync(templateId: Long): List<EventTemplateField> =
        fieldDao.getByTemplateSync(templateId).map { EventTemplateField.fromEntity(it) }

    override suspend fun getFieldsForTemplatesSync(templateIds: List<Long>): Map<Long, List<EventTemplateField>> =
        fieldDao.getByTemplateIdsSync(templateIds)
            .groupBy { it.templateId }
            .mapValues { (_, rows) -> rows.map { EventTemplateField.fromEntity(it) } }

    override suspend fun getTagIdsForTemplateSync(templateId: Long): List<Long> =
        bindingDao.getByTemplateSync(templateId).map { it.tagId }

    override suspend fun insertTemplate(template: EventTemplate): Long =
        templateDao.insert(template.toEntity())

    override suspend fun updateTemplate(template: EventTemplate) =
        templateDao.update(template.toEntity())

    override suspend fun deleteTemplate(id: Long) = templateDao.delete(id)

    override suspend fun saveTemplateFields(templateId: Long, fields: List<EventTemplateField>) {
        database.withTransaction {
            // 差量替换：id 非空 = 既有字段（update 保留其事件值），id = 0 = 新增字段（insert）；
            // 仅真正被删除的字段触发级联清空其事件值（符合接口文档「原生替换」契约的意图）
            val keepIds = fields.filter { it.id != 0L }.map { it.id }
            if (keepIds.isEmpty()) {
                fieldDao.deleteByTemplate(templateId)
            } else {
                fieldDao.deleteExceptIds(templateId, keepIds)
            }
            fields.forEach { field ->
                if (field.id != 0L) {
                    fieldDao.update(field.toEntity().copy(templateId = templateId))
                } else {
                    fieldDao.insert(field.toEntity().copy(templateId = templateId))
                }
            }
        }
    }

    override suspend fun saveTemplateBindings(templateId: Long, tagIds: List<Long>) {
        database.withTransaction {
            bindingDao.deleteByTemplate(templateId)
            if (tagIds.isNotEmpty()) {
                bindingDao.insertAll(
                    tagIds.map { tagId ->
                        EventTemplateTagBindingEntity(templateId = templateId, tagId = tagId)
                    }
                )
            }
        }
    }

    override suspend fun addTagBinding(templateId: Long, tagId: Long) {
        bindingDao.insert(EventTemplateTagBindingEntity(templateId = templateId, tagId = tagId))
    }

    override suspend fun removeTagBinding(templateId: Long, tagId: Long) {
        bindingDao.delete(templateId, tagId)
    }

    override suspend fun getMaxSortOrder(): Int = templateDao.getMaxSortOrder()

    override suspend fun matchTemplateByTags(tagIds: List<Long>): EventTemplate? {
        if (tagIds.isEmpty()) return null
        return bindingDao.matchTemplatesByTags(tagIds).firstOrNull()?.toEventTemplate()
    }
}
