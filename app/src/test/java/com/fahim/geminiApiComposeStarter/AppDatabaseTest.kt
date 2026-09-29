package com.fahim.geminiApiComposeStarter

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ChatMessageDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.chatMessageDao()
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun databaseCanBeCreatedAndDaoObtained() {
        assertNotNull(database)
        assertNotNull(dao)
        database.openHelper.writableDatabase
        assertTrue(database.isOpen)
    }

    @Test
    fun emptyDatabaseBehavesCorrectly() = runTest {
        val messages = dao.getAllMessages().first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun chatMessageInsertionAndRetrievalWorks() = runTest {
        val entity = ChatMessageEntity(
            id = "msg-1",
            text = "Hello Room!",
            role = "USER",
            timestamp = 1000L,
        )

        dao.insertMessage(entity)

        val stored = dao.getAllMessages().first()
        assertEquals(1, stored.size)
        assertEquals("msg-1", stored[0].id)
        assertEquals("Hello Room!", stored[0].text)
        assertEquals("USER", stored[0].role)
        assertEquals(1000L, stored[0].timestamp)

        // Verify conversion to domain ChatMessage
        val domain = stored[0].toChatMessage()
        assertEquals(ChatRole.USER, domain.role)
        assertEquals("Hello Room!", domain.text)
    }

    @Test
    fun multipleMessagesPreserveExpectedChronologicalOrdering() = runTest {
        // Insert out of chronological order
        val msg3 = ChatMessageEntity(id = "3", text = "Third", role = "USER", timestamp = 3000L)
        val msg1 = ChatMessageEntity(id = "1", text = "First", role = "USER", timestamp = 1000L)
        val msg2 = ChatMessageEntity(id = "2", text = "Second", role = "ASSISTANT", timestamp = 2000L)

        dao.insertMessages(listOf(msg3, msg1, msg2))

        val retrieved = dao.getAllMessages().first()
        assertEquals(3, retrieved.size)
        assertEquals("1", retrieved[0].id)
        assertEquals("First", retrieved[0].text)
        assertEquals("2", retrieved[1].id)
        assertEquals("Second", retrieved[1].text)
        assertEquals("3", retrieved[2].id)
        assertEquals("Third", retrieved[2].text)
    }

    @Test
    fun insertReplacesExistingMessageOnConflict() = runTest {
        val initial = ChatMessageEntity(id = "same-id", text = "Initial text", role = "USER", timestamp = 1000L)
        val updated = ChatMessageEntity(id = "same-id", text = "Updated text", role = "USER", timestamp = 1000L)

        dao.insertMessage(initial)
        dao.insertMessage(updated)

        val stored = dao.getAllMessages().first()
        assertEquals(1, stored.size)
        assertEquals("Updated text", stored[0].text)
    }

    @Test
    fun deleteFunctionalityClearsAllMessages() = runTest {
        val msg1 = ChatMessageEntity(id = "1", text = "Msg 1", role = "USER", timestamp = 1000L)
        val msg2 = ChatMessageEntity(id = "2", text = "Msg 2", role = "ASSISTANT", timestamp = 2000L)
        dao.insertMessages(listOf(msg1, msg2))

        assertEquals(2, dao.getAllMessages().first().size)

        val deletedCount = dao.clearAllMessages()
        assertEquals(2, deletedCount)

        val remaining = dao.getAllMessages().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun chatHistoryRepositoryWorksEndToEndWithRoom() = runTest {
        val repository = ChatHistoryRepositoryImpl(dao)

        val message = ChatMessage(id = "repo-msg-1", text = "Repo test", role = ChatRole.USER, timestamp = 1500L)
        repository.saveMessage(message)

        val retrieved = repository.getMessages().first()
        assertEquals(1, retrieved.size)
        assertEquals("repo-msg-1", retrieved[0].id)
        assertEquals("Repo test", retrieved[0].text)
        assertEquals(ChatRole.USER, retrieved[0].role)

        repository.clearHistory()
        assertTrue(repository.getMessages().first().isEmpty())
    }

    @Test
    fun databaseIsProperlyClosedAfterTests() {
        database.close()
        assertFalse(database.isOpen)
    }
}
