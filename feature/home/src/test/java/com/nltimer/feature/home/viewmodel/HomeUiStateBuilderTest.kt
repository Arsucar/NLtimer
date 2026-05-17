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

    @Test
    fun `today grid rows are reverse-ordered with ascending cells within each row`() {
        val behaviors = listOf(
            behavior(1L, today, 8),
            behavior(2L, today, 10),
            behavior(3L, today, 12),
            behavior(4L, today, 14),
            behavior(5L, today, 16),
        )

        val state = builder.buildUiState(
            behaviors = behaviors,
            activities = emptyList(),
            tagsByBehaviorId = emptyMap(),
            now = LocalTime.of(17, 0),
            currentTimeMs = epochMs(today, 17),
            today = today,
        )

        val todaySection = state.gridSections.first { it.date == today }
        // 5 个 behavior + 1 个 addCell = 6 cells，gridColumns=4 → 切成 [[1..4],[5,add]]，块倒序后 2 行
        assertEquals(2, todaySection.rows.size)

        // 顶部行（rows[0]）= 最新一块：先是 behavior 5（hour=16），随后是 addCell 占位，再 padding 至 4 列
        val topRow = todaySection.rows[0]
        val topBehaviorIds = topRow.cells.mapNotNull { it.behaviorId }
        assertEquals(listOf(5L), topBehaviorIds)
        assertEquals(LocalTime.of(16, 0), topRow.startTime)

        // 底部行（rows[1]）= 最旧一块：行内按时间正序排列 1→2→3→4（hour=8→10→12→14）
        val bottomRow = todaySection.rows[1]
        val bottomBehaviorIds = bottomRow.cells.mapNotNull { it.behaviorId }
        assertEquals(listOf(1L, 2L, 3L, 4L), bottomBehaviorIds)
        assertEquals(LocalTime.of(8, 0), bottomRow.startTime)
    }

    @Test
    fun `non-today grid keeps reverse rows and ascending intra-row cells`() {
        val yesterday = today.minusDays(1)
        val behaviors = listOf(
            behavior(1L, yesterday, 7),
            behavior(2L, yesterday, 9),
            behavior(3L, yesterday, 11),
            behavior(4L, yesterday, 13),
            behavior(5L, yesterday, 15),
            // 今天补一条以便 today section 仍然存在
            behavior(6L, today, 9),
        )

        val state = builder.buildUiState(
            behaviors = behaviors,
            activities = emptyList(),
            tagsByBehaviorId = emptyMap(),
            now = LocalTime.of(10, 0),
            currentTimeMs = epochMs(today, 10),
            today = today,
        )

        val yesterdaySection = state.gridSections.first { it.date == yesterday }
        // 历史日期不追加 addCell，5 个 behavior chunked(4) = [[1..4],[5]]，倒序后 2 行
        assertEquals(2, yesterdaySection.rows.size)

        val topRow = yesterdaySection.rows[0]
        assertEquals(listOf(5L), topRow.cells.mapNotNull { it.behaviorId })
        assertEquals(LocalTime.of(15, 0), topRow.startTime)

        val bottomRow = yesterdaySection.rows[1]
        assertEquals(listOf(1L, 2L, 3L, 4L), bottomRow.cells.mapNotNull { it.behaviorId })
        assertEquals(LocalTime.of(7, 0), bottomRow.startTime)
    }
}
