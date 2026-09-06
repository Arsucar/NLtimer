package com.nltimer.feature.stats.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsGridDropIndexTest {

    @Test
    fun emptyItems_returnsItemCount() {
        assertEquals(0, computeGridDropIndex(0f, 0f, emptyList(), itemCount = 0))
        assertEquals(3, computeGridDropIndex(10f, 10f, emptyList(), itemCount = 3))
    }

    @Test
    fun fullWidthPanel_usesVerticalHalf() {
        val items = listOf(
            GridDropItem(index = 0, offsetX = 0, offsetY = 0, width = 400, height = 200, colSpan = 4),
            GridDropItem(index = 1, offsetX = 0, offsetY = 220, width = 400, height = 200, colSpan = 4),
        )
        assertEquals(0, computeGridDropIndex(200f, 40f, items, itemCount = 2))
        assertEquals(1, computeGridDropIndex(200f, 160f, items, itemCount = 2))
        assertEquals(1, computeGridDropIndex(200f, 250f, items, itemCount = 2))
        assertEquals(2, computeGridDropIndex(200f, 360f, items, itemCount = 2))
    }

    @Test
    fun mixedColSpan_usesHorizontalHalfOnHalfWidthCards() {
        val items = listOf(
            GridDropItem(index = 0, offsetX = 0, offsetY = 0, width = 180, height = 120, colSpan = 2),
            GridDropItem(index = 1, offsetX = 200, offsetY = 0, width = 180, height = 120, colSpan = 2),
            GridDropItem(index = 2, offsetX = 0, offsetY = 140, width = 400, height = 200, colSpan = 4),
        )
        assertEquals(0, computeGridDropIndex(40f, 40f, items, itemCount = 3))
        assertEquals(1, computeGridDropIndex(160f, 40f, items, itemCount = 3))
        assertEquals(1, computeGridDropIndex(220f, 40f, items, itemCount = 3))
        assertEquals(2, computeGridDropIndex(360f, 40f, items, itemCount = 3))
        assertEquals(3, computeGridDropIndex(200f, 280f, items, itemCount = 3))
    }

    @Test
    fun pointerBelowVisibleItems_appendsAfterLastAbove() {
        val items = listOf(
            GridDropItem(index = 0, offsetX = 0, offsetY = 0, width = 400, height = 100, colSpan = 4),
        )
        assertEquals(1, computeGridDropIndex(10f, 400f, items, itemCount = 1))
    }
}
