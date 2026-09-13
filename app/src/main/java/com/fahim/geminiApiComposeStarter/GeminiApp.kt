package com.fahim.geminiApiComposeStarter

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.ApiKeyStore
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.prefs.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gemini_settings")

class GeminiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Hand-rolled dependency container. Small enough that Hilt would be overkill, and it keeps
 * every dependency swappable for a fake in tests.
 */
class AppContainer(context: Context) {

    private val dataStore = context.dataStore

    private val database: ChatDatabase = Room
        .databaseBuilder(context, ChatDatabase::class.java, "chat.db")
        .build()

    private val apiKeyStore = ApiKeyStore(
        dataStore = dataStore,
        buildTimeKey = BuildConfig.GEMINI_API_KEY,
    )

    /** The key is decrypted lazily, only when the first request needs it. */
    val geminiRepository: GeminiRepository = GeminiRepositoryImpl { apiKeyStore.apiKey() }

    val chatHistoryRepository: ChatHistoryRepository =
        RoomChatHistoryRepository(database.messageDao())

    val userPreferencesRepository: UserPreferencesRepository =
        DataStoreUserPreferencesRepository(dataStore)
}
