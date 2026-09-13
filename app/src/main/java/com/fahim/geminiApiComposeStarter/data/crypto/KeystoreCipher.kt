package com.fahim.geminiApiComposeStarter.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Seals a short secret with an AES-256-GCM key that is generated inside the Android Keystore.
 *
 * The key material is created in, and never leaves, the Keystore (hardware backed on devices
 * with a TEE or StrongBox), so the ciphertext can only be opened by this app on this device.
 * GCM gives us authenticated encryption: a tampered blob fails to decrypt instead of
 * silently returning garbage.
 */
object KeystoreCipher {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "gemini_api_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128

    /** Ciphertext plus the randomised IV that GCM needs to open it again. */
    class Sealed(val ciphertext: ByteArray, val iv: ByteArray)

    fun encrypt(plaintext: String): Sealed {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Sealed(ciphertext = ciphertext, iv = cipher.iv)
    }

    fun decrypt(sealed: Sealed): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, sealed.iv),
        )
        return cipher.doFinal(sealed.ciphertext).toString(Charsets.UTF_8)
    }

    /** Drops the Keystore entry, e.g. after the stored blob is found to be unreadable. */
    fun deleteKey() {
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun secretKey(): SecretKey {
        val existing = (keyStore().getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
