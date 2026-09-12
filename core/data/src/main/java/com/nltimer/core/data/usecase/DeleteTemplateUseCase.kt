package com.nltimer.core.data.usecase

import com.nltimer.core.data.repository.EventTemplateRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeleteTemplateUseCase 删除打点模板
 * 模板删除后，模板字段、标签绑定与该模板下的全部事件及其字段值随外键级联删除
 * （UI 层需先行弹确认说明对存量事件的影响）
 */
@Singleton
class DeleteTemplateUseCase @Inject constructor(
    private val eventTemplateRepository: EventTemplateRepository,
) {
    suspend operator fun invoke(templateId: Long) = eventTemplateRepository.deleteTemplate(templateId)
}
