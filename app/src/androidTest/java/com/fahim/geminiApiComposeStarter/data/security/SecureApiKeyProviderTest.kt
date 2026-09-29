package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.preferences.appDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureApiKeyProviderTest {

    private lateinit var context: Context
    private lateinit var cryptoManager: CryptoManager
    private lateinit var provider: SecureApiKeyProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cryptoManager = CryptoManager()
        cryptoManager.generateSecretKey()
        provider = SecureApiKeyProvider(
            context = context,
            cryptoManager = cryptoManager,
            dataStore = context.appDataStore,
        )
    }

    @After
    fun tearDown() = runBlocking {
        cryptoManager.removeKey()
        context.appDataStore.edit { it.clear() }
    }

    @Test
    fun getApiKey_whenEncryptedInPreferences_decryptsSuccessfully() = runBlocking {
        val dummyKey = "test_gemini_api_key_value_98765"
        val payload = cryptoManager.encrypt(dummyKey)

        context.appDataStore.edit { prefs ->
            prefs[KEY_CIPHERTEXT] = payload.ciphertextBase64
            prefs[KEY_IV] = payload.ivBase64
        }

        val decryptedKey = provider.getApiKey()
        assertEquals(dummyKey, decryptedKey)
    }

    @Test
    fun plaintext_neverExposedInPreferences() = runBlocking {
        val dummyKey = "secret_plaintext_key"
        val payload = cryptoManager.encrypt(dummyKey)

        context.appDataStore.edit { prefs ->
            prefs[KEY_CIPHERTEXT] = payload.ciphertextBase64
            prefs[KEY_IV] = payload.ivBase64
        }

        val storedPrefs = context.appDataStore.data.first()
        val storedCiphertext = storedPrefs[KEY_CIPHERTEXT]
        val storedIv = storedPrefs[KEY_IV]

        // Verify stored value is not plaintext
        assertNotNull(storedCiphertext)
        assertNotNull(storedIv)
        assertNotEquals(dummyKey, storedCiphertext)
        assertNotEquals(dummyKey, storedIv)
    }
}
