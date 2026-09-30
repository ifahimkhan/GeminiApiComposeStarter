package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.preferences.appDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.crypto.AEADBadTagException

private const val TAG = "ApiKeyProvider"

val KEY_CIPHERTEXT = stringPreferencesKey("encrypted_api_key_ciphertext")
val KEY_IV = stringPreferencesKey("encrypted_api_key_iv")

/**
 * Stores a SHA-256 fingerprint of the BuildConfig key so that if the key in local.properties
 * changes between builds the stored ciphertext is invalidated and reseeded automatically (§2.1).
 */
val KEY_BUILD_FINGERPRINT = stringPreferencesKey("encrypted_api_key_build_fingerprint")

/** Interface providing access to the decrypted API key in memory. */
interface ApiKeyProvider {
    suspend fun getApiKey(): String?
}

/**
 * Implementation of [ApiKeyProvider] that encrypts the API key at rest using
 * AndroidKeyStore (AES-256-GCM) and stores ciphertext, IV, and a SHA-256 fingerprint
 * of the BuildConfig key inside Preferences DataStore.
 *
 * **Key rotation (§2.1):** On every call, if the fingerprint of the current BuildConfig
 * key differs from the stored fingerprint (i.e. the developer rotated the key in
 * `local.properties` and rebuilt), the stored ciphertext is discarded and the new key is
 * encrypted and stored. This ensures key rotation takes effect on the very first launch
 * after a rebuild.
 *
 * **In-memory cache:** [cachedKey] prevents redundant DataStore / Keystore I/O within a
 * single process lifetime. Call [clearCache] to force a fresh read (e.g. in tests).
 */
class SecureApiKeyProvider(
    private val cryptoManager: CryptoManager,
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val buildKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
) : ApiKeyProvider {

    constructor(
        context: Context,
        cryptoManager: CryptoManager = CryptoManager(),
        dataStore: DataStore<Preferences> = context.appDataStore,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        buildKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
    ) : this(
        cryptoManager = cryptoManager,
        dataStore = dataStore,
        ioDispatcher = ioDispatcher,
        buildKeyProvider = buildKeyProvider,
    )

    @Volatile
    private var cachedKey: String? = null

    override suspend fun getApiKey(): String? {
        cachedKey?.let { return it }
        return withContext(ioDispatcher) {
            resolveKey().also { cachedKey = it }
        }
    }

    /** Clears the in-memory cache so the next [getApiKey] call re-reads DataStore. */
    fun clearCache() {
        cachedKey = null
    }

    private suspend fun resolveKey(): String? {
        val prefs = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Failed to read Preferences DataStore", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .first()

        val ciphertext = prefs[KEY_CIPHERTEXT]
        val iv = prefs[KEY_IV]
        val storedFingerprint = prefs[KEY_BUILD_FINGERPRINT]

        val buildKey = buildKeyProvider().trim()
        val buildFingerprint = if (buildKey.isNotBlank()) sha256HexOf(buildKey) else null

        // §2.1 — Key rotation: if fingerprint differs or key was removed (blank),
        // discard the old ciphertext and reseed/clear from the new BuildConfig key.
        val shouldReseed = (buildKey.isBlank() && !ciphertext.isNullOrBlank()) ||
                (buildFingerprint != null &&
                        storedFingerprint != null &&
                        buildFingerprint != storedFingerprint)
        if (shouldReseed) {
            Log.i(TAG, "BuildConfig key changed or removed — reseeding encrypted key at rest.")
            return reseedFromBuildConfig(buildKey)
        }

        if (!ciphertext.isNullOrBlank() && !iv.isNullOrBlank()) {
            try {
                val decrypted = cryptoManager.decrypt(ciphertext, iv)
                if (decrypted.isNotBlank()) {
                    // Semantics Decision - Legacy Migration:
                    // If ciphertext and IV exist in DataStore but no fingerprint was recorded
                    // (i.e. an existing installation upgrading from a prior version without fingerprints),
                    // we trust the decrypted key and backfill its fingerprint into DataStore.
                    // This preserves valid encrypted state while enabling future rotation detection.
                    if (storedFingerprint == null) {
                        dataStore.edit { p ->
                            p[KEY_BUILD_FINGERPRINT] = sha256HexOf(decrypted)
                        }
                    }
                    return decrypted
                }
            } catch (e: Exception) {
                when (e) {
                    is AEADBadTagException,
                    is java.security.InvalidKeyException,
                    is GeneralSecurityException,
                    is IllegalArgumentException -> {
                        Log.e(TAG, "Decryption error — attempting recovery and re-seeding.", e)
                    }
                    else -> {
                        Log.e(TAG, "Unexpected error during API key decryption.", e)
                    }
                }
                // Corrupted ciphertext, invalid tag, or lost Keystore key: safely re-seed.
                return reseedFromBuildConfig(buildKey)
            }
        }

        // First launch or missing encrypted key: seed from BuildConfig if present.
        return reseedFromBuildConfig(buildKey)
    }

    private suspend fun reseedFromBuildConfig(buildKey: String = buildKeyProvider().trim()): String? {
        if (buildKey.isBlank()) {
            dataStore.edit { prefs ->
                prefs.remove(KEY_CIPHERTEXT)
                prefs.remove(KEY_IV)
                prefs.remove(KEY_BUILD_FINGERPRINT)
            }
            return null
        }

        return try {
            cryptoManager.generateSecretKey()
            val payload = cryptoManager.encrypt(buildKey)
            val fingerprint = sha256HexOf(buildKey)
            dataStore.edit { prefs ->
                prefs[KEY_CIPHERTEXT] = payload.ciphertextBase64
                prefs[KEY_IV] = payload.ivBase64
                prefs[KEY_BUILD_FINGERPRINT] = fingerprint
            }
            buildKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and persist API key at rest", e)
            buildKey
        }
    }

    companion object {
        /**
         * Returns the lower-case hex SHA-256 digest of [input].
         * Exposed as a static companion function for JVM unit tests (no Android context required).
         *
         * Security Justification (Bare SHA-256 vs HMAC/Salted):
         * The fingerprint stored in DataStore is solely a one-way collision-resistant token
         * used to detect build-key rotation without decrypting ciphertext at rest.
         * The threat model considers candidate key pre-computation / dictionary attacks moot:
         * 1) Legitimate Gemini API keys have ~190+ bits of entropy (random 39-character strings),
         *    making dictionary or rainbow table inversion computationally infeasible.
         * 2) In this mobile architecture, the plaintext key is compiled into the APK dex via
         *    BuildConfig (see Known Limitations in README). An attacker capable of reading the
         *    app's private DataStore can inspect the APK dex directly, so a salted HMAC
         *    provides no additional security boundary.
         */
        fun sha256HexOf(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}
