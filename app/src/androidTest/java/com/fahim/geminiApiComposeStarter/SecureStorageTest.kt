package com.fahim.geminiApiComposeStarter

import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferences
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureStorageTest {
    @Test fun keyIsEncryptedAtRestAndCanBeReadAfterStoreRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SecureApiKeyStore(context)
        val fixture = "instrumentation-placeholder-not-a-real-key"
        try {
            store.initialize(fixture)
            val persisted = File(context.filesDir, "datastore/encrypted_credentials.preferences_pb").readBytes()
            assertFalse("Plaintext must not be persisted", persisted.toString(Charsets.UTF_8).contains(fixture))
            assertTrue("Encrypted key must round trip", SecureApiKeyStore(context).decryptForModel() == fixture)
            store.initialize("rotated-instrumentation-placeholder")
            assertTrue("Rotated key must replace prior key", store.decryptForModel() == "rotated-instrumentation-placeholder")
        } finally {
            store.initialize(BuildConfig.GEMINI_API_KEY)
        }
    }

    @Test fun themeSurvivesPreferenceRepositoryRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = DataStoreUserPreferences(context)
        val original = preferences.theme.first()
        try {
            preferences.setTheme(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, DataStoreUserPreferences(context).theme.first())
        } finally {
            preferences.setTheme(original)
        }
    }
}
