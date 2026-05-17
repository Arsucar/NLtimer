package com.nltimer.core.data.util

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature

data class SnapResult(
    val adjustedStart: Long,
    val adjustedEnd: Long?,
    val hasConflict: Boolean,
)

class TimeSnapService {
    fun snapAndCheckConflict(
        newStart: Long,
        newEnd: Long?,
        newStatus: BehaviorNature,
        overlappingBehaviors: List<Behavior>,
        currentTime: Long = System.currentTimeMillis(),
        ignoreBehaviorId: Long? = null,
    ): SnapResult {
        var adjustedStart = newStart
        var adjustedEnd = newEnd

        if (newStatus == BehaviorNature.COMPLETED) {
            val effectiveNewEnd = adjustedEnd ?: adjustedStart
            val hasConflict = effectiveNewEnd > adjustedStart &&
                hasTimeConflict(
                    newStart = adjustedStart,
                    newEnd = adjustedEnd,
                    newStatus = newStatus,
                    existingBehaviors = overlappingBehaviors,
                    currentTime = currentTime,
                    ignoreBehaviorId = ignoreBehaviorId,
                )
            return SnapResult(adjustedStart, adjustedEnd, hasConflict)
        }

        if (newStatus != BehaviorNature.PENDING) {
            val prevBehavior = overlappingBehaviors
                .filter { it.endTime != null && it.endTime >= adjustedStart }
                .maxByOrNull { it.endTime!! }
            val prevEnd = prevBehavior?.endTime
            if (prevEnd != null && prevEnd >= adjustedStart) {
                adjustedStart = prevEnd + 1
                if (newStatus == BehaviorNature.COMPLETED && adjustedEnd != null) {
                    if (newEnd / MILLIS_PER_MINUTE == prevEnd / MILLIS_PER_MINUTE) {
                        adjustedEnd = prevEnd / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE + 59_999
                    }
                }
            }
        }

        val hasConflict = if (newStatus != BehaviorNature.PENDING) {
            val effectiveNewEnd = when (newStatus) {
                BehaviorNature.ACTIVE -> Long.MAX_VALUE
                BehaviorNature.COMPLETED -> adjustedEnd ?: adjustedStart
                BehaviorNature.PENDING -> null
            }
            effectiveNewEnd != null && effectiveNewEnd > adjustedStart &&
                hasTimeConflict(
                    newStart = adjustedStart,
                    newEnd = adjustedEnd,
                    newStatus = newStatus,
                    existingBehaviors = overlappingBehaviors,
                    currentTime = currentTime,
                    ignoreBehaviorId = ignoreBehaviorId,
                )
        } else false

        return SnapResult(adjustedStart, adjustedEnd, hasConflict)
    }
}
