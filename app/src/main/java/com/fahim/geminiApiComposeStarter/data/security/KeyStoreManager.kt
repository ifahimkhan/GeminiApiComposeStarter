package com.fahim.geminiApiComposeStarter.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages the cryptographic key lifecycle within the hardware-backed or OS-isolated
 * Android Keystore system.
 *
 * Keys generated here never leave the Keystore in plaintext, providing strong protection
 * against unauthorized access and exfiltration from app storage.
 */
class KeyStoreManager(
    private val keyStoreProvider: String = KEYSTORE_PROVIDER_ANDROID,
) {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider).apply {
            load(null)
        }
    }

    /**
     * Retrieves an existing AES-256 key with [alias], or generates a new one inside the
     * Android Keystore if it does not already exist.
     */
    @Synchronized
    fun getOrCreateKey(alias: String = DEFAULT_ALIAS): SecretKey {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            keyStoreProvider,
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plaintext string using the Keystore-managed AES key with AES-GCM.
     */
    fun encrypt(plaintext: String, alias: String = DEFAULT_ALIAS): String {
        val secretKey = getOrCreateKey(alias)
        return AesGcmCipher.encryptString(plaintext, secretKey)
    }

    /**
     * Decrypts Base64-encoded encrypted payload using the Keystore-managed AES key.
     */
    fun decrypt(base64Payload: String, alias: String = DEFAULT_ALIAS): String {
        val secretKey = getOrCreateKey(alias)
        return AesGcmCipher.decryptString(base64Payload, secretKey)
    }

    /**
     * Deletes the key from Keystore (useful for full resets / secure logout).
     */
    fun deleteKey(alias: String = DEFAULT_ALIAS) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    companion object {
        const val KEYSTORE_PROVIDER_ANDROID = "AndroidKeyStore"
        const val DEFAULT_ALIAS = "gemini_api_key_keystore_alias"
        const val AES_KEY_SIZE_BITS = 256
    }
}
