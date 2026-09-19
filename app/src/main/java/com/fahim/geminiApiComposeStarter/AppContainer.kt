package com.fahim.geminiApiComposeStarter

import android.content.Context
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.prefs.DataStoreSettingsRepository
import com.fahim.geminiApiComposeStarter.data.prefs.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.prefs.settingsDataStore
import com.fahim.geminiApiComposeStarter.security.ApiKeyStore
import com.fahim.geminiApiComposeStarter.security.KeystoreCipher
import com.fahim.geminiApiComposeStarter.security.secureDataStore

/** Manual dependency container: one instance per process, created by [GeminiApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: ChatDatabase by lazy {
        Room.databaseBuilder(appContext, ChatDatabase::class.java, "chat.db").build()
    }

    private val apiKeyStore = ApiKeyStore(
        dataStore = appContext.secureDataStore,
        cipher = KeystoreCipher(),
        buildTimeKey = BuildConfig.GEMINI_API_KEY,
    )

    val chatHistory: ChatHistoryRepository by lazy { RoomChatHistoryRepository(database.messageDao()) }
    val settings: SettingsRepository = DataStoreSettingsRepository(appContext.settingsDataStore)
    val gemini: GeminiRepository = GeminiRepositoryImpl(apiKeyProvider = apiKeyStore::getApiKey)
}
