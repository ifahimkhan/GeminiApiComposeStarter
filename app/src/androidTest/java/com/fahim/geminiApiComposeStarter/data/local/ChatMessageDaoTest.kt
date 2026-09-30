package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ChatMessageDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ChatMessageDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.chatMessageDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertMessage_and_queryHistory_returnsOrderedMessages() = runTest {
        val msgLater = ChatMessageEntity(
            id = "msg-2",
            text = "Second message",
            participant = "ASSISTANT",
            timestamp = 2000L,
        )
        val msgEarlier = ChatMessageEntity(
            id = "msg-1",
            text = "First message",
            participant = "USER",
            timestamp = 1000L,
        )

        // Insert in reverse chronological order
        dao.insertMessage(msgLater)
        dao.insertMessage(msgEarlier)

        val messages = dao.getAllMessages().first()
        assertEquals(2, messages.size)
        assertEquals("msg-1", messages[0].id)
        assertEquals("First message", messages[0].text)
        assertEquals("msg-2", messages[1].id)
        assertEquals("Second message", messages[1].text)
        assertTrue(messages[0].timestamp < messages[1].timestamp)
    }

    @Test
    fun insertDuplicateId_replacesExistingMessage() = runTest {
        val initialMsg = ChatMessageEntity(
            id = "stable-id-1",
            text = "Draft response",
            participant = "ASSISTANT",
            timestamp = 1000L,
        )
        val updatedMsg = ChatMessageEntity(
            id = "stable-id-1",
            text = "Final completed response",
            participant = "ASSISTANT",
            timestamp = 1000L,
        )

        dao.insertMessage(initialMsg)
        assertEquals(1, dao.getAllMessages().first().size)
        assertEquals("Draft response", dao.getAllMessages().first()[0].text)

        dao.insertMessage(updatedMsg)
        val messages = dao.getAllMessages().first()
        assertEquals(1, messages.size)
        assertEquals("Final completed response", messages[0].text)
    }

    @Test
    fun clearAllMessages_deletesAllRecords() = runTest {
        dao.insertMessage(
            ChatMessageEntity(id = "1", text = "One", participant = "USER", timestamp = 1L)
        )
        dao.insertMessage(
            ChatMessageEntity(id = "2", text = "Two", participant = "ASSISTANT", timestamp = 2L)
        )
        assertEquals(2, dao.getAllMessages().first().size)

        dao.clearAllMessages()
        val messages = dao.getAllMessages().first()
        assertTrue(messages.isEmpty())
    }
}
