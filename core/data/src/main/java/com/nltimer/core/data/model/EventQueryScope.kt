package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

/**
 * EventQueryScope 事件查询范围
 * ObserveEventsUseCase 按此范围分发到仓库对应的观察查询
 */
@Immutable
sealed interface EventQueryScope {
    /** 全部事件混排（timestamp 倒序，stats 事件面板混排视图） */
    data object All : EventQueryScope

    /** 按活动查询 */
    data class ByActivity(val activityId: Long) : EventQueryScope

    /** 按标签查询（经 behavior_tag_cross_ref 反向 JOIN） */
    data class ByTag(val tagId: Long) : EventQueryScope

    /** 按行为查询（行为详情弹窗事件分区） */
    data class ByBehavior(val behaviorId: Long) : EventQueryScope

    /** 按模板聚焦查询 */
    data class ByTemplate(val templateId: Long) : EventQueryScope
}
