package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.fahim.geminiApiComposeStarter.BuildConfig
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encapsulates encrypted data consisting of ciphertext and GCM initialization vector.
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

/**
 * Result of API key validation.
 */
sealed class KeyValidationResult {
    data object Valid : KeyValidationResult()
    data object MissingOrBlank : KeyValidationResult()
    data object Placeholder : KeyValidationResult()
    data object InvalidFormat : KeyValidationResult()
}

/**
 * Abstraction for obtaining or creating an AES secret key.
 * Uses AndroidKeyStore by default on Android devices, while allowing in-memory
 * or mock key providers during unit tests.
 */
fun interface KeyProvider {
    fun getOrCreateSecretKey(): SecretKey
}

/**
 * Key provider that securely stores and retrieves keys in the hardware-backed
 * or system Android Keystore.
 */
class AndroidKeyStoreKeyProvider(
    private val keyAlias: String = SecureApiKeyManager.DEFAULT_KEY_ALIAS,
    private val keyStoreType: String = ANDROID_KEYSTORE_TYPE,
) : KeyProvider {

    companion object {
        private const val ANDROID_KEYSTORE_TYPE = "AndroidKeyStore"
    }

    override fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreType).apply { load(null) }
        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            keyStoreType,
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

/**
 * In-memory key provider intended for fast JVM testing without AndroidKeyStore dependencies.
 */
class InMemoryKeyProvider(
    private val secretKey: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey(),
) : KeyProvider {
    override fun getOrCreateSecretKey(): SecretKey = secretKey
}

/**
 * Secure API Key Manager for the Gemini client.
 *
 * Provides runtime key protection using AES-GCM and Android Keystore.
 * Note: While Android Keystore significantly raises the barrier against static analysis and
 * extraction compared to plaintext BuildConfig constants, no client-side secret is entirely
 * immune from extraction on a fully rooted/compromised device. This implementation establishes
 * defense-in-depth by preventing plaintext exposure in source code, build constants, UI state, and logs.
 */
class SecureApiKeyManager(
    private val sharedPreferences: SharedPreferences? = null,
    private val keyProvider: KeyProvider = AndroidKeyStoreKeyProvider(),
    private val bootstrapPayload: String? = null,
    private val hasBootstrapKey: Boolean = false,
) {

    @Volatile
    private var inMemoryEncryptedData: EncryptedData? = null

    /**
     * Checks if a valid API key is currently available (configured via bootstrap or stored).
     */
    fun hasValidApiKey(): Boolean {
        return getApiKey() != null
    }

    /**
     * Retrieves the decrypted Gemini API key on-demand.
     * Returns null if unconfigured, placeholder, or invalid.
     */
    fun getApiKey(): String? {
        // 1. Try reading existing encrypted data from SharedPreferences or in-memory cache
        val storedEncrypted = loadEncryptedData()
        if (storedEncrypted != null) {
            val decrypted = decrypt(storedEncrypted)
            if (isValidKey(decrypted)) {
                return decrypted
            }
        }

        // 2. Fall back to bootstrapping from build configuration if present
        if (hasBootstrapKey && !bootstrapPayload.isNullOrBlank()) {
            val deobfuscated = deobfuscateBootstrapPayload(bootstrapPayload)
            if (isValidKey(deobfuscated)) {
                // Securely store the de-obfuscated key using AES-GCM
                storeApiKey(deobfuscated)
                return deobfuscated
            }
        }

        return null
    }

    /**
     * Encrypts and securely stores an API key using AES-GCM and the configured key provider.
     */
    fun storeApiKey(rawKey: String): Boolean {
        if (!isValidKey(rawKey)) {
            return false
        }
        return try {
            val encrypted = encrypt(rawKey)
            inMemoryEncryptedData = encrypted
            sharedPreferences?.let { prefs ->
                val cipherTextBase64 = Base64.getEncoder().encodeToString(encrypted.ciphertext)
                val ivBase64 = Base64.getEncoder().encodeToString(encrypted.iv)
                prefs.edit()
                    .putString(PREF_KEY_CIPHERTEXT, cipherTextBase64)
                    .putString(PREF_KEY_IV, ivBase64)
                    .apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Clears any stored API key from storage and memory.
     */
    fun clearStoredApiKey() {
        inMemoryEncryptedData = null
        sharedPreferences?.edit()
            ?.remove(PREF_KEY_CIPHERTEXT)
            ?.remove(PREF_KEY_IV)
            ?.apply()
    }

    /**
     * Encrypts plaintext using AES/GCM/NoPadding.
     */
    fun encrypt(plainText: String): EncryptedData {
        require(plainText.isNotEmpty()) { "Plaintext to encrypt cannot be empty" }
        val secretKey = keyProvider.getOrCreateSecretKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return EncryptedData(ciphertext = ciphertext, iv = iv)
    }

    /**
     * Decrypts ciphertext using AES/GCM/NoPadding.
     * Returns null safely on tampering, authentication tag mismatch, or corrupted data.
     */
    fun decrypt(encryptedData: EncryptedData): String? {
        return try {
            if (encryptedData.ciphertext.isEmpty() || encryptedData.iv.isEmpty()) {
                return null
            }
            val secretKey = keyProvider.getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedData.ciphertext)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Validates whether a key string is non-blank, non-placeholder, and meets basic format rules.
     */
    fun validateKey(key: String?): KeyValidationResult {
        if (key.isNullOrBlank()) {
            return KeyValidationResult.MissingOrBlank
        }
        val trimmed = key.trim()
        if (KNOWN_PLACEHOLDERS.any { trimmed.equals(it, ignoreCase = true) } ||
            trimmed.startsWith("YOUR_") ||
            trimmed.startsWith("<") ||
            trimmed.contains("TODO")
        ) {
            return KeyValidationResult.Placeholder
        }
        if (trimmed.length < MIN_KEY_LENGTH || trimmed.contains(" ")) {
            return KeyValidationResult.InvalidFormat
        }
        return KeyValidationResult.Valid
    }

    /**
     * Convenience method returning true only if the key is valid.
     */
    fun isValidKey(key: String?): Boolean = validateKey(key) is KeyValidationResult.Valid

    /**
     * Safely masks an API key for diagnostic reporting without exposing the secret.
     */
    fun maskKey(key: String?): String {
        if (key.isNullOrBlank()) return "[UNCONFIGURED]"
        val trimmed = key.trim()
        if (!isValidKey(trimmed)) return "[INVALID]"
        if (trimmed.length <= 10) return "******"
        val prefix = trimmed.take(6)
        val suffix = trimmed.takeLast(4)
        return "$prefix...$suffix"
    }

    private fun loadEncryptedData(): EncryptedData? {
        inMemoryEncryptedData?.let { return it }

        val prefs = sharedPreferences ?: return null
        val cipherTextBase64 = prefs.getString(PREF_KEY_CIPHERTEXT, null) ?: return null
        val ivBase64 = prefs.getString(PREF_KEY_IV, null) ?: return null

        return try {
            val ciphertext = Base64.getDecoder().decode(cipherTextBase64)
            val iv = Base64.getDecoder().decode(ivBase64)
            EncryptedData(ciphertext, iv).also { inMemoryEncryptedData = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun deobfuscateBootstrapPayload(payload: String): String {
        return try {
            val decoded = Base64.getDecoder().decode(payload)
            val deobfuscated = ByteArray(decoded.size) { i ->
                (decoded[i].toInt() xor BOOTSTRAP_MASK.toInt()).toByte()
            }
            String(deobfuscated, StandardCharsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "gemini_api_key_v1"
        private const val PREFS_NAME = "gemini_secure_prefs"
        private const val PREF_KEY_CIPHERTEXT = "encrypted_api_key"
        private const val PREF_KEY_IV = "encrypted_api_key_iv"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val MIN_KEY_LENGTH = 20
        const val BOOTSTRAP_MASK: Byte = 0x5A

        private val KNOWN_PLACEHOLDERS = setOf(
            "YOUR_API_KEY",
            "YOUR_GEMINI_API_KEY",
            "YOUR_GEMINI_API_KEY_HERE",
            "TODO",
            "API_KEY",
            "CHANGEME",
            "INSERT_KEY_HERE",
        )

        @Volatile
        private var instance: SecureApiKeyManager? = null

        fun getInstance(context: Context): SecureApiKeyManager {
            return instance ?: synchronized(this) {
                instance ?: SecureApiKeyManager(
                    sharedPreferences = context.applicationContext.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE,
                    ),
                    keyProvider = AndroidKeyStoreKeyProvider(),
                    bootstrapPayload = BuildConfig.SECURE_KEY_BOOTSTRAP_PAYLOAD,
                    hasBootstrapKey = BuildConfig.HAS_API_KEY,
                ).also { instance = it }
            }
        }
    }
}
