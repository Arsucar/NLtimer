package com.nltimer.core.data.usecase

import com.nltimer.core.data.repository.BehaviorEventRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeleteEventUseCase 删除行为事件
 * 事件删除后其字段值随外键级联删除
 */
@Singleton
class DeleteEventUseCase @Inject constructor(
    private val behaviorEventRepository: BehaviorEventRepository,
) {
    suspend operator fun invoke(eventId: Long) = behaviorEventRepository.deleteEvent(eventId)
}
