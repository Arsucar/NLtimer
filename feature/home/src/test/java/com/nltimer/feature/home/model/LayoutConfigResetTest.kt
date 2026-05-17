package com.nltimer.feature.home.model

import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.theme.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutConfigResetTest {

    private val customized = HomeLayoutConfig(
        grid = GridLayoutStyle(columns = 8, minRowHeight = 200),
        log = LogLayoutStyle(cardPadding = 24),
        timeline = TimelineLayoutStyle(itemSpacing = 24),
        moment = MomentLayoutStyle(cardPadding = 32),
    )

    @Test
    fun `reset GRID only resets grid block`() {
        val result = customized.resetLayout(HomeLayout.GRID)
        assertEquals(GridLayoutStyle(), result.grid)
        assertEquals(customized.log, result.log)
        assertEquals(customized.timeline, result.timeline)
        assertEquals(customized.moment, result.moment)
    }

    @Test
    fun `reset LOG only resets log block`() {
        val result = customized.resetLayout(HomeLayout.LOG)
        assertEquals(customized.grid, result.grid)
        assertEquals(LogLayoutStyle(), result.log)
        assertEquals(customized.timeline, result.timeline)
        assertEquals(customized.moment, result.moment)
    }

    @Test
    fun `reset TIMELINE_REVERSE only resets timeline block`() {
        val result = customized.resetLayout(HomeLayout.TIMELINE_REVERSE)
        assertEquals(customized.grid, result.grid)
        assertEquals(customized.log, result.log)
        assertEquals(TimelineLayoutStyle(), result.timeline)
        assertEquals(customized.moment, result.moment)
    }

    @Test
    fun `reset MOMENT only resets moment block`() {
        val result = customized.resetLayout(HomeLayout.MOMENT)
        assertEquals(customized.grid, result.grid)
        assertEquals(customized.log, result.log)
        assertEquals(customized.timeline, result.timeline)
        assertEquals(MomentLayoutStyle(), result.moment)
    }
}
