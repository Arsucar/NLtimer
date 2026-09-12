package com.nltimer.core.behaviorui.sheet

import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.model.SecondsStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AddBehaviorTimeResolveTest {

    private val initialStart = LocalDateTime.of(2026, 9, 11, 10, 4, 37, 500_000_000)
    private val truncatedStart = LocalDateTime.of(2026, 9, 11, 10, 4, 0)
    private val adjustedStart = LocalDateTime.of(2026, 9, 11, 10, 10, 0)
    private val initialEnd = LocalDateTime.of(2026, 9, 11, 21, 21, 12)
    private val truncatedEnd = LocalDateTime.of(2026, 9, 11, 21, 21, 0)
    private val sheetOpen = LocalDateTime.of(2026, 9, 11, 22, 0, 15)
    private val confirm = LocalDateTime.of(2026, 9, 11, 22, 1, 40)

    @Test
    fun `completed resolveStart keeps initial millis when start minute was not adjusted`() {
        val resolved = resolveBehaviorStartTime(
            mode = BehaviorNature.COMPLETED,
            userAdjustedStart = false,
            startTime = truncatedStart,
            initialStartTime = initialStart,
            strategy = SecondsStrategy.OPEN_TIME,
            sheetOpenTime = sheetOpen,
            confirmTime = confirm,
        )
        assertEquals(initialStart, resolved)
    }

    @Test
    fun `completed resolveStart uses stored startTime after start minute was adjusted`() {
        val resolved = resolveBehaviorStartTime(
            mode = BehaviorNature.COMPLETED,
            userAdjustedStart = true,
            startTime = adjustedStart,
            initialStartTime = initialStart,
            strategy = SecondsStrategy.OPEN_TIME,
            sheetOpenTime = sheetOpen,
            confirmTime = confirm,
        )
        assertEquals(adjustedStart, resolved)
    }

    @Test
    fun `completed resolveStart keeps leftover seconds from prev-end jump`() {
        val resolved = resolveBehaviorStartTime(
            mode = BehaviorNature.COMPLETED,
            userAdjustedStart = true,
            startTime = initialStart,
            initialStartTime = truncatedStart,
            strategy = SecondsStrategy.OPEN_TIME,
            sheetOpenTime = sheetOpen,
            confirmTime = confirm,
        )
        assertEquals(initialStart, resolved)
    }

    @Test
    fun `completed resolveEnd keeps initial millis when end was not adjusted`() {
        val resolved = resolveBehaviorEndTime(
            mode = BehaviorNature.COMPLETED,
            userAdjustedEnd = false,
            endTime = truncatedEnd,
            initialEndTime = initialEnd,
        )
        assertEquals(initialEnd, resolved)
    }

    @Test
    fun `completed resolveEnd uses stored endTime after end was adjusted`() {
        val resolved = resolveBehaviorEndTime(
            mode = BehaviorNature.COMPLETED,
            userAdjustedEnd = true,
            endTime = truncatedEnd,
            initialEndTime = initialEnd,
        )
        assertEquals(truncatedEnd, resolved)
    }

    @Test
    fun `active resolveEnd is null`() {
        assertNull(
            resolveBehaviorEndTime(
                mode = BehaviorNature.ACTIVE,
                userAdjustedEnd = true,
                endTime = initialEnd,
                initialEndTime = initialEnd,
            ),
        )
    }

    @Test
    fun `picker does not count truncated-only start as a minute change`() {
        assertFalse(hasMinuteLevelChange(initialStart, truncatedStart))
    }

    @Test
    fun `picker counts a different minute as a change`() {
        assertTrue(hasMinuteLevelChange(initialStart, adjustedStart))
    }

    @Test
    fun `prev end jump keeps leftover seconds`() {
        assertEquals(initialStart, prevEndAdjustmentTarget(initialStart, confirm))
    }

    @Test
    fun `prev end jump falls back to now when missing`() {
        assertEquals(confirm, prevEndAdjustmentTarget(null, confirm))
    }
}
