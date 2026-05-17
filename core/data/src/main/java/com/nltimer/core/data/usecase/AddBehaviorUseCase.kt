package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.MILLIS_PER_DAY
import com.nltimer.core.data.util.SnapResult
import com.nltimer.core.data.util.TimeSnapService
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddBehaviorUseCase @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val timeSnapService: TimeSnapService,
    private val clockService: ClockService,
) {
    sealed class Result {
        data class Success(val behaviorId: Long) : Result()
        data class Conflict(val message: String) : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend operator fun invoke(
        activityId: Long,
        tagIds: List<Long>,
        startTime: Long,
        endTime: Long?,
        status: BehaviorNature,
        note: String?,
        editBehaviorId: Long? = null,
        estimatedDurationMs: Long? = null,
    ): Result {
        val now = clockService.currentTimeMillis()

        if (editBehaviorId != null) {
            return executeEdit(editBehaviorId, activityId, tagIds, startTime, endTime, status, note, now)
        }

        val validationError = validateTimeConstraints(startTime, endTime, status, now)
        if (validationError != null) return validationError

        if (status == BehaviorNature.ACTIVE) {
            behaviorRepository.endCurrentBehavior(startTime)
        }

        val snapResult = performSnapAndConflictCheck(startTime, endTime, status, now)
        if (snapResult.hasConflict) {
            return Result.Conflict("该时间段与已有行为记录冲突")
        }

        val finalStart = snapResult.adjustedStart
        val finalEnd = snapResult.adjustedEnd

        val wasPlanned = status == BehaviorNature.PENDING
        val dayBehaviors = getDayBehaviors(status, finalStart)
        val newSequence = calculateSequence(status, finalStart, dayBehaviors)

        updateSubsequentSequences(status, newSequence, dayBehaviors)

        val actualDuration = calculateActualDuration(status, finalStart, finalEnd)

        val id = behaviorRepository.insert(
            Behavior(
                id = 0,
                activityId = activityId,
                startTime = if (status == BehaviorNature.PENDING) 0L else finalStart,
                endTime = if (status == BehaviorNature.COMPLETED) finalEnd ?: finalStart else null,
                status = status,
                note = note,
                pomodoroCount = 0,
                sequence = newSequence,
                estimatedDuration = estimatedDurationMs,
                actualDuration = actualDuration,
                achievementLevel = null,
                wasPlanned = wasPlanned,
            ),
            tagIds = tagIds,
        )
        return Result.Success(id)
    }

    private fun validateTimeConstraints(
        startTime: Long,
        endTime: Long?,
        status: BehaviorNature,
        now: Long,
    ): Result.ValidationError? {
        when (status) {
            BehaviorNature.COMPLETED -> {
                if (endTime != null && endTime > now) {
                    return Result.ValidationError("结束时间不能大于当前时间")
                }
            }
            BehaviorNature.ACTIVE -> {
                if (startTime > now) {
                    return Result.ValidationError("开始时间不能大于当前时间")
                }
            }
            BehaviorNature.PENDING -> {}
        }
        return null
    }

    private suspend fun performSnapAndConflictCheck(
        startTime: Long,
        endTime: Long?,
        status: BehaviorNature,
        now: Long,
    ): SnapResult {
        if (status == BehaviorNature.PENDING) {
            return SnapResult(startTime, endTime, false)
        }

        val snapQueryEnd = when (status) {
            BehaviorNature.ACTIVE -> Long.MAX_VALUE
            BehaviorNature.COMPLETED -> endTime ?: startTime
            BehaviorNature.PENDING -> Long.MAX_VALUE
        }
        val overlapping = behaviorRepository
            .getBehaviorsOverlappingRange(startTime, snapQueryEnd)
            .firstOrNull().orEmpty()

        return timeSnapService.snapAndCheckConflict(
            newStart = startTime,
            newEnd = endTime,
            newStatus = status,
            overlappingBehaviors = overlapping,
            currentTime = now,
        )
    }

    private suspend fun getDayBehaviors(status: BehaviorNature, finalStart: Long): List<Behavior> {
        if (status == BehaviorNature.PENDING) return emptyList()
        val dayStart = getDayStartMillis(finalStart)
        val dayEnd = dayStart + MILLIS_PER_DAY
        return behaviorRepository
            .getByDayRange(dayStart, dayEnd)
            .firstOrNull()
            .orEmpty()
            .filter { it.status != BehaviorNature.PENDING }
            .sortedBy { it.startTime }
    }

    private suspend fun calculateSequence(status: BehaviorNature, finalStart: Long, dayBehaviors: List<Behavior>): Int {
        if (status == BehaviorNature.PENDING) {
            return behaviorRepository.getMaxSequence() + 1
        }
        val insertIndex = dayBehaviors.indexOfFirst { it.startTime > finalStart }
        return if (insertIndex == -1) dayBehaviors.size else insertIndex
    }

    private suspend fun updateSubsequentSequences(status: BehaviorNature, newSequence: Int, dayBehaviors: List<Behavior>) {
        if (status == BehaviorNature.PENDING) return
        dayBehaviors.forEachIndexed { index, behavior ->
            if (index >= newSequence) {
                behaviorRepository.setSequence(behavior.id, index + 1)
            }
        }
    }

    private fun calculateActualDuration(status: BehaviorNature, finalStart: Long, finalEnd: Long?): Long? {
        return when (status) {
            BehaviorNature.COMPLETED -> {
                if (finalEnd != null && finalStart > 0) finalEnd - finalStart else null
            }
            BehaviorNature.ACTIVE -> {
                if (finalStart > 0) clockService.currentTimeMillis() - finalStart else null
            }
            BehaviorNature.PENDING -> null
        }
    }

    private suspend fun executeEdit(
        behaviorId: Long,
        activityId: Long,
        tagIds: List<Long>,
        startTime: Long,
        endTime: Long?,
        status: BehaviorNature,
        note: String?,
        now: Long,
    ): Result {
        val validationError = validateTimeConstraints(startTime, endTime, status, now)
        if (validationError != null) return validationError

        behaviorRepository.updateBehavior(
            id = behaviorId,
            activityId = activityId,
            startTime = if (status == BehaviorNature.PENDING) 0L else startTime,
            endTime = if (status == BehaviorNature.COMPLETED) endTime ?: startTime else null,
            status = status.key,
            note = note,
        )
        behaviorRepository.updateTagsForBehavior(behaviorId, tagIds)
        return Result.Success(behaviorId)
    }

    private fun getDayStartMillis(timestamp: Long): Long {
        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
        val startOfDay = zonedDateTime.toLocalDate()
            .atStartOfDay(java.time.ZoneId.systemDefault())
        return startOfDay.toInstant().toEpochMilli()
    }
}
