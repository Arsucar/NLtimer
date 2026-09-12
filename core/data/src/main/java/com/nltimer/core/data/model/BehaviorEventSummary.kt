package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

/**
 * BehaviorEventSummary 行为事件聚合摘要
 * 首页活跃卡片徽标用：单个行为的 @property eventCount 与最新 @property latestTimestamp
 * 由 BehaviorEventRepository.observeSummariesForBehaviors 批量产出（key = behaviorId）
 */
@Immutable
data class BehaviorEventSummary(
    val behaviorId: Long,
    val eventCount: Int,
    val latestTimestamp: Long?,
)
