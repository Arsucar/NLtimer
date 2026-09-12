package com.nltimer.core.data.usecase

import com.nltimer.core.data.repository.BehaviorEventRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConvertEventToIndependentUseCase 事件转独立事件
 * 将事件的 behaviorId 置空，保留 activityId 与全部字段值
 * （删除含事件行为时“保留事件”选项的执行入口；已独立的重复转换幂等返回）
 */
@Singleton
class ConvertEventToIndependentUseCase @Inject constructor(
    private val behaviorEventRepository: BehaviorEventRepository,
) {
    sealed class Result {
        data class Success(val eventId: Long) : Result()
        data object NotFound : Result()
        data object AlreadyIndependent : Result()
    }

    suspend operator fun invoke(eventId: Long): Result {
        val event = behaviorEventRepository.getEventById(eventId) ?: return Result.NotFound
        if (event.behaviorId == null) return Result.AlreadyIndependent
        behaviorEventRepository.setEventBehaviorId(eventId, null)
        return Result.Success(eventId)
    }
}
