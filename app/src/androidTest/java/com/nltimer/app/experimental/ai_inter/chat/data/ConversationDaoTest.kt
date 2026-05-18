package com.nltimer.app.experimental.ai_inter.chat.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nltimer.app.experimental.ai_inter.data.AiInterDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConversationDaoTest {

    private lateinit var db: AiInterDatabase
    private lateinit var convDao: ConversationDao
    private lateinit var msgDao: ConversationMessageDao

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, AiInterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        convDao = db.conversationDao()
        msgDao = db.conversationMessageDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun upsertAndObserveAll_returnsConversationOrderedByUpdatedAtDesc() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        convDao.upsert(ConversationEntity("b", "B", 2L, 10L))
        convDao.upsert(ConversationEntity("c", "C", 3L, 5L))

        val list = convDao.observeAll().first()
        assertEquals(listOf("b", "c", "a"), list.map { it.id })
    }

    @Test
    fun rename_updatesTitleAndTimestamp() = runBlocking {
        convDao.upsert(ConversationEntity("a", "old", 1L, 1L))
        convDao.rename("a", "new", 99L)

        val got = convDao.get("a")!!
        assertEquals("new", got.title)
        assertEquals(99L, got.updatedAt)
    }

    @Test
    fun delete_cascadesToMessages() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        msgDao.insert(ConversationMessageEntity("m1", "a", 0, "user", "hi", "", "", 1L))
        msgDao.insert(ConversationMessageEntity("m2", "a", 1, "assistant", "ok", "", "", 2L))

        convDao.delete("a")

        val msgs = msgDao.observeByConversation("a").first()
        assertEquals(emptyList<ConversationMessageEntity>(), msgs)
        assertNull(convDao.get("a"))
    }

    @Test
    fun nextOrder_returnsZeroForEmpty_thenIncrements() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        assertEquals(0, msgDao.nextOrder("a"))

        msgDao.insert(ConversationMessageEntity("m1", "a", 0, "user", "hi", "", "", 1L))
        assertEquals(1, msgDao.nextOrder("a"))

        msgDao.insert(ConversationMessageEntity("m2", "a", 5, "assistant", "ok", "", "", 2L))
        assertEquals(6, msgDao.nextOrder("a"))
    }

    @Test
    fun deleteAllInConversation_clearsMessagesKeepsConversation() = runBlocking {
        convDao.upsert(ConversationEntity("a", "A", 1L, 1L))
        msgDao.insert(ConversationMessageEntity("m1", "a", 0, "user", "hi", "", "", 1L))
        msgDao.insert(ConversationMessageEntity("m2", "a", 1, "assistant", "ok", "", "", 2L))

        msgDao.deleteAllInConversation("a")

        assertEquals(emptyList<ConversationMessageEntity>(), msgDao.observeByConversation("a").first())
        assertEquals("A", convDao.get("a")?.title)
    }
}
