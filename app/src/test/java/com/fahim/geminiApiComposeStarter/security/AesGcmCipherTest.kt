package com.fahim.geminiApiComposeStarter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AesGcmCipherTest {

    private lateinit var secretKey: SecretKey

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        secretKey = keyGen.generateKey()
    }

    @Test
    fun encryptAndDecrypt_returnsOriginalPlaintext() {
        val originalKey = "test-gemini-api-key-1234567890"

        val payload = AesGcmCipher.encrypt(originalKey, secretKey)
        assertNotNull(payload)
        assertFalse(payload.ciphertext.contentEquals(originalKey.toByteArray(Charsets.UTF_8)))
        assertEquals(12, payload.iv.size)

        val decrypted = AesGcmCipher.decrypt(payload, secretKey)
        assertEquals(originalKey, decrypted)
    }

    @Test
    fun encryptTwice_generatesDifferentRandomizedIVsAndCiphertexts() {
        val sampleKey = "sample-secret-api-key"

        val payload1 = AesGcmCipher.encrypt(sampleKey, secretKey)
        val payload2 = AesGcmCipher.encrypt(sampleKey, secretKey)

        // Randomized IV requirement: IVs must never repeat
        assertFalse(payload1.iv.contentEquals(payload2.iv))
        assertFalse(payload1.ciphertext.contentEquals(payload2.ciphertext))

        // Both decrypt to the same plaintext
        assertEquals(sampleKey, AesGcmCipher.decrypt(payload1, secretKey))
        assertEquals(sampleKey, AesGcmCipher.decrypt(payload2, secretKey))
    }

    @Test(expected = Exception::class)
    fun decryptWithTamperedCiphertext_fails() {
        val sampleKey = "sample-secret-api-key"
        val payload = AesGcmCipher.encrypt(sampleKey, secretKey)

        // Tamper with ciphertext
        val tamperedCiphertext = payload.ciphertext.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()

        val tamperedPayload = AesGcmCipher.EncryptedPayload(
            ciphertext = tamperedCiphertext,
            iv = payload.iv,
        )

        AesGcmCipher.decrypt(tamperedPayload, secretKey)
    }
}
