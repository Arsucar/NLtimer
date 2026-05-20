package com.nltimer.feature.home.match

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeywordMatchStrategyTest {

    private lateinit var strategy: KeywordMatchStrategy

    @Before
    fun setup() {
        strategy = KeywordMatchStrategy()
    }

    @Test
    fun `matchActivities with blank query returns all activities`() {
        val activities = listOf(
            createActivity("工作"),
            createActivity("学习"),
        )
        val result = strategy.matchActivities("", activities)
        assertEquals(2, result.size)
    }

    @Test
    fun `matchActivities with blank query returns all activities when query has spaces`() {
        val activities = listOf(
            createActivity("工作"),
            createActivity("学习"),
        )
        val result = strategy.matchActivities("   ", activities)
        assertEquals(2, result.size)
    }

    @Test
    fun `matchActivities filters by exact match`() {
        val activities = listOf(
            createActivity("工作"),
            createActivity("学习"),
        )
        val result = strategy.matchActivities("工作", activities)
        assertEquals(1, result.size)
        assertEquals("工作", result[0].name)
    }

    @Test
    fun `matchActivities filters by partial match`() {
        val activities = listOf(
            createActivity("工作"),
            createActivity("工作汇报"),
            createActivity("学习"),
        )
        val result = strategy.matchActivities("工作", activities)
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("工作") })
    }

    @Test
    fun `matchActivities is case insensitive`() {
        val activities = listOf(
            createActivity("Work"),
            createActivity("work"),
            createActivity("WORK"),
        )
        val result = strategy.matchActivities("work", activities)
        assertEquals(3, result.size)
    }

    @Test
    fun `matchActivities returns empty list when no match`() {
        val activities = listOf(
            createActivity("工作"),
            createActivity("学习"),
        )
        val result = strategy.matchActivities("运动", activities)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `matchActivities handles empty list`() {
        val result = strategy.matchActivities("工作", emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `matchTags with blank query returns all tags`() {
        val tags = listOf(
            createTag("工作"),
            createTag("学习"),
        )
        val result = strategy.matchTags("", tags)
        assertEquals(2, result.size)
    }

    @Test
    fun `matchTags with blank query returns all tags when query has spaces`() {
        val tags = listOf(
            createTag("工作"),
            createTag("学习"),
        )
        val result = strategy.matchTags("   ", tags)
        assertEquals(2, result.size)
    }

    @Test
    fun `matchTags filters by exact match`() {
        val tags = listOf(
            createTag("工作"),
            createTag("学习"),
        )
        val result = strategy.matchTags("工作", tags)
        assertEquals(1, result.size)
        assertEquals("工作", result[0].name)
    }

    @Test
    fun `matchTags filters by partial match`() {
        val tags = listOf(
            createTag("工作"),
            createTag("工作汇报"),
            createTag("学习"),
        )
        val result = strategy.matchTags("工作", tags)
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("工作") })
    }

    @Test
    fun `matchTags is case insensitive`() {
        val tags = listOf(
            createTag("Work"),
            createTag("work"),
            createTag("WORK"),
        )
        val result = strategy.matchTags("work", tags)
        assertEquals(3, result.size)
    }

    @Test
    fun `matchTags returns empty list when no match`() {
        val tags = listOf(
            createTag("工作"),
            createTag("学习"),
        )
        val result = strategy.matchTags("运动", tags)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `matchTags handles empty list`() {
        val result = strategy.matchTags("工作", emptyList())
        assertTrue(result.isEmpty())
    }

    private fun createActivity(name: String): Activity =
        Activity(
            id = 0,
            name = name,
        )

    private fun createTag(name: String): Tag =
        Tag(
            id = 0,
            name = name,
            color = null,
            iconKey = null,
            category = null,
            groupId = null,
            priority = 0,
            usageCount = 0,
            sortOrder = 0,
            keywords = null,
            isArchived = false,
        )
}
