package com.fahim.geminiApiComposeStarter.di

import android.content.Context
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.prefs.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.prefs.secureDataStore
import com.fahim.geminiApiComposeStarter.data.prefs.userPreferencesDataStore
import com.fahim.geminiApiComposeStarter.security.ApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel

/**
 * Manual dependency container. The app has one screen and four collaborators, so a DI framework
 * would be more machinery than the problem needs; everything here is lazy and process-scoped.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database by lazy { ChatDatabase.getInstance(appContext) }

    private val apiKeyStore by lazy { ApiKeyStore(appContext.secureDataStore) }

    val chatHistoryRepository: ChatHistoryRepository by lazy {
        RoomChatHistoryRepository(database.chatDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(appContext.userPreferencesDataStore)
    }

    val geminiRepository: GeminiRepository by lazy {
        // The lambda is what keeps the plaintext key off this object: it is decrypted on demand,
        // inside the repository, only when the GenerativeModel is constructed.
        GeminiRepositoryImpl(
            apiKeyProvider = { apiKeyStore.apiKey(BuildConfig.GEMINI_API_KEY) },
        )
    }

    fun chatViewModelFactory() = ChatViewModel.factory(
        repository = geminiRepository,
        history = chatHistoryRepository,
        preferences = userPreferencesRepository,
        hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
    )
}
