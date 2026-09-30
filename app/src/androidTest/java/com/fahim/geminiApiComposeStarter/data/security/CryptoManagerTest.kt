package com.fahim.geminiApiComposeStarter.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.InvalidKeyException
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        cryptoManager = CryptoManager()
        cryptoManager.generateSecretKey()
    }

    @After
    fun tearDown() {
        cryptoManager.removeKey()
    }

    @Test
    fun encryptionDecryption_roundTrip_returnsOriginalPlaintext() {
        val samplePlaintext = "dummy_gemini_api_key_for_testing_12345"
        val payload = cryptoManager.encrypt(samplePlaintext)

        assertNotNull(payload.ciphertextBase64)
        assertNotNull(payload.ivBase64)
        assertNotEquals(samplePlaintext, payload.ciphertextBase64)

        val decrypted = cryptoManager.decrypt(payload.ciphertextBase64, payload.ivBase64)
        assertEquals(samplePlaintext, decrypted)
    }

    @Test
    fun encryption_usesFreshIvEachTime() {
        val samplePlaintext = "identical_plaintext"
        val payload1 = cryptoManager.encrypt(samplePlaintext)
        val payload2 = cryptoManager.encrypt(samplePlaintext)

        // AES-GCM must generate unique IV for every encryption call
        assertNotEquals(payload1.ivBase64, payload2.ivBase64)
        assertNotEquals(payload1.ciphertextBase64, payload2.ciphertextBase64)

        // Both must still decrypt to the original plaintext
        assertEquals(samplePlaintext, cryptoManager.decrypt(payload1.ciphertextBase64, payload1.ivBase64))
        assertEquals(samplePlaintext, cryptoManager.decrypt(payload2.ciphertextBase64, payload2.ivBase64))
    }

    @Test(expected = Exception::class)
    fun decryption_corruptedCiphertext_throwsException() {
        val payload = cryptoManager.encrypt("test_message")
        // Tamper with the ciphertext
        val corruptedCiphertext = "AAAA" + payload.ciphertextBase64.drop(4)
        cryptoManager.decrypt(corruptedCiphertext, payload.ivBase64)
    }

    @Test(expected = Exception::class)
    fun decryption_corruptedIv_throwsException() {
        val payload = cryptoManager.encrypt("test_message")
        // Tamper with the IV
        val corruptedIv = "BBBB" + payload.ivBase64.drop(4)
        cryptoManager.decrypt(payload.ciphertextBase64, corruptedIv)
    }

    @Test(expected = InvalidKeyException::class)
    fun removeKey_thenDecrypt_throwsInvalidKeyException() {
        val payload = cryptoManager.encrypt("key_removal_test")
        cryptoManager.removeKey()
        cryptoManager.decrypt(payload.ciphertextBase64, payload.ivBase64)
    }
}
