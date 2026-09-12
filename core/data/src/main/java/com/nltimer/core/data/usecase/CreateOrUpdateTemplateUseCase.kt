package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.util.ClockService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CreateOrUpdateTemplateUseCase 创建 / 更新打点模板
 * 校验模板名与字段结构后经仓库写库：insert/update 模板行 + 事务内整体替换字段与绑定
 */
@Singleton
class CreateOrUpdateTemplateUseCase @Inject constructor(
    private val eventTemplateRepository: EventTemplateRepository,
    private val clockService: ClockService,
) {
    sealed class Result {
        data class Success(val templateId: Long) : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend operator fun invoke(
        editTemplateId: Long? = null,
        name: String,
        description: String? = null,
        fields: List<EventTemplateField>,
        tagIds: List<Long> = emptyList(),
    ): Result {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return Result.ValidationError("模板名称不能为空")

        val normalizedFields = fields.mapIndexed { index, field ->
            field.copy(name = field.name.trim(), sortOrder = index)
        }
        for (field in normalizedFields) {
            if (field.name.isEmpty()) return Result.ValidationError("字段名称不能为空")
            if (field.type == EventFieldType.SELECT && field.options.isEmpty()) {
                return Result.ValidationError("单选字段“${field.name}”缺少选项")
            }
        }
        val duplicateName = normalizedFields
            .groupBy { it.name }
            .filterValues { it.size > 1 }
            .keys
            .firstOrNull()
        if (duplicateName != null) return Result.ValidationError("字段名称重复：$duplicateName")

        val existingByName = eventTemplateRepository.getTemplateByName(trimmedName)
        if (existingByName != null && existingByName.id != editTemplateId) {
            return Result.ValidationError("模板名称已存在")
        }

        val now = clockService.currentTimeMillis()
        val savedId: Long = if (editTemplateId == null) {
            val maxSortOrder = eventTemplateRepository.getMaxSortOrder()
            eventTemplateRepository.insertTemplate(
                EventTemplate(
                    id = 0,
                    name = trimmedName,
                    description = description,
                    createdAt = now,
                    sortOrder = maxSortOrder + 1,
                )
            )
        } else {
            val existing = eventTemplateRepository.getTemplateById(editTemplateId)
                ?: return Result.ValidationError("模板不存在")
            eventTemplateRepository.updateTemplate(
                existing.copy(name = trimmedName, description = description)
            )
            existing.id
        }

        eventTemplateRepository.saveTemplateFields(savedId, normalizedFields)
        eventTemplateRepository.saveTemplateBindings(savedId, tagIds.distinct())
        return Result.Success(savedId)
    }
}
