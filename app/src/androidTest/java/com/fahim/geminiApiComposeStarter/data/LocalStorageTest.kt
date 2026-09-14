package com.fahim.geminiApiComposeStarter.data

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class LocalStorageTest {
    @Test fun historySurvivesReopeningFilesAndDeletionPersists() = runTest {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(base.cacheDir, "storage-test-" + UUID.randomUUID()).apply { mkdirs() }
        val context = object : ContextWrapper(base) { override fun getNoBackupFilesDir() = folder }
        try {
            val chat = Conversation("one", "Saved chat", listOf(ChatMessage(9, ChatRole.USER, "Hello")), "draft")
            FileChatStorage(context).save(listOf(chat))
            assertEquals(listOf(chat), FileChatStorage(context).load())
            FileChatStorage(context).save(emptyList())
            assertTrue(FileChatStorage(context).load().isEmpty())
        } finally { folder.deleteRecursively() }
    }

    @Test fun keyIsEncryptedAtRestAndCanBeRemoved() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(base.cacheDir, "vault-test-" + UUID.randomUUID()).apply { mkdirs() }
        val context = object : ContextWrapper(base) { override fun getNoBackupFilesDir() = folder }
        try {
            val vault = ApiKeyVault(context)
            vault.save("test-key-not-a-real-secret")
            assertEquals("test-key-not-a-real-secret", ApiKeyVault(context).read())
            assertFalse(File(folder, "gemini-key.enc").readBytes().toString(Charsets.UTF_8).contains("test-key-not-a-real-secret"))
            vault.save("")
            assertEquals("", vault.read())
        } finally { folder.deleteRecursively() }
    }
}
