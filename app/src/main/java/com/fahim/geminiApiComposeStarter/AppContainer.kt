package com.fahim.geminiApiComposeStarter

import android.app.Application
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStore

/** Simple manual dependency container, created once per process. */
class GeminiApp : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(app: Application) {
    val secureKeyStore = SecureApiKeyStore(app, BuildConfig.GEMINI_API_KEY)
    val hasApiKey: Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    private val database = Room.databaseBuilder(app, ChatDatabase::class.java, "chat.db").build()

    val geminiRepository = GeminiRepositoryImpl(apiKeyProvider = { secureKeyStore.decryptApiKey() })
    val chatHistoryRepository = RoomChatHistoryRepository(database.messageDao())
    val userPreferencesRepository = DataStoreUserPreferencesRepository(app)
}
