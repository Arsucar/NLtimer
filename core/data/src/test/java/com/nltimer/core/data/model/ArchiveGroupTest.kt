package com.nltimer.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveGroupTest {

    @Test
    fun `empty list returns empty groups`() {
        val result = groupArchivedItems(
            items = emptyList<Item>(),
            groupName = { it.group },
            archivedAt = { it.archivedAt },
            itemName = { it.name },
            uncategorizedName = "未分类",
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `uncategorized stays first then groups by newest archive`() {
        val result = groupArchivedItems(
            items = listOf(
                Item("未分类", "a", 1L),
                Item("生活", "b", 10L),
                Item("学习", "c", 20L),
            ),
            groupName = { it.group },
            archivedAt = { it.archivedAt },
            itemName = { it.name },
            uncategorizedName = "未分类",
        )
        assertEquals(listOf("未分类", "学习", "生活"), result.map { it.name })
    }

    @Test
    fun `within group later archivedAt comes first and nulls last`() {
        val result = groupArchivedItems(
            items = listOf(
                Item("学习", "旧", 100L),
                Item("学习", "无日期", null),
                Item("学习", "新", 300L),
                Item("学习", "中", 200L),
            ),
            groupName = { it.group },
            archivedAt = { it.archivedAt },
            itemName = { it.name },
            uncategorizedName = "未分类",
        )
        assertEquals(listOf("新", "中", "旧", "无日期"), result.single().items.map { it.name })
    }

    private data class Item(
        val group: String,
        val name: String,
        val archivedAt: Long?,
    )
}
