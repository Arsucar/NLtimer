package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.model.BehaviorEventSummary
import com.nltimer.core.data.model.BehaviorEventWithValues
import com.nltimer.core.data.model.EventQueryScope
import com.nltimer.core.data.repository.BehaviorEventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ObserveEventsUseCase 观察事件列表
 * 按查询范围（全部 / 活动 / 标签 / 行为 / 模板）分发；详情组装变体附带字段值
 */
@Singleton
class ObserveEventsUseCase @Inject constructor(
    private val behaviorEventRepository: BehaviorEventRepository,
) {
    fun observe(scope: EventQueryScope): Flow<List<BehaviorEvent>> =
        behaviorEventRepository.observeEvents(scope)

    fun observeWithValues(scope: EventQueryScope): Flow<List<BehaviorEventWithValues>> =
        behaviorEventRepository.observeEventsWithValues(scope)

    fun observeSummariesForBehaviors(behaviorIds: List<Long>): Flow<Map<Long, BehaviorEventSummary>> =
        behaviorEventRepository.observeSummariesForBehaviors(behaviorIds)
}
