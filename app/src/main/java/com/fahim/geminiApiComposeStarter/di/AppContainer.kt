package com.fahim.geminiApiComposeStarter.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.prefs.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.KeystoreCipher
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStore

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
private val Context.securePrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs")

/** Minimal manual dependency injection: one instance of each dependency for the whole app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val secureApiKeyStore = SecureApiKeyStore(
        dataStore = appContext.securePrefsDataStore,
        cipher = KeystoreCipher(),
        buildTimeKeyProvider = { BuildConfig.GEMINI_API_KEY },
    )

    val geminiRepository: GeminiRepository = GeminiRepositoryImpl(
        apiKeyProvider = { secureApiKeyStore.decryptApiKey() },
    )

    private val database by lazy { ChatDatabase.create(appContext) }

    val chatHistoryRepository: ChatHistoryRepository by lazy {
        RoomChatHistoryRepository(database.chatDao())
    }

    val userPreferencesRepository: UserPreferencesRepository =
        DataStoreUserPreferencesRepository(appContext.userPrefsDataStore)
}
