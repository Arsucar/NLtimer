package com.nltimer.feature.home.viewmodel

import com.nltimer.core.data.model.Behavior
import com.nltimer.core.data.model.BehaviorNature
import com.nltimer.feature.home.model.HomeListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class HomeUiStateBuilderTest {

    private val builder = HomeUiStateBuilder()
    private val zone = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.of(2026, 5, 13)

    private fun epochMs(date: LocalDate, hour: Int) =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun behavior(
        id: Long,
        date: LocalDate,
        hour: Int,
        status: BehaviorNature = BehaviorNature.COMPLETED,
    ) = Behavior(
        id = id,
        activityId = 1L,
        startTime = epochMs(date, hour),
        endTime = epochMs(date, hour + 1),
        status = status,
        sequence = 0,
        wasPlanned = false,
        achievementLevel = null,
        estimatedDuration = null,
        actualDuration = 3_600_000L,
        note = null,
        pomodoroCount = 0,
    )

    @Test
    fun `items contain DayDivider at every cross-day boundary`() {
        val yesterday = today.minusDays(1)
        val behaviors = listOf(
            behavior(1L, yesterday, 10),
            behavior(2L, yesterday, 14),
            behavior(3L, today, 9),
        )

        val state = builder.buildUiState(
            behaviors = behaviors,
            activities = emptyList(),
            tagsByBehaviorId = emptyMap(),
            now = LocalTime.of(10, 0),
            currentTimeMs = epochMs(today, 10),
            today = today,
        )

        val dividerDates = state.items.filterIsInstance<HomeListItem.DayDivider>().map { it.date }
        assertEquals(listOf(yesterday, today), dividerDates)
    }

    @Test
    fun `gridSections one per day with rows for that day`() {
        val yesterday = today.minusDays(1)
        val behaviors = listOf(
            behavior(1L, yesterday, 10),
            behavior(2L, today, 9),
        )

        val state = builder.buildUiState(
            behaviors = behaviors,
            activities = emptyList(),
            tagsByBehaviorId = emptyMap(),
            now = LocalTime.of(10, 0),
            currentTimeMs = epochMs(today, 10),
            today = today,
        )

        assertEquals(listOf(today, yesterday), state.gridSections.map { it.date })
        assertTrue(state.gridSections.all { it.rows.isNotEmpty() })
    }

    @Test
    fun `empty day is skipped from items and gridSections`() {
        val twoDaysAgo = today.minusDays(2)
        val behaviors = listOf(
            behavior(1L, twoDaysAgo, 10),
            behavior(2L, today, 9),
        )

        val state = builder.buildUiState(
            behaviors = behaviors,
            activities = emptyList(),
            tagsByBehaviorId = emptyMap(),
            now = LocalTime.of(10, 0),
            currentTimeMs = epochMs(today, 10),
            today = today,
        )

        val dividerDates = state.items.filterIsInstance<HomeListItem.DayDivider>().map { it.date }
        assertEquals(listOf(twoDaysAgo, today), dividerDates)
        assertEquals(listOf(today, twoDaysAgo), state.gridSections.map { it.date })
    }

    @Test
    fun `momentCells lists today behaviors first then non-today`() {
        val yesterday = today.minusDays(1)
        val behaviors = listOf(
            behavior(1L, yesterday, 10),
            behavior(2L, today, 9),
        )

        val state = builder.buildUiState(
            behaviors = behaviors,
            activities = emptyList(),
            tagsByBehaviorId = emptyMap(),
            now = LocalTime.of(10, 0),
            currentTimeMs = epochMs(today, 10),
            today = today,
        )

        val ids = state.momentCells.mapNotNull { it.behaviorId }
        assertEquals(listOf(2L, 1L), ids)
    }
}
