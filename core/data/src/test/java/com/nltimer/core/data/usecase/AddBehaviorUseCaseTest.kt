package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.core.data.repository.BehaviorRepository
import com.nltimer.core.data.util.ClockService
import com.nltimer.core.data.util.SnapResult
import com.nltimer.core.data.util.TimeSnapService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddBehaviorUseCaseTest {

    private lateinit var behaviorRepository: BehaviorRepository
    private lateinit var timeSnapService: TimeSnapService
    private lateinit var clockService: ClockService
    private lateinit var useCase: AddBehaviorUseCase

    private val fixedNow = 1700000000000L

    @Before
    fun setup() {
        behaviorRepository = mockk(relaxed = true)
        timeSnapService = mockk()
        clockService = mockk()
        every { clockService.currentTimeMillis() } returns fixedNow
        useCase = AddBehaviorUseCase(behaviorRepository, timeSnapService, clockService)
    }

    private suspend fun invokeUseCase(
        activityId: Long = 1L,
        tagIds: List<Long> = emptyList(),
        startTime: Long,
        endTime: Long? = null,
        status: BehaviorNature,
        note: String? = null,
        editBehaviorId: Long? = null,
    ) = useCase(activityId, tagIds, startTime, endTime, status, note, editBehaviorId)

    // --- Validation ---

    @Test
    fun `COMPLETED with endTime in future returns ValidationError`() = runTest {
        val result = invokeUseCase(
            startTime = fixedNow - 5000, endTime = fixedNow + 10000,
            status = BehaviorNature.COMPLETED,
        )
        assertTrue(result is AddBehaviorUseCase.Result.ValidationError)
    }

    @Test
    fun `ACTIVE with startTime in future returns ValidationError`() = runTest {
        val result = invokeUseCase(
            startTime = fixedNow + 10000,
            status = BehaviorNature.ACTIVE,
        )
        assertTrue(result is AddBehaviorUseCase.Result.ValidationError)
    }

    @Test
    fun `PENDING never returns ValidationError regardless of times`() = runTest {
        coEvery { behaviorRepository.getMaxSequence() } returns -1
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 1L

        val result = invokeUseCase(
            startTime = fixedNow + 100000, endTime = fixedNow + 200000,
            status = BehaviorNature.PENDING,
        )
        assertTrue(result is AddBehaviorUseCase.Result.Success)
    }

    // --- ACTIVE behavior ---

    @Test
    fun `ACTIVE ends current behavior before creating new one`() = runTest {
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 1000, null, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 42L

        invokeUseCase(startTime = fixedNow - 1000, status = BehaviorNature.ACTIVE)

        coVerify { behaviorRepository.endCurrentBehavior(fixedNow - 1000) }
    }

    @Test
    fun `ACTIVE with conflict returns Conflict result`() = runTest {
        coEvery { behaviorRepository.getBehaviorsOverlappingRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 1000, null, true)

        val result = invokeUseCase(startTime = fixedNow - 1000, status = BehaviorNature.ACTIVE)

        assertTrue(result is AddBehaviorUseCase.Result.Conflict)
    }

    @Test
    fun `ACTIVE without conflict inserts behavior and returns Success`() = runTest {
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 1000, null, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 99L

        val result = invokeUseCase(
            tagIds = listOf(10L, 20L),
            startTime = fixedNow - 1000,
            status = BehaviorNature.ACTIVE, note = "test note",
        )

        assertTrue(result is AddBehaviorUseCase.Result.Success)
        assertEquals(99L, (result as AddBehaviorUseCase.Result.Success).behaviorId)
    }

    @Test
    fun `ACTIVE behavior startTime is set to snapped value`() = runTest {
        val snappedStart = fixedNow - 5000
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(snappedStart, null, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = fixedNow - 1000, status = BehaviorNature.ACTIVE)

        assertEquals(snappedStart, behaviorSlot.captured.startTime)
    }

    // --- COMPLETED behavior ---

    @Test
    fun `COMPLETED without conflict inserts with correct times`() = runTest {
        val start = fixedNow - 60000
        val end = fixedNow - 10000
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(start, end, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = start, endTime = end, status = BehaviorNature.COMPLETED)

        assertEquals(start, behaviorSlot.captured.startTime)
        assertEquals(end, behaviorSlot.captured.endTime)
        assertEquals(BehaviorNature.COMPLETED, behaviorSlot.captured.status)
    }

    @Test
    fun `COMPLETED with endTime null uses snapped time`() = runTest {
        val start = fixedNow - 60000
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(start, start, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = start, endTime = null, status = BehaviorNature.COMPLETED)

        assertEquals(start, behaviorSlot.captured.endTime)
    }

    @Test
    fun `COMPLETED does not call endCurrentBehavior`() = runTest {
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 60000, fixedNow, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 1L

        invokeUseCase(
            startTime = fixedNow - 60000, endTime = fixedNow,
            status = BehaviorNature.COMPLETED,
        )

        coVerify(exactly = 0) { behaviorRepository.endCurrentBehavior(any()) }
    }

    // --- PENDING behavior ---

    @Test
    fun `PENDING sets startTime to 0 and endTime to null`() = runTest {
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getMaxSequence() } returns 5
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(
            startTime = fixedNow + 100000, endTime = fixedNow + 200000,
            status = BehaviorNature.PENDING, note = "planned",
        )

        assertEquals(0L, behaviorSlot.captured.startTime)
        assertEquals(null, behaviorSlot.captured.endTime)
        assertEquals(BehaviorNature.PENDING, behaviorSlot.captured.status)
    }

    @Test
    fun `PENDING sets sequence to maxSequence plus one`() = runTest {
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getMaxSequence() } returns 3
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = 0, status = BehaviorNature.PENDING)

        assertEquals(4, behaviorSlot.captured.sequence)
    }

    @Test
    fun `PENDING skips conflict check`() = runTest {
        coEvery { behaviorRepository.getMaxSequence() } returns -1
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 1L

        invokeUseCase(startTime = 0, status = BehaviorNature.PENDING)

        coVerify(exactly = 0) { behaviorRepository.getBehaviorsOverlappingRange(any(), any()) }
    }

    @Test
    fun `PENDING wasPlanned is true`() = runTest {
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getMaxSequence() } returns -1
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = 0, status = BehaviorNature.PENDING)

        assertTrue(behaviorSlot.captured.wasPlanned)
    }

    // --- Sequence calculation ---

    @Test
    fun `sequence is 0 when no existing behaviors for the day`() = runTest {
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow, null, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = fixedNow, status = BehaviorNature.ACTIVE)

        assertEquals(0, behaviorSlot.captured.sequence)
    }

    @Test
    fun `sequence is inserted at correct position among existing behaviors`() = runTest {
        val behaviorSlot = slot<Behavior>()
        val existingBehaviors = listOf(
            Behavior(id = 1, activityId = 1, startTime = fixedNow - 10000, endTime = fixedNow - 5000,
                status = BehaviorNature.COMPLETED, note = null, pomodoroCount = 0, sequence = 0,
                estimatedDuration = null, actualDuration = null, achievementLevel = null, wasPlanned = false),
            Behavior(id = 2, activityId = 1, startTime = fixedNow + 10000, endTime = fixedNow + 20000,
                status = BehaviorNature.COMPLETED, note = null, pomodoroCount = 0, sequence = 1,
                estimatedDuration = null, actualDuration = null, achievementLevel = null, wasPlanned = false),
        )
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(existingBehaviors)
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow, null, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 3L

        invokeUseCase(startTime = fixedNow, status = BehaviorNature.ACTIVE)

        assertEquals(1, behaviorSlot.captured.sequence)
    }

    @Test
    fun `subsequent sequences are shifted when new behavior is inserted in middle`() = runTest {
        val existingBehaviors = listOf(
            Behavior(id = 1, activityId = 1, startTime = fixedNow - 10000, endTime = fixedNow - 5000,
                status = BehaviorNature.COMPLETED, note = null, pomodoroCount = 0, sequence = 0,
                estimatedDuration = null, actualDuration = null, achievementLevel = null, wasPlanned = false),
            Behavior(id = 2, activityId = 1, startTime = fixedNow + 5000, endTime = fixedNow + 10000,
                status = BehaviorNature.COMPLETED, note = null, pomodoroCount = 0, sequence = 1,
                estimatedDuration = null, actualDuration = null, achievementLevel = null, wasPlanned = false),
        )
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(existingBehaviors)
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow, null, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 3L

        invokeUseCase(startTime = fixedNow, status = BehaviorNature.ACTIVE)

        coVerify { behaviorRepository.setSequence(2L, 2) }
    }

    // --- Edit behavior ---

    @Test
    fun `edit mode calls updateBehavior and updateTagsForBehavior`() = runTest {
        coEvery { behaviorRepository.getBehaviorsOverlappingRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 5000, fixedNow, false)

        val result = invokeUseCase(
            tagIds = listOf(10L, 20L),
            startTime = fixedNow - 5000, endTime = fixedNow,
            status = BehaviorNature.COMPLETED, note = "edited",
            editBehaviorId = 42L,
        )

        assertTrue(result is AddBehaviorUseCase.Result.Success)
        assertEquals(42L, (result as AddBehaviorUseCase.Result.Success).behaviorId)
        coVerify { behaviorRepository.updateBehavior(42L, 1L, fixedNow - 5000, fixedNow, "completed", "edited") }
        coVerify { behaviorRepository.updateTagsForBehavior(42L, listOf(10L, 20L)) }
        // 编辑时必须忽略自身 id，避免与自己冲突
        io.mockk.verify {
            timeSnapService.snapAndCheckConflict(
                newStart = fixedNow - 5000,
                newEnd = fixedNow,
                newStatus = BehaviorNature.COMPLETED,
                overlappingBehaviors = any(),
                currentTime = fixedNow,
                ignoreBehaviorId = 42L,
            )
        }
    }

    @Test
    fun `edit mode with PENDING sets startTime to 0`() = runTest {
        invokeUseCase(startTime = 0, status = BehaviorNature.PENDING, editBehaviorId = 5L)

        coVerify { behaviorRepository.updateBehavior(5L, 1L, 0L, null, "pending", null) }
    }

    @Test
    fun `edit mode still validates time constraints`() = runTest {
        val result = invokeUseCase(
            startTime = fixedNow - 5000, endTime = fixedNow + 10000,
            status = BehaviorNature.COMPLETED, editBehaviorId = 42L,
        )

        assertTrue(result is AddBehaviorUseCase.Result.ValidationError)
    }

    @Test
    fun `edit mode with conflict returns Conflict result`() = runTest {
        coEvery { behaviorRepository.getBehaviorsOverlappingRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 5000, fixedNow, true)

        val result = invokeUseCase(
            startTime = fixedNow - 5000, endTime = fixedNow,
            status = BehaviorNature.COMPLETED,
            editBehaviorId = 42L,
        )

        assertTrue(result is AddBehaviorUseCase.Result.Conflict)
        coVerify(exactly = 0) { behaviorRepository.updateBehavior(any(), any(), any(), any(), any(), any()) }
    }

    // --- Actual duration ---

    @Test
    fun `COMPLETED calculates actualDuration from start to end`() = runTest {
        val start = fixedNow - 60000
        val end = fixedNow
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(start, end, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = start, endTime = end, status = BehaviorNature.COMPLETED)

        assertEquals(60000L, behaviorSlot.captured.actualDuration)
    }

    @Test
    fun `ACTIVE calculates actualDuration from start to now`() = runTest {
        val start = fixedNow - 30000
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(start, null, false)
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = start, status = BehaviorNature.ACTIVE)

        assertEquals(30000L, behaviorSlot.captured.actualDuration)
    }

    @Test
    fun `PENDING has null actualDuration`() = runTest {
        val behaviorSlot = slot<Behavior>()
        coEvery { behaviorRepository.getMaxSequence() } returns -1
        coEvery { behaviorRepository.insert(capture(behaviorSlot), any()) } returns 1L

        invokeUseCase(startTime = 0, status = BehaviorNature.PENDING)

        assertEquals(null, behaviorSlot.captured.actualDuration)
    }

    // --- Day query optimization ---

    @Test
    fun `day query is fetched once for both calculateSequence and updateSubsequent`() = runTest {
        val existingBehaviors = listOf(
            Behavior(id = 1, activityId = 1, startTime = fixedNow - 10000, endTime = fixedNow - 5000,
                status = BehaviorNature.COMPLETED, note = null, pomodoroCount = 0, sequence = 0,
                estimatedDuration = null, actualDuration = null, achievementLevel = null, wasPlanned = false),
        )
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(existingBehaviors)
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow, null, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 2L

        invokeUseCase(startTime = fixedNow, status = BehaviorNature.ACTIVE)

        coVerify(exactly = 1) { behaviorRepository.getByDayRange(any(), any()) }
    }

    // --- Edge cases ---

    @Test
    fun `empty tagIds list creates behavior without tags`() = runTest {
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow, null, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), emptyList()) } returns 1L

        invokeUseCase(startTime = fixedNow, status = BehaviorNature.ACTIVE)

        coVerify { behaviorRepository.insert(any<Behavior>(), emptyList()) }
    }

    @Test
    fun `COMPLETED with endTime equal to now is valid`() = runTest {
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow - 60000, fixedNow, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 1L

        val result = invokeUseCase(
            startTime = fixedNow - 60000, endTime = fixedNow,
            status = BehaviorNature.COMPLETED,
        )

        assertTrue(result is AddBehaviorUseCase.Result.Success)
    }

    @Test
    fun `ACTIVE with startTime equal to now is valid`() = runTest {
        coEvery { behaviorRepository.getByDayRange(any(), any()) } returns flowOf(emptyList())
        every { timeSnapService.snapAndCheckConflict(any(), any(), any(), any(), any(), any()) } returns
            SnapResult(fixedNow, null, false)
        coEvery { behaviorRepository.insert(any<Behavior>(), any()) } returns 1L

        val result = invokeUseCase(startTime = fixedNow, status = BehaviorNature.ACTIVE)

        assertTrue(result is AddBehaviorUseCase.Result.Success)
    }
}
