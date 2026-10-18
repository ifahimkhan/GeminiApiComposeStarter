package com.fahim.geminiApiComposeStarter.security

import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStorage
import com.fahim.geminiApiComposeStarter.data.security.CryptoEngine
import com.fahim.geminiApiComposeStarter.data.security.EncryptedData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyManagerTest {

    private class FakeApiKeyStorage : ApiKeyStorage {
        private var stored: EncryptedData? = null

        override fun saveEncryptedKey(encryptedData: EncryptedData) {
            stored = encryptedData
        }

        override fun getEncryptedKey(): EncryptedData? = stored

        override fun hasEncryptedKey(): Boolean = stored != null

        override fun clear() {
            stored = null
        }
    }

    /**
     * Standard JVM AES-GCM crypto engine for unit test verification without
     * needing AndroidKeyStore runtime.
     */
    private class JvmAesGcmCryptoEngine : CryptoEngine {
        private val secretKey: SecretKey = run {
            val generator = KeyGenerator.getInstance("AES")
            generator.init(256)
            generator.generateKey()
        }

        override fun encrypt(plainText: String): EncryptedData {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return EncryptedData(ciphertext = ciphertext, iv = iv)
        }

        override fun decrypt(encryptedData: EncryptedData): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, encryptedData.iv))
            val plaintextBytes = cipher.doFinal(encryptedData.ciphertext)
            return String(plaintextBytes, Charsets.UTF_8)
        }
    }

    private lateinit var storage: FakeApiKeyStorage
    private lateinit var cryptoEngine: JvmAesGcmCryptoEngine
    private lateinit var apiKeyManager: ApiKeyManager

    @Before
    fun setup() {
        storage = FakeApiKeyStorage()
        cryptoEngine = JvmAesGcmCryptoEngine()
        apiKeyManager = ApiKeyManager(cryptoEngine, storage)
    }

    @Test
    fun `when storage is empty hasApiKey returns false and decrypted key is null`() {
        assertFalse(apiKeyManager.hasApiKey())
        assertNull(apiKeyManager.getDecryptedApiKey())
    }

    @Test
    fun `initialize with blank key does not store anything`() {
        apiKeyManager.initialize("   ")
        assertFalse(apiKeyManager.hasApiKey())
        assertNull(storage.getEncryptedKey())
    }

    @Test
    fun `initialize encrypts and stores key properly`() {
        val testKey = "test_gemini_api_key_sample"
        apiKeyManager.initialize(testKey)

        assertTrue(apiKeyManager.hasApiKey())
        val encrypted = storage.getEncryptedKey()
        assertTrue(encrypted != null)
        assertFalse(encrypted!!.ciphertext.contentEquals(testKey.toByteArray()))

        val decrypted = apiKeyManager.getDecryptedApiKey()
        assertEquals(testKey, decrypted)
    }

    @Test
    fun `clear removes key from storage`() {
        val testKey = "test_key_to_clear"
        apiKeyManager.initialize(testKey)
        assertTrue(apiKeyManager.hasApiKey())

        apiKeyManager.clear()
        assertFalse(apiKeyManager.hasApiKey())
        assertNull(apiKeyManager.getDecryptedApiKey())
    }

    @Test
    fun `GeminiRepositoryImpl lazy provider is only called when repository is used`() = runTest {
        var providerCallCount = 0
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = {
                providerCallCount++
                ""
            }
        )

        assertEquals(0, providerCallCount)

        val result = repository.generateText("hello")
        assertTrue(result.isFailure)
        assertEquals(1, providerCallCount)
    }
}
