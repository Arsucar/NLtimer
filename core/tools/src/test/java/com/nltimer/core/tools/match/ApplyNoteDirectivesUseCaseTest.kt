package com.nltimer.core.tools.match

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Tag
import com.nltimer.core.data.usecase.AddActivityUseCase
import com.nltimer.core.data.usecase.AddTagUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApplyNoteDirectivesUseCaseTest {

    private lateinit var addActivityUseCase: AddActivityUseCase
    private lateinit var addTagUseCase: AddTagUseCase
    private lateinit var useCase: ApplyNoteDirectivesUseCase

    @Before
    fun setup() {
        addActivityUseCase = mockk()
        addTagUseCase = mockk()
        useCase = ApplyNoteDirectivesUseCase(addActivityUseCase, addTagUseCase)
    }

    private fun activity(id: Long, name: String, archived: Boolean = false) =
        Activity(id = id, name = name, isArchived = archived)

    private fun tag(id: Long, name: String, archived: Boolean = false) = Tag(
        id = id, name = name, color = null, iconKey = null, category = null, groupId = null,
        priority = 0, usageCount = 0, sortOrder = 0, keywords = null, isArchived = archived,
    )

    private fun atDir(name: String) = NoteDirectiveParser.Directive('@', name, IntRange.EMPTY)
    private fun hashDir(name: String) = NoteDirectiveParser.Directive('#', name, IntRange.EMPTY)

    @Test
    fun `empty directives returns Empty outcome`() = runTest {
        val out = useCase(emptyList(), emptyList(), emptyList())
        assertNull(out.lastActivityId)
        assertTrue(out.addedTagIds.isEmpty())
        assertTrue(out.createdActivityNames.isEmpty())
        assertTrue(out.createdTagNames.isEmpty())
    }

    @Test
    fun `existing activity is matched without creating`() = runTest {
        val activities = listOf(activity(7L, "跑步"))
        val out = useCase(listOf(atDir("跑步")), activities, emptyList())
        assertEquals(7L, out.lastActivityId)
        assertTrue(out.createdActivityNames.isEmpty())
        assertEquals(listOf("跑步"), out.matchedActivityNames)
        coVerify(exactly = 0) { addActivityUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `unknown activity is created with default attrs`() = runTest {
        coEvery {
            addActivityUseCase("新跑", null, null, null, null, emptyList())
        } returns 42L
        val out = useCase(listOf(atDir("新跑")), emptyList(), emptyList())
        assertEquals(42L, out.lastActivityId)
        assertEquals(listOf("新跑"), out.createdActivityNames)
        coVerify(exactly = 1) {
            addActivityUseCase("新跑", null, null, null, null, emptyList())
        }
    }

    @Test
    fun `multiple at directives lastActivityId is last`() = runTest {
        coEvery { addActivityUseCase("a", null, null, null, null, emptyList()) } returns 1L
        coEvery { addActivityUseCase("b", null, null, null, null, emptyList()) } returns 2L
        val out = useCase(listOf(atDir("a"), atDir("b")), emptyList(), emptyList())
        assertEquals(2L, out.lastActivityId)
        assertEquals(listOf("a", "b"), out.createdActivityNames)
    }

    @Test
    fun `same name in batch is not created twice`() = runTest {
        coEvery { addActivityUseCase("a", null, null, null, null, emptyList()) } returns 1L
        val out = useCase(listOf(atDir("a"), atDir("a")), emptyList(), emptyList())
        assertEquals(1L, out.lastActivityId)
        coVerify(exactly = 1) {
            addActivityUseCase("a", null, null, null, null, emptyList())
        }
    }

    @Test
    fun `case insensitive match against existing`() = runTest {
        val activities = listOf(activity(9L, "studying"))
        val out = useCase(listOf(atDir("StuDYing")), activities, emptyList())
        assertEquals(9L, out.lastActivityId)
        coVerify(exactly = 0) { addActivityUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `archived existing skips match and creates new`() = runTest {
        coEvery { addActivityUseCase("zzz", null, null, null, null, emptyList()) } returns 99L
        val activities = listOf(activity(1L, "zzz", archived = true))
        val out = useCase(listOf(atDir("zzz")), activities, emptyList())
        assertEquals(99L, out.lastActivityId)
        assertEquals(listOf("zzz"), out.createdActivityNames)
    }

    @Test
    fun `hash directives union into addedTagIds`() = runTest {
        coEvery { addTagUseCase("t1", null, null, 0, null, null, null) } returns 11L
        coEvery { addTagUseCase("t2", null, null, 0, null, null, null) } returns 12L
        val out = useCase(listOf(hashDir("t1"), hashDir("t2")), emptyList(), emptyList())
        assertEquals(setOf(11L, 12L), out.addedTagIds)
        assertNull(out.lastActivityId)
    }

    @Test
    fun `existing tag is matched without creating`() = runTest {
        val tags = listOf(tag(5L, "重要"))
        val out = useCase(listOf(hashDir("重要")), emptyList(), tags)
        assertEquals(setOf(5L), out.addedTagIds)
        assertEquals(listOf("重要"), out.matchedTagNames)
        coVerify(exactly = 0) { addTagUseCase(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `mixed at and hash`() = runTest {
        coEvery { addActivityUseCase("跑", null, null, null, null, emptyList()) } returns 7L
        coEvery { addTagUseCase("健康", null, null, 0, null, null, null) } returns 8L
        val out = useCase(listOf(atDir("跑"), hashDir("健康")), emptyList(), emptyList())
        assertEquals(7L, out.lastActivityId)
        assertEquals(setOf(8L), out.addedTagIds)
    }

    @Test
    fun `tag creation failure does not block subsequent tags`() = runTest {
        coEvery { addTagUseCase("t1", null, null, 0, null, null, null) } throws RuntimeException("DB error")
        coEvery { addTagUseCase("t2", null, null, 0, null, null, null) } returns 22L
        val out = useCase(listOf(hashDir("t1"), hashDir("t2")), emptyList(), emptyList())
        assertEquals(setOf(22L), out.addedTagIds)
    }
}
