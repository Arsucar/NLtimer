package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.repository.TagRepository
import javax.inject.Inject

/**
 * 添加标签用例。
 *
 * 封装 Tag 构造、插入和数据绑定逻辑，避免在多个 ViewModel 中重复。
 */
class AddTagUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(
        name: String,
        color: Long?,
        iconKey: String?,
        priority: Int,
        category: String?,
        keywords: String?,
        activityId: Long?,
    ): Long {
        val tag = Tag(
            id = 0,
            name = name,
            color = color,
            iconKey = iconKey,
            category = category,
            groupId = null,
            priority = priority,
            usageCount = 0,
            sortOrder = 0,
            keywords = keywords,
            isArchived = false,
        )
        val tagId = tagRepository.insert(tag)
        if (activityId != null) {
            tagRepository.setActivityTagBindings(tagId, listOf(activityId))
        }
        return tagId
    }
}
