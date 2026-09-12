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
        val adjustedEnd = newEnd

        if (newStatus == BehaviorNature.COMPLETED) {
            val sameMinutePrevEnd = overlappingBehaviors
                .asSequence()
                .filter { ignoreBehaviorId == null || it.id != ignoreBehaviorId }
                .filter { it.status == BehaviorNature.COMPLETED }
                .mapNotNull { it.endTime }
                .filter { prevEnd ->
                    adjustedStart < prevEnd &&
                        adjustedStart / MILLIS_PER_MINUTE == prevEnd / MILLIS_PER_MINUTE
                }
                .maxOrNull()
            if (sameMinutePrevEnd != null) {
                adjustedStart = sameMinutePrevEnd
            }
            val snapInvertedEnd = sameMinutePrevEnd != null &&
                adjustedEnd != null &&
                adjustedEnd <= adjustedStart
            val effectiveNewEnd = adjustedEnd ?: adjustedStart
            val hasConflict = snapInvertedEnd || (
                effectiveNewEnd > adjustedStart &&
                    hasTimeConflict(
                        newStart = adjustedStart,
                        newEnd = adjustedEnd,
                        newStatus = newStatus,
                        existingBehaviors = overlappingBehaviors,
                        currentTime = currentTime,
                        ignoreBehaviorId = ignoreBehaviorId,
                    )
                )
            return SnapResult(adjustedStart, adjustedEnd, hasConflict)
        }

        if (newStatus == BehaviorNature.ACTIVE) {
            val prevEnd = overlappingBehaviors
                .mapNotNull { it.endTime }
                .filter { it >= adjustedStart }
                .maxOrNull()
            if (prevEnd != null) {
                adjustedStart = prevEnd + 1
            }
        }

        val hasConflict = newStatus == BehaviorNature.ACTIVE &&
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
}
