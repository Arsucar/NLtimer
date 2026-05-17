package com.nltimer.core.data.util

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimeSnapServiceTest {

    private lateinit var service: TimeSnapService

    @Before
    fun setUp() {
        service = TimeSnapService()
    }

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
    fun `no snap and no conflict when no overlapping behaviors`() {
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = emptyList(),
            currentTime = 30_000,
        )
        assertEquals(10_000L, result.adjustedStart)
        assertEquals(20_000L, result.adjustedEnd)
        assertFalse(result.hasConflict)
    }

    @Test
    fun `completed boundary touch does not snap or conflict`() {
        val prev = createBehavior(1, 5_000, 10_000, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(prev),
            currentTime = 30_000,
        )
        assertEquals(10_000L, result.adjustedStart)
        assertEquals(20_000L, result.adjustedEnd)
        assertFalse(result.hasConflict)
    }

    @Test
    fun `completed overlap returns conflict without changing requested times`() {
        val prevEnd = 60_000L + 30_000L
        val prev = createBehavior(1, 60_000, prevEnd, BehaviorNature.COMPLETED)
        val newEnd = 60_000L + 45_000L
        val result = service.snapAndCheckConflict(
            newStart = 60_000L,
            newEnd = newEnd,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(prev),
            currentTime = 120_000,
        )
        assertEquals(60_000L, result.adjustedStart)
        assertEquals(newEnd, result.adjustedEnd)
        assertTrue(result.hasConflict)
    }

    @Test
    fun `completed overlap crossing minute boundary returns conflict without changing requested times`() {
        val prevEnd = 60_000L + 30_000L
        val prev = createBehavior(1, 60_000, prevEnd, BehaviorNature.COMPLETED)
        val newEnd = 120_000L + 15_000L
        val result = service.snapAndCheckConflict(
            newStart = 60_000L,
            newEnd = newEnd,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(prev),
            currentTime = 200_000,
        )
        assertEquals(60_000L, result.adjustedStart)
        assertEquals(newEnd, result.adjustedEnd)
        assertTrue(result.hasConflict)
    }

    @Test
    fun `completed containing existing range returns conflict`() {
        val existing = createBehavior(1, 60_000L, 180_000L, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 60_000L,
            newEnd = 300_000L,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(existing),
            currentTime = 500_000L,
        )
        assertEquals(60_000L, result.adjustedStart)
        assertTrue(result.hasConflict)
    }

    @Test
    fun `ACTIVE behavior with null newEnd snap adjusts start past existing`() {
        val existing = createBehavior(1, 60_000L, 180_000L, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 60_000L,
            newEnd = null,
            newStatus = BehaviorNature.ACTIVE,
            overlappingBehaviors = listOf(existing),
            currentTime = 500_000L,
        )
        assertEquals(180_001L, result.adjustedStart)
        assertFalse(result.hasConflict)
    }

    @Test
    fun `PENDING behavior does not snap or check conflict`() {
        val existing = createBehavior(1, 5_000, 15_000, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.PENDING,
            overlappingBehaviors = listOf(existing),
            currentTime = 30_000,
        )
        assertEquals(10_000L, result.adjustedStart)
        assertEquals(20_000L, result.adjustedEnd)
        assertFalse(result.hasConflict)
    }

    @Test
    fun `ignoreBehaviorId excludes specified behavior from conflict check`() {
        val existing = createBehavior(1, 15_000, 25_000, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(existing),
            currentTime = 30_000,
            ignoreBehaviorId = 1,
        )
        assertFalse(result.hasConflict)
    }

    @Test
    fun `completed does not adjust start when prevEnd equals newStart`() {
        val prev = createBehavior(1, 0, 10_000, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(prev),
            currentTime = 30_000,
        )
        assertEquals(10_000L, result.adjustedStart)
        assertFalse(result.hasConflict)
    }

    @Test
    fun `multiple overlapping completed behaviors return conflict without snapping`() {
        val prev1 = createBehavior(1, 1_000, 8_000, BehaviorNature.COMPLETED)
        val prev2 = createBehavior(2, 2_000, 12_000, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = listOf(prev1, prev2),
            currentTime = 30_000,
        )
        assertEquals(10_000L, result.adjustedStart)
        assertTrue(result.hasConflict)
    }

    @Test
    fun `empty overlapping list returns original times`() {
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.COMPLETED,
            overlappingBehaviors = emptyList(),
            currentTime = 30_000,
        )
        assertEquals(10_000L, result.adjustedStart)
        assertEquals(20_000L, result.adjustedEnd)
        assertFalse(result.hasConflict)
    }

    @Test
    fun `PENDING with overlapping behaviors still no conflict`() {
        val existing = createBehavior(1, 5_000, 25_000, BehaviorNature.COMPLETED)
        val result = service.snapAndCheckConflict(
            newStart = 10_000,
            newEnd = 20_000,
            newStatus = BehaviorNature.PENDING,
            overlappingBehaviors = listOf(existing),
            currentTime = 30_000,
        )
        assertFalse(result.hasConflict)
    }
}
