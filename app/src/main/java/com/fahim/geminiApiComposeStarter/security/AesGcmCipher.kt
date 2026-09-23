package com.fahim.geminiApiComposeStarter.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Low-level cryptographic helper implementing AES-256-GCM encryption and decryption.
 *
 * Requirements satisfied:
 * - Uses AES-GCM (NoPadding) with 128-bit authentication tag.
 * - Generates a fresh, cryptographically secure 12-byte IV for every encryption operation.
 * - Decrypts data in-memory only.
 * - Pure cryptographic logic without hardcoded credentials or plaintext logging.
 */
object AesGcmCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_SIZE_BYTES = 12

    private val secureRandom = SecureRandom()

    data class EncryptedPayload(
        val ciphertext: ByteArray,
        val iv: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedPayload
            return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }

    /**
     * Encrypts plaintext using AES-GCM with the provided secret key.
     * Generates a fresh random IV for this specific operation.
     */
    fun encrypt(plaintext: String, secretKey: SecretKey): EncryptedPayload {
        val iv = ByteArray(IV_SIZE_BYTES).apply { secureRandom.nextBytes(this) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(ciphertext = ciphertext, iv = iv)
    }

    /**
     * Decrypts ciphertext using AES-GCM with the provided secret key and IV.
     * The resulting string is kept in-memory only.
     */
    fun decrypt(payload: EncryptedPayload, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plaintextBytes = cipher.doFinal(payload.ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }
}
