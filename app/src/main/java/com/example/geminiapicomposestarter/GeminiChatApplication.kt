package com.example.geminiapicomposestarter

import android.app.Application
import com.example.geminiapicomposestarter.data.ChatHistoryRepository
import com.example.geminiapicomposestarter.data.GeminiRepository
import com.example.geminiapicomposestarter.data.local.AppDatabase
import com.example.geminiapicomposestarter.security.KeyStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeminiChatApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var keyStoreManager: KeyStoreManager
        private set

    lateinit var historyRepository: ChatHistoryRepository
        private set

    lateinit var geminiRepository: GeminiRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        keyStoreManager = KeyStoreManager(this)
        historyRepository = ChatHistoryRepository(database.chatDao())

        // On launch: Encrypt and persist API key into AndroidKeyStore + DataStore if available
        CoroutineScope(Dispatchers.IO).launch {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "your_gemini_api_key_here") {
                keyStoreManager.saveEncryptedApiKey(key)
            }
        }

        // Initialize Gemini Repository with lazy decrypted key provider
        geminiRepository = GeminiRepository {
            keyStoreManager.getDecryptedApiKey() ?: BuildConfig.GEMINI_API_KEY
        }
    }
}
