package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var chatDao: ChatDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        chatDao = database.chatDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetMessages_orderedChronologically() = runBlocking {
        val msg1 = ChatMessageEntity(id = "1", text = "First", participant = "USER", timestamp = 100L)
        val msg2 = ChatMessageEntity(id = "2", text = "Second", participant = "ASSISTANT", timestamp = 200L)
        val msg3 = ChatMessageEntity(id = "3", text = "Third", participant = "USER", timestamp = 300L)

        // Insert out of order
        chatDao.insertMessage(msg2)
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg3)

        val retrieved = chatDao.getAllMessagesSnapshot()
        assertEquals(3, retrieved.size)
        assertEquals("First", retrieved[0].text)
        assertEquals("Second", retrieved[1].text)
        assertEquals("Third", retrieved[2].text)
    }

    @Test
    fun clearAllMessages_removesAllEntries() = runBlocking {
        chatDao.insertMessage(ChatMessageEntity(id = "1", text = "First", participant = "USER", timestamp = 100L))
        chatDao.clearAllMessages()

        val retrieved = chatDao.getAllMessagesSnapshot()
        assertTrue(retrieved.isEmpty())
    }
}
