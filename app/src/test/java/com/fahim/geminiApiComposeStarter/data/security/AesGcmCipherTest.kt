package com.fahim.geminiApiComposeStarter.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AesGcmCipherTest {

    private lateinit var secretKey: SecretKey
    private lateinit var alternateKey: SecretKey

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        secretKey = keyGen.generateKey()
        alternateKey = keyGen.generateKey()
    }

    @Test
    fun encryptAndDecryptString_roundTrip_restoresExactPlaintext() {
        val originalText = "AIzaSyFakeSampleKeyForTesting-1234567890"

        val encryptedBase64 = AesGcmCipher.encryptString(originalText, secretKey)
        assertNotNull(encryptedBase64)
        assertNotEquals(originalText, encryptedBase64)

        val decryptedText = AesGcmCipher.decryptString(encryptedBase64, secretKey)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun encryptString_usesFreshRandomIv_producingDifferentCiphertextsForSameInput() {
        val message = "Sensitive API Key Payload"

        val encrypted1 = AesGcmCipher.encryptString(message, secretKey)
        val encrypted2 = AesGcmCipher.encryptString(message, secretKey)

        // Fresh random IV prevents identical ciphertexts (crucial GCM security property)
        assertNotEquals(encrypted1, encrypted2)

        // But both decrypt back to the identical plaintext
        assertEquals(message, AesGcmCipher.decryptString(encrypted1, secretKey))
        assertEquals(message, AesGcmCipher.decryptString(encrypted2, secretKey))
    }

    @Test
    fun decryptString_withWrongKey_failsAuthentication() {
        val message = "Confidential Credentials"
        val encrypted = AesGcmCipher.encryptString(message, secretKey)

        // Attempting to decrypt with a different AES-256 key must fail AEAD tag validation
        assertThrows(GeneralSecurityException::class.java) {
            AesGcmCipher.decryptString(encrypted, alternateKey)
        }
    }

    @Test
    fun decrypt_withTamperedCiphertext_failsAeadTagVerification() {
        val plaintext = "Tamper Detection Test".toByteArray(Charsets.UTF_8)
        val encryptedBytes = AesGcmCipher.encrypt(plaintext, secretKey)

        // Flip one bit in the ciphertext / tag region
        val tamperedBytes = encryptedBytes.clone()
        tamperedBytes[tamperedBytes.size - 1] = (tamperedBytes[tamperedBytes.size - 1].toInt() xor 0x01).toByte()

        assertThrows(GeneralSecurityException::class.java) {
            AesGcmCipher.decrypt(tamperedBytes, secretKey)
        }
    }

    @Test
    fun decrypt_withTooShortPayload_throwsIllegalArgumentException() {
        val invalidShortPayload = byteArrayOf(0, 0, 0, 12, 1, 2, 3)

        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCipher.decrypt(invalidShortPayload, secretKey)
        }
    }

    @Test
    fun encryptAndDecrypt_emptyString_worksCorrectly() {
        val emptyText = ""
        val encrypted = AesGcmCipher.encryptString(emptyText, secretKey)
        val decrypted = AesGcmCipher.decryptString(encrypted, secretKey)
        assertEquals(emptyText, decrypted)
    }
}
