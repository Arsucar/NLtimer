package com.nltimer.core.behaviorui.sheet

import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.SecondsStrategy
import java.time.LocalDateTime

internal fun resolveBehaviorStartTime(
    mode: BehaviorNature,
    userAdjustedStart: Boolean,
    startTime: LocalDateTime,
    initialStartTime: LocalDateTime?,
    strategy: SecondsStrategy,
    sheetOpenTime: LocalDateTime,
    confirmTime: LocalDateTime,
): LocalDateTime {
    if (userAdjustedStart) return startTime
    if (mode == BehaviorNature.COMPLETED) {
        return initialStartTime ?: startTime
    }
    val sourceSeconds = when (strategy) {
        SecondsStrategy.OPEN_TIME -> sheetOpenTime.second
        SecondsStrategy.CONFIRM_TIME -> confirmTime.second
    }
    return startTime.withSecond(sourceSeconds).withNano(0)
}

internal fun resolveBehaviorEndTime(
    mode: BehaviorNature,
    userAdjustedEnd: Boolean,
    endTime: LocalDateTime,
    initialEndTime: LocalDateTime?,
): LocalDateTime? {
    if (mode != BehaviorNature.COMPLETED) return null
    return if (userAdjustedEnd) endTime else initialEndTime ?: endTime
}

internal fun hasMinuteLevelChange(current: LocalDateTime, candidate: LocalDateTime): Boolean {
    return current.withSecond(0).withNano(0) != candidate.withSecond(0).withNano(0)
}

internal fun prevEndAdjustmentTarget(
    prevEndTime: LocalDateTime?,
    now: LocalDateTime,
): LocalDateTime = prevEndTime ?: now
