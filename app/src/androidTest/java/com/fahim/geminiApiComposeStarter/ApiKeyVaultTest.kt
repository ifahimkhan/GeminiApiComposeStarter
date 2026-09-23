package com.fahim.geminiApiComposeStarter

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.fahim.geminiApiComposeStarter.data.AiProvider
import com.fahim.geminiApiComposeStarter.data.ApiKeyVault
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiKeyVaultTest {
    @Test fun keyIsEncryptedInDataStoreAndCanBeReadAfterReopeningVault() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stored = File(context.cacheDir, "vault-test.preferences_pb")
        val store = PreferenceDataStoreFactory.create(produceFile = { stored })
        val vault = ApiKeyVault(context, store)
        vault.storeOnFirstLaunch(AiProvider.GEMINI, "test-placeholder-key")
        assertTrue(AiProvider.GEMINI in vault.availableProviders.first())
        assertEquals("test-placeholder-key", ApiKeyVault(context, store).readForModel(AiProvider.GEMINI))
        vault.storeOnFirstLaunch(AiProvider.OPENAI, "test-openai-placeholder")
        assertEquals("test-openai-placeholder", vault.readForModel(AiProvider.OPENAI))
        assertTrue(stored.exists())
        assertFalse(stored.readText(Charsets.ISO_8859_1).contains("test-placeholder-key"))
        assertFalse(stored.readText(Charsets.ISO_8859_1).contains("test-openai-placeholder"))
    }
}
