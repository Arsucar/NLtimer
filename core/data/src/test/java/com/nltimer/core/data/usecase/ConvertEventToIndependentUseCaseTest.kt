package com.nltimer.core.data.usecase

import com.nltimer.core.data.model.BehaviorEvent
import com.nltimer.core.data.repository.BehaviorEventRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConvertEventToIndependentUseCaseTest {

    private lateinit var behaviorEventRepository: BehaviorEventRepository
    private lateinit var useCase: ConvertEventToIndependentUseCase

    private val relatedEvent = BehaviorEvent(
        id = 5L,
        behaviorId = 3L,
        activityId = 7L,
        templateId = 1L,
        timestamp = 1000L,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private val independentEvent = relatedEvent.copy(behaviorId = null)

    @Before
    fun setup() {
        behaviorEventRepository = mockk(relaxed = true)
        useCase = ConvertEventToIndependentUseCase(behaviorEventRepository)
    }

    @Test
    fun `missing event returns NotFound`() = runTest {
        coEvery { behaviorEventRepository.getEventById(5L) } returns null

        val result = useCase(5L)

        assertTrue(result is ConvertEventToIndependentUseCase.Result.NotFound)
    }

    @Test
    fun `already independent returns AlreadyIndependent`() = runTest {
        coEvery { behaviorEventRepository.getEventById(5L) } returns independentEvent

        val result = useCase(5L)

        assertTrue(result is ConvertEventToIndependentUseCase.Result.AlreadyIndependent)
    }

    @Test
    fun `related event converts by clearing behaviorId`() = runTest {
        coEvery { behaviorEventRepository.getEventById(5L) } returns relatedEvent

        val result = useCase(5L)

        assertTrue(result is ConvertEventToIndependentUseCase.Result.Success)
        assertEquals(5L, (result as ConvertEventToIndependentUseCase.Result.Success).eventId)
        coVerify { behaviorEventRepository.setEventBehaviorId(5L, null) }
    }
}
