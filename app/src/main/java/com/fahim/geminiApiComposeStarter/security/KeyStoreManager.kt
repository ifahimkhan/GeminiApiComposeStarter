package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray,
) {
    fun toBase64Strings(): Pair<String, String> {
        val ivStr = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherStr = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return Pair(ivStr, cipherStr)
    }

    companion object {
        fun fromBase64Strings(ivStr: String, cipherStr: String): EncryptedPayload {
            val iv = Base64.decode(ivStr, Base64.NO_WRAP)
            val cipher = Base64.decode(cipherStr, Base64.NO_WRAP)
            return EncryptedPayload(iv, cipher)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!iv.contentEquals(other.iv)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

/**
 * Manages encryption and decryption using Android Keystore with AES-256 in GCM mode.
 * The master key never leaves the hardware-backed keystore.
 */
class KeyStoreManager(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE,
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE_BITS)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns the generated 12-byte IV and the ciphertext with GCM authentication tag.
     */
    fun encrypt(plainText: String): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(iv = iv, ciphertext = cipherText)
    }

    /**
     * Decrypts AES-256-GCM ciphertext using the stored key and matching IV.
     */
    fun decrypt(payload: EncryptedPayload): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decryptedBytes = cipher.doFinal(payload.ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "GeminiKeyStoreAlias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
