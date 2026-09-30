package com.fahim.geminiApiComposeStarter.data.security

import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureApiKeyStorage(
    private val preferencesRepository: PreferencesRepository?,
    private val encryptionManagerProvider: () -> ApiKeyEncryptionManager = { ApiKeyEncryptionManager() },
) {

    private val encryptionManager by lazy { encryptionManagerProvider() }

    /** Retrieves decrypted API key in memory, performing first-run encryption migration if needed */
    suspend fun getDecryptedApiKey(): String = withContext(Dispatchers.IO) {
        if (preferencesRepository == null) {
            return@withContext BuildConfig.GEMINI_API_KEY
        }

        return@withContext try {
            val (encryptedKey, iv) = preferencesRepository.getEncryptedApiKeyData()

            if (encryptedKey.isNotBlank() && iv.isNotBlank()) {
                val decrypted = encryptionManager.decrypt(encryptedKey, iv)
                if (decrypted.isNotBlank()) {
                    return@withContext decrypted
                }
            }

            // First-run migration or re-encryption
            val plainKey = BuildConfig.GEMINI_API_KEY
            if (plainKey.isNotBlank()) {
                val (newEncryptedKey, newIv) = encryptionManager.encrypt(plainKey)
                if (newEncryptedKey.isNotBlank() && newIv.isNotBlank()) {
                    preferencesRepository.saveEncryptedApiKey(newEncryptedKey, newIv)
                }
            }
            plainKey
        } catch (_: Exception) {
            BuildConfig.GEMINI_API_KEY
        }
    }
}
