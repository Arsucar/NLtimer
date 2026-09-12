package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.repository.EventTemplateRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MatchTemplateByTagsUseCase 按标签集合匹配打点模板
 * 命中标签数多者优先，其后按模板 sortOrder 升序；无命中返回 null
 * （调用方随后回退上次使用模板 / 通用表单，回退逻辑属录入端 UI 职责）
 */
@Singleton
class MatchTemplateByTagsUseCase @Inject constructor(
    private val eventTemplateRepository: EventTemplateRepository,
) {
    suspend operator fun invoke(tagIds: List<Long>): EventTemplate? =
        eventTemplateRepository.matchTemplateByTags(tagIds)
}
