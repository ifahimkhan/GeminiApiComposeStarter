package com.fahim.geminiApiComposeStarter

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPersistenceTest {
    @Test fun historySurvivesDatabaseReopenAndClearPersists() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-restart-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
        try {
            database.chatDao().insert(ChatMessage(text = "Persist this message", isUser = true))
            database.close()
            database = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
            assertEquals("Persist this message", database.chatDao().observeMessages().first().single().text)
            database.chatDao().clear()
            database.close()
            database = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
            assertTrue(database.chatDao().observeMessages().first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
