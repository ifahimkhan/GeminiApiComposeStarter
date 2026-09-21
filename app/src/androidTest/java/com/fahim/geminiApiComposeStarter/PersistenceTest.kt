package com.fahim.geminiApiComposeStarter

import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.local.*
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferences
import com.fahim.geminiApiComposeStarter.security.ApiKeyVault
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun roomHistorySurvivesDatabaseReopen() = runBlocking {
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
            first.chatDao().insertMessage(ChatMessageEntity(text = "Saved message", isFromUser = true))
            first.close()
            val reopened = Room.databaseBuilder(context, ChatDatabase::class.java, name).build()
            try { assertEquals("Saved message", reopened.chatDao().getAllMessages().first().single().text) }
            finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun preferenceSurvivesRepositoryRecreation() = runBlocking {
        val first = DataStoreUserPreferences(context)
        val previous = first.compactMessages.first()
        try {
            first.setCompactMessages(!previous)
            assertEquals(!previous, DataStoreUserPreferences(context).compactMessages.first())
        } finally { first.setCompactMessages(previous) }
    }
    @Test fun encryptedKeyCanBeReadAfterVaultRecreationAndRotated() = runBlocking {
        // Synthetic test values only. Restore configured build key when the test finishes.
        try {
            ApiKeyVault(context).initialize("test-only-placeholder-one")
            assertEquals("test-only-placeholder-one", ApiKeyVault(context).decrypt())
            ApiKeyVault(context).initialize("test-only-placeholder-two")
            assertEquals("test-only-placeholder-two", ApiKeyVault(context).decrypt())
            val data = java.io.File(context.filesDir, "datastore/api_key_vault.preferences_pb").readBytes()
            assertFalse(data.toString(Charsets.ISO_8859_1).contains("test-only-placeholder"))
        } finally { ApiKeyVault(context).initialize(BuildConfig.GEMINI_API_KEY) }
    }
}
