package com.fahim.geminiApiComposeStarter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class AesGcmCryptoTest {

    @Test
    fun aes256Gcm_encryptAndDecryptRoundTrip_preservesSecretInMemory() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()

        val sampleSecret = "dummy_test_secret_payload_token_12345"
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val encryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = encryptCipher.doFinal(sampleSecret.toByteArray(Charsets.UTF_8))

        assertNotEquals(sampleSecret, String(ciphertext, Charsets.UTF_8))
        assertFalse(String(ciphertext, Charsets.UTF_8).contains("dummy_test_secret"))

        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = decryptCipher.doFinal(ciphertext)
        val decrypted = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(sampleSecret, decrypted)
    }

    @Test
    fun encryptedPayload_equalityAndHashCode() {
        val iv1 = byteArrayOf(1, 2, 3)
        val data1 = byteArrayOf(4, 5, 6)
        val iv2 = byteArrayOf(1, 2, 3)
        val data2 = byteArrayOf(4, 5, 6)

        val payload1 = EncryptedPayload(iv1, data1)
        val payload2 = EncryptedPayload(iv2, data2)

        assertEquals(payload1, payload2)
        assertEquals(payload1.hashCode(), payload2.hashCode())
    }
}
