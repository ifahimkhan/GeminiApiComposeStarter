package com.fahim.geminiApiComposeStarter

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatDatabaseTest {
    @Test fun messagesSurviveDatabaseReopen() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val name = "chat-history-test.db"
            context.deleteDatabase(name)
            val first = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
            first.messages().insert(ChatMessage("saved", "Remember this", true))
            first.close()
            val reopened = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
            assertEquals("Remember this", reopened.messages().observeAll().first().single().text)
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
