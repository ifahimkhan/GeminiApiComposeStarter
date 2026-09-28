package com.fahim.geminiApiComposeStarter.data.security

import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * High-level security coordinator that manages encrypting the API key at rest
 * and decrypting it strictly in-memory when the Gemini client is initialized.
 */
class SecureApiKeyManager(
    private val cryptoManager: CryptoManager,
    private val preferencesRepository: UserPreferencesRepository,
) {

    /**
     * Initializes encryption at rest. If the build-time or environment key is present
     * and ciphertext is not yet stored in DataStore, encrypts it via Android Keystore
     * AES-256-GCM and persists the ciphertext.
     */
    suspend fun initializeKeySecurity(rawKey: String = BuildConfig.GEMINI_API_KEY) {
        if (rawKey.isNotBlank()) {
            val existingPayload = preferencesRepository.encryptedApiKeyFlow.firstOrNull()
            if (existingPayload == null) {
                val encrypted = cryptoManager.encrypt(rawKey)
                preferencesRepository.saveEncryptedApiKey(encrypted)
            }
        }
    }

    /**
     * Retrieves and decrypts the API key in memory only for GenerativeModel construction.
     * Never logs, caches long-term, or displays the returned value.
     */
    suspend fun getDecryptedApiKey(): String {
        return try {
            val payload = preferencesRepository.encryptedApiKeyFlow.firstOrNull()
            if (payload != null) {
                val decrypted = cryptoManager.decrypt(payload)
                if (decrypted.isNotBlank()) decrypted else BuildConfig.GEMINI_API_KEY
            } else {
                BuildConfig.GEMINI_API_KEY
            }
        } catch (_: Exception) {
            // Safe fallback if Keystore had a temporary glitch or running in emulator without hardware Keystore
            BuildConfig.GEMINI_API_KEY
        }
    }
}
