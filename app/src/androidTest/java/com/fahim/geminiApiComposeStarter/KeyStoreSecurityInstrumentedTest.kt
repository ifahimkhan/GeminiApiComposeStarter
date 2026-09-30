package com.fahim.geminiApiComposeStarter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.data.security.KeyStoreManager
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyStoreSecurityInstrumentedTest {

    private lateinit var keyStoreManager: KeyStoreManager
    private lateinit var secureStorage: SecureApiKeyStorage
    private val testAlias = "test_instrumented_key_alias"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        keyStoreManager = KeyStoreManager()
        secureStorage = SecureApiKeyStorage(
            context = context,
            keyStoreManager = keyStoreManager,
            prefsName = "test_gemini_credentials",
        )
    }

    @After
    fun tearDown() {
        try {
            keyStoreManager.deleteKey(testAlias)
            secureStorage.clearStoredKey()
        } catch (_: Exception) {
        }
    }

    @Test
    fun keyStoreManager_generatesKeyAndEncryptsDecryptsSuccessfully() {
        val sampleSecret = "Test-Secret-API-Key-1234567890"

        val encryptedBase64 = keyStoreManager.encrypt(sampleSecret, testAlias)
        assertNotNull(encryptedBase64)
        assertNotEquals(sampleSecret, encryptedBase64)

        val decrypted = keyStoreManager.decrypt(encryptedBase64, testAlias)
        assertEquals(sampleSecret, decrypted)
    }

    @Test
    fun secureApiKeyStorage_storesAndRetrievesEncryptedKeyAtRest() {
        val sampleKey = "AIzaSySampleKeyEncryptedAtRest"

        secureStorage.storeEncryptedApiKey(sampleKey)
        assertTrue(secureStorage.hasStoredKey())

        val retrievedKey = secureStorage.getDecryptedApiKey()
        assertEquals(sampleKey, retrievedKey)

        secureStorage.clearStoredKey()
        assertFalse(secureStorage.hasStoredKey())
        assertNull(secureStorage.getDecryptedApiKey())
    }
}
