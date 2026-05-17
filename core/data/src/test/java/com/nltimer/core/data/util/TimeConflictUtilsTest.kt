package com.nltimer.core.data.util

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeConflictUtilsTest {

    private fun createBehavior(
        id: Long,
        startTime: Long,
        endTime: Long?,
        status: BehaviorNature,
    ) = Behavior(
        id = id,
        activityId = 1,
        startTime = startTime,
        endTime = endTime,
        status = status,
        note = null,
        pomodoroCount = 0,
        sequence = 0,
        estimatedDuration = null,
        actualDuration = null,
        achievementLevel = null,
        wasPlanned = false,
    )

    @Test
    fun `no conflict when intervals do not overlap`() {
        val existing = listOf(createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(2000, 3000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `no conflict when boundary touches`() {
        val existing = listOf(createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(2000, 3000, BehaviorNature.COMPLETED, existing))
        assertFalse(hasTimeConflict(0, 1000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `conflict when intervals overlap`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED))
        assertTrue(hasTimeConflict(2000, 4000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `conflict when new interval contains existing`() {
        val existing = listOf(createBehavior(1, 2000, 3000, BehaviorNature.COMPLETED))
        assertTrue(hasTimeConflict(1000, 4000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `conflict when existing interval contains new`() {
        val existing = listOf(createBehavior(1, 1000, 4000, BehaviorNature.COMPLETED))
        assertTrue(hasTimeConflict(2000, 3000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `pending behavior does not conflict`() {
        val existing = listOf(createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(1500, 2500, BehaviorNature.PENDING, existing))
    }

    @Test
    fun `active behavior conflicts with any future interval`() {
        val existing = listOf(createBehavior(1, 1000, null, BehaviorNature.ACTIVE))
        assertTrue(hasTimeConflict(2000, 3000, BehaviorNature.COMPLETED, existing, currentTime = 5000))
    }

    @Test
    fun `active behavior does not conflict with past interval`() {
        val existing = listOf(createBehavior(1, 3000, null, BehaviorNature.ACTIVE))
        assertFalse(hasTimeConflict(1000, 2000, BehaviorNature.COMPLETED, existing, currentTime = 5000))
    }

    @Test
    fun `completed without endTime returns false`() {
        val existing = listOf(createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(3000, null, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `ignore specified behavior id`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(2000, 4000, BehaviorNature.COMPLETED, existing, ignoreBehaviorId = 1))
    }

    @Test
    fun `zero length interval returns false`() {
        val existing = listOf(createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(3000, 3000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `cross day behavior conflicts`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED))
        assertTrue(hasTimeConflict(2000, 4000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `newStart zero returns false`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(0, 2000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `newStart negative returns false`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(-100, 2000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `existing pending behavior is skipped`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.PENDING))
        assertFalse(hasTimeConflict(2000, 2500, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `existing behavior with zero startTime is skipped`() {
        val existing = listOf(createBehavior(1, 0, 3000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(1000, 2000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `existing completed with null endTime is skipped`() {
        val existing = listOf(createBehavior(1, 1000, null, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(1500, 2500, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `existing with endTime before startTime is skipped`() {
        val existing = listOf(createBehavior(1, 3000, 1000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(1500, 2500, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `multiple existing behaviors only one conflicts`() {
        val existing = listOf(
            createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED),
            createBehavior(2, 4000, 5000, BehaviorNature.COMPLETED),
        )
        assertTrue(hasTimeConflict(1500, 4500, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `active new behavior conflicts with completed existing`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED))
        assertTrue(hasTimeConflict(2000, null, BehaviorNature.ACTIVE, existing))
    }

    @Test
    fun `pending existing behavior does not conflict with active new`() {
        val existing = listOf(createBehavior(1, 1000, 3000, BehaviorNature.PENDING))
        assertFalse(hasTimeConflict(2000, null, BehaviorNature.ACTIVE, existing))
    }

    @Test
    fun `ignoreBehaviorId only ignores specified id`() {
        val existing = listOf(
            createBehavior(1, 1000, 3000, BehaviorNature.COMPLETED),
            createBehavior(2, 2000, 4000, BehaviorNature.COMPLETED),
        )
        assertTrue(hasTimeConflict(2500, 3500, BehaviorNature.COMPLETED, existing, ignoreBehaviorId = 1))
    }

    @Test
    fun `empty existing list never conflicts`() {
        assertFalse(hasTimeConflict(1000, 2000, BehaviorNature.COMPLETED, emptyList()))
    }

    @Test
    fun `two active behaviors always conflict`() {
        val existing = listOf(createBehavior(1, 1000, null, BehaviorNature.ACTIVE))
        assertTrue(hasTimeConflict(2000, null, BehaviorNature.ACTIVE, existing, currentTime = 5000))
    }

    @Test
    fun `endTime equal to existing startTime does not conflict`() {
        val existing = listOf(createBehavior(1, 2000, 3000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(1000, 2000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `startTime equal to existing endTime does not conflict`() {
        val existing = listOf(createBehavior(1, 1000, 2000, BehaviorNature.COMPLETED))
        assertFalse(hasTimeConflict(2000, 3000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `idle range inside adjacent millisecond boundaries does not conflict`() {
        val existing = listOf(
            createBehavior(1, 10_000, 15_000, BehaviorNature.COMPLETED),
            createBehavior(2, 18_000, 20_000, BehaviorNature.COMPLETED),
        )

        assertFalse(hasTimeConflict(15_001, 18_000, BehaviorNature.COMPLETED, existing))
    }

    @Test
    fun `very large time range conflicts`() {
        val existing = listOf(createBehavior(1, Long.MAX_VALUE - 1000, null, BehaviorNature.ACTIVE))
        assertTrue(hasTimeConflict(Long.MAX_VALUE - 500, null, BehaviorNature.ACTIVE, existing, currentTime = Long.MAX_VALUE))
    }
}
