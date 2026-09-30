package com.fahim.geminiApiComposeStarter.data.security

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Low-level cryptographic engine implementing AES-256-GCM authenticated encryption.
 *
 * Security properties:
 * - Cipher: AES (Advanced Encryption Standard)
 * - Block mode: GCM (Galois/Counter Mode) with 128-bit authentication tag
 * - Padding: NoPadding (GCM operates as a stream cipher under the hood)
 * - Nonce/IV: Fresh cryptographically random 12-byte (96-bit) IV per encryption operation
 * - Payload structure: [4 bytes: IV length] + [IV bytes] + [Ciphertext + Auth Tag]
 * - Encoding: Base64 (RFC 4648) for safe storage in string-based media
 */
object AesGcmCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val MIN_PAYLOAD_LENGTH = 4 + GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)

    /**
     * Encrypts plaintext bytes using the provided [SecretKey] with AES-GCM.
     * Generates a fresh, non-deterministic random IV for each invocation.
     *
     * @param plaintext The raw bytes to encrypt.
     * @param secretKey The AES secret key (256 bits).
     * @return Byte array containing [IV length + IV + ciphertext with authentication tag].
     * @throws GeneralSecurityException if encryption fails.
     */
    fun encrypt(plaintext: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: throw GeneralSecurityException("Cipher did not generate a valid IV")
        val ciphertext = cipher.doFinal(plaintext)

        val buffer = ByteBuffer.allocate(4 + iv.size + ciphertext.size)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(ciphertext)
        return buffer.array()
    }

    /**
     * Decrypts and verifies the authenticated [encryptedPayload] using the provided [SecretKey].
     *
     * @param encryptedPayload Byte array containing [IV length + IV + ciphertext with tag].
     * @param secretKey The AES secret key that was used during encryption.
     * @return Decrypted plaintext bytes.
     * @throws IllegalArgumentException if the payload format or length is invalid.
     * @throws GeneralSecurityException if ciphertext has been tampered with or key is incorrect.
     */
    fun decrypt(encryptedPayload: ByteArray, secretKey: SecretKey): ByteArray {
        if (encryptedPayload.size < MIN_PAYLOAD_LENGTH) {
            throw IllegalArgumentException("Encrypted payload too short (size: ${encryptedPayload.size})")
        }

        val buffer = ByteBuffer.wrap(encryptedPayload)
        val ivSize = buffer.int
        if (ivSize < 8 || ivSize > 64 || buffer.remaining() < ivSize + (GCM_TAG_LENGTH_BITS / 8)) {
            throw IllegalArgumentException("Invalid IV size ($ivSize) or truncated payload")
        }

        val iv = ByteArray(ivSize).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Convenience method to encrypt a UTF-8 string into a Base64-encoded encrypted payload.
     */
    fun encryptString(plaintext: String, secretKey: SecretKey): String {
        val encryptedBytes = encrypt(plaintext.toByteArray(Charsets.UTF_8), secretKey)
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    /**
     * Convenience method to decrypt a Base64-encoded encrypted payload into a UTF-8 string.
     */
    fun decryptString(base64Payload: String, secretKey: SecretKey): String {
        val encryptedBytes = Base64.getDecoder().decode(base64Payload)
        val decryptedBytes = decrypt(encryptedBytes, secretKey)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
