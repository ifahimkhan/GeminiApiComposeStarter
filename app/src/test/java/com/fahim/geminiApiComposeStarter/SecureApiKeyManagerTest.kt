package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.security.EncryptedData
import com.fahim.geminiApiComposeStarter.security.InMemoryKeyProvider
import com.fahim.geminiApiComposeStarter.security.KeyValidationResult
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class SecureApiKeyManagerTest {

    private lateinit var keyManager: SecureApiKeyManager
    private val dummyValidKey = "AIzaSyTestApiKeyForVerification12345"

    @Before
    fun setUp() {
        keyManager = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
        )
    }

    @Test
    fun validateKey_withValidKey_returnsValid() {
        val result = keyManager.validateKey(dummyValidKey)
        assertTrue(result is KeyValidationResult.Valid)
        assertTrue(keyManager.isValidKey(dummyValidKey))
    }

    @Test
    fun validateKey_withNullOrBlank_returnsMissingOrBlank() {
        val nullResult = keyManager.validateKey(null)
        assertTrue(nullResult is KeyValidationResult.MissingOrBlank)
        assertFalse(keyManager.isValidKey(null))

        val emptyResult = keyManager.validateKey("")
        assertTrue(emptyResult is KeyValidationResult.MissingOrBlank)
        assertFalse(keyManager.isValidKey(""))

        val blankResult = keyManager.validateKey("   \t\n")
        assertTrue(blankResult is KeyValidationResult.MissingOrBlank)
        assertFalse(keyManager.isValidKey("   \t\n"))
    }

    @Test
    fun validateKey_withPlaceholders_returnsPlaceholder() {
        val placeholders = listOf(
            "YOUR_API_KEY",
            "YOUR_GEMINI_API_KEY",
            "YOUR_GEMINI_API_KEY_HERE",
            "TODO",
            "API_KEY",
            "CHANGEME",
            "INSERT_KEY_HERE",
            "YOUR_KEY_123456789012345",
            "<INSERT_YOUR_KEY_HERE>",
        )

        for (placeholder in placeholders) {
            val result = keyManager.validateKey(placeholder)
            assertTrue("Expected Placeholder for '$placeholder', got $result", result is KeyValidationResult.Placeholder)
            assertFalse(keyManager.isValidKey(placeholder))
        }
    }

    @Test
    fun validateKey_withTooShortOrSpaces_returnsInvalidFormat() {
        val shortKeyResult = keyManager.validateKey("AIzaSyTooShort")
        assertTrue(shortKeyResult is KeyValidationResult.InvalidFormat)
        assertFalse(keyManager.isValidKey("AIzaSyTooShort"))

        val spacedKeyResult = keyManager.validateKey("AIzaSy Test Api Key With Spaces 12345")
        assertTrue(spacedKeyResult is KeyValidationResult.InvalidFormat)
        assertFalse(keyManager.isValidKey("AIzaSy Test Api Key With Spaces 12345"))
    }

    @Test
    fun encryptionDecryption_roundTrip_matchesOriginalPlaintext() {
        val encrypted = keyManager.encrypt(dummyValidKey)
        assertNotNull(encrypted)
        assertTrue(encrypted.ciphertext.isNotEmpty())
        assertTrue(encrypted.iv.isNotEmpty())

        val decrypted = keyManager.decrypt(encrypted)
        assertEquals(dummyValidKey, decrypted)
    }

    @Test
    fun encryption_producesUniqueIVAndCiphertextEachTime() {
        val encrypted1 = keyManager.encrypt(dummyValidKey)
        val encrypted2 = keyManager.encrypt(dummyValidKey)

        assertNotEquals(encrypted1, encrypted2)
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))

        // Both decrypt back to the identical original plaintext
        assertEquals(dummyValidKey, keyManager.decrypt(encrypted1))
        assertEquals(dummyValidKey, keyManager.decrypt(encrypted2))
    }

    @Test
    fun decryption_withCorruptedCiphertext_returnsNullSafely() {
        val encrypted = keyManager.encrypt(dummyValidKey)

        // Corrupt the ciphertext bytes (e.g. flipping a bit to violate GCM auth tag)
        val tamperedCiphertext = encrypted.ciphertext.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()

        val tampered = EncryptedData(tamperedCiphertext, encrypted.iv)
        val decrypted = keyManager.decrypt(tampered)

        // GCM authentication tag verification must reject tampered data safely without crashing
        assertNull(decrypted)
    }

    @Test
    fun decryption_withCorruptedIV_returnsNullSafely() {
        val encrypted = keyManager.encrypt(dummyValidKey)

        val tamperedIV = encrypted.iv.clone()
        tamperedIV[0] = (tamperedIV[0].toInt() xor 0x01).toByte()

        val tampered = EncryptedData(encrypted.ciphertext, tamperedIV)
        val decrypted = keyManager.decrypt(tampered)

        assertNull(decrypted)
    }

    @Test
    fun decryption_withEmptyData_returnsNullSafely() {
        assertNull(keyManager.decrypt(EncryptedData(ByteArray(0), ByteArray(12))))
        assertNull(keyManager.decrypt(EncryptedData(ByteArray(16), ByteArray(0))))
    }

    @Test
    fun storeApiKey_andGetApiKey_roundTrip() {
        val stored = keyManager.storeApiKey(dummyValidKey)
        assertTrue(stored)
        assertTrue(keyManager.hasValidApiKey())
        assertEquals(dummyValidKey, keyManager.getApiKey())
    }

    @Test
    fun storeApiKey_withInvalidKey_failsAndDoesNotStore() {
        val storedBlank = keyManager.storeApiKey("   ")
        assertFalse(storedBlank)

        val storedPlaceholder = keyManager.storeApiKey("YOUR_GEMINI_API_KEY_HERE")
        assertFalse(storedPlaceholder)

        assertNull(keyManager.getApiKey())
        assertFalse(keyManager.hasValidApiKey())
    }

    @Test
    fun clearStoredApiKey_removesKey() {
        keyManager.storeApiKey(dummyValidKey)
        assertTrue(keyManager.hasValidApiKey())

        keyManager.clearStoredApiKey()
        assertFalse(keyManager.hasValidApiKey())
        assertNull(keyManager.getApiKey())
    }

    @Test
    fun maskKey_safelyRedactsSensitivePortion() {
        val masked = keyManager.maskKey(dummyValidKey)
        assertEquals("AIzaSy...2345", masked)
        assertFalse(masked.contains(dummyValidKey.substring(6, dummyValidKey.length - 4)))

        assertEquals("[UNCONFIGURED]", keyManager.maskKey(null))
        assertEquals("[UNCONFIGURED]", keyManager.maskKey(""))
        assertEquals("[INVALID]", keyManager.maskKey("YOUR_KEY"))
    }

    @Test
    fun bootstrapPayload_whenConfigured_initializesKeySafely() {
        val mask = SecureApiKeyManager.BOOTSTRAP_MASK
        val obfuscatedBytes = dummyValidKey.toByteArray(Charsets.UTF_8).map {
            (it.toInt() xor mask.toInt()).toByte()
        }.toByteArray()
        val payload = Base64.getEncoder().encodeToString(obfuscatedBytes)

        val managerWithBootstrap = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
            bootstrapPayload = payload,
            hasBootstrapKey = true,
        )

        assertTrue(managerWithBootstrap.hasValidApiKey())
        assertEquals(dummyValidKey, managerWithBootstrap.getApiKey())
    }
}
