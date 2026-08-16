package com.nltimer.core.data.util

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature

/**
 * 检测新行为与已有行为列表是否存在时间冲突
 *
 * 时间区间使用半开区间 [start, end)：
 * - start 包含在区间内
 * - end 不包含在区间内
 * - 边界相接（如 [10:00, 11:00) 和 [11:00, 12:00)）不冲突
 *
 * ACTIVE 行为视为 [startTime, +∞)
 * PENDING 行为不参与冲突检测
 *
 * @param newStart 新行为开始时间（epoch millis）
 * @param newEnd 新行为结束时间，null 表示未结束
 * @param newStatus 新行为状态
 * @param existingBehaviors 已有行为列表
 * @param currentTime 保留参数以兼容命名调用；ACTIVE 按设计视为 [start, +∞)，不使用 now 截断
 * @param ignoreBehaviorId 需要忽略的行为 ID（编辑场景使用，新增时传 null）
 * @return true 表示存在冲突
 */
fun hasTimeConflict(
    newStart: Long,
    newEnd: Long?,
    newStatus: BehaviorNature,
    existingBehaviors: List<Behavior>,
    @Suppress("UnusedParameter")
    currentTime: Long = System.currentTimeMillis(),
    ignoreBehaviorId: Long? = null,
): Boolean {
    // currentTime 未使用：ACTIVE 设计为 [start, +∞)，保留参数避免破坏调用方命名参数
    if (newStatus == BehaviorNature.PENDING) return false
    if (newStart <= 0L) return false

    val effectiveNewEnd = when (newStatus) {
        BehaviorNature.ACTIVE -> Long.MAX_VALUE
        BehaviorNature.COMPLETED -> newEnd ?: return false
        BehaviorNature.PENDING -> return false
    }

    if (effectiveNewEnd <= newStart) return false

    return existingBehaviors.any { existing ->
        if (ignoreBehaviorId != null && existing.id == ignoreBehaviorId) return@any false
        if (existing.status == BehaviorNature.PENDING) return@any false
        if (existing.startTime <= 0L) return@any false

        val existingEnd = when {
            existing.endTime != null -> existing.endTime
            existing.status == BehaviorNature.ACTIVE -> Long.MAX_VALUE
            else -> return@any false
        }

        if (existingEnd <= existing.startTime) return@any false

        newStart < existingEnd && existing.startTime < effectiveNewEnd
    }
}
