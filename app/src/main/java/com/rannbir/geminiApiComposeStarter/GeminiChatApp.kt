package com.rannbir.geminiApiComposeStarter

import android.app.Application
import com.rannbir.geminiApiComposeStarter.data.GeminiRepository
import com.rannbir.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.rannbir.geminiApiComposeStarter.data.local.ChatDatabase
import com.rannbir.geminiApiComposeStarter.data.local.UserPreferencesRepository
import com.rannbir.geminiApiComposeStarter.data.security.ApiKeySecurityManager

class GeminiChatApp : Application() {

    lateinit var securityManager: ApiKeySecurityManager
        private set

    lateinit var database: ChatDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var repository: GeminiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Keystore Security Manager & encrypt API key at rest on first launch
        securityManager = ApiKeySecurityManager(this)
        if (!securityManager.hasStoredKey() && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            securityManager.encryptAndStoreApiKey(BuildConfig.GEMINI_API_KEY)
        }

        // 2. Initialize Room Database & Preferences DataStore
        database = ChatDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)

        // 3. Initialize Repository with lazy in-memory key decryption
        repository = GeminiRepositoryImpl(
            apiKeyProvider = {
                securityManager.getDecryptedApiKey() ?: BuildConfig.GEMINI_API_KEY
            },
            chatDao = database.chatDao()
        )
    }

    val hasApiKey: Boolean
        get() = securityManager.hasStoredKey() || BuildConfig.GEMINI_API_KEY.isNotBlank()

    companion object {
        lateinit var instance: GeminiChatApp
            private set
    }
}
