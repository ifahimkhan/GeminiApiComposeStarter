package com.fahim.geminiApiComposeStarter

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class PersistenceTest {
    @Test fun messagesSurviveDatabaseReopenAndRetryIsIdempotent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
        var database = open()
        try {
            var store = ConversationRepository(database)
            store.create(Conversation("one", "First", Category.RESEARCH)); store.create(Conversation("two", "Second", Category.GENERAL))
            val user = ChatMessage("u", "one", "turn", 0, Role.USER, "persistent question", Delivery.PENDING)
            store.appendUser(user); store.complete(user, "first answer"); store.complete(user, "replacement answer")
            database.close(); database = open(); store = ConversationRepository(database)
            val messages = store.messages("one").first()
            assertEquals(2, messages.size); assertEquals("replacement answer", messages.last().text)
            assertTrue(store.messages("two").first().isEmpty())
            assertEquals("one", store.conversations("persistent").first().single().id)
            store.appendUser(ChatMessage("interrupted", "one", "turn2", 2, Role.USER, "unfinished", Delivery.PENDING))
            store.recoverInterrupted(); assertEquals(Delivery.INTERRUPTED, store.messages("one").first().last().delivery)
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test fun preferencesSurviveRepositoryRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PreferencesRepository(context)
        val before = store.preferences.first()
        try {
            store.save(UserPreferences("Local test", Category.RESEARCH, true, GeminiModels.options.last().id))
            val after = PreferencesRepository(context).preferences.first()
            assertEquals("Local test", after.displayName); assertTrue(after.reducedMotion)
            assertEquals(GeminiModels.options.last().id, after.modelId)
        } finally { store.save(before) }
    }
}
