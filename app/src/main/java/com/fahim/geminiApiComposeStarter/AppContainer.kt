package com.fahim.geminiApiComposeStarter

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.security.KeystoreCipher
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStore

private val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore("user_preferences")
private val Context.secureKeyStore: DataStore<Preferences> by preferencesDataStore("secure_api_key")

/** Manual dependency container; one instance lives in [GeminiApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val apiKeyStore = SecureApiKeyStore(
        dataStore = appContext.secureKeyStore,
        cipher = KeystoreCipher(),
        buildTimeKey = BuildConfig.GEMINI_API_KEY,
    )

    val geminiRepository: GeminiRepository =
        GeminiRepositoryImpl(apiKeyProvider = apiKeyStore::decrypt)

    val chatHistoryRepository: ChatHistoryRepository =
        RoomChatHistoryRepository(ChatDatabase.create(appContext).chatMessageDao())

    val userPreferencesRepository: UserPreferencesRepository =
        DataStoreUserPreferencesRepository(appContext.userPreferencesStore)
}
