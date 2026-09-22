package com.silas.geminichat.di

import android.content.Context
import com.silas.geminichat.BuildConfig
import com.silas.geminichat.data.ChatHistoryRepository
import com.silas.geminichat.data.GeminiRepository
import com.silas.geminichat.data.GeminiRepositoryImpl
import com.silas.geminichat.data.local.ChatDatabase
import com.silas.geminichat.data.local.RoomChatHistoryRepository
import com.silas.geminichat.data.prefs.DataStoreUserPreferencesRepository
import com.silas.geminichat.data.prefs.UserPreferencesRepository
import com.silas.geminichat.data.secure.ApiKeyManager

/**
 * Manual dependency container (no DI framework) so every dependency is a plain
 * object that is easy to swap for a fake in unit tests.
 *
 * @author Silas · n091 (MBA Tech CE, E1)
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database = ChatDatabase.getInstance(appContext)

    val apiKeyManager = ApiKeyManager(appContext)

    val geminiRepository: GeminiRepository = GeminiRepositoryImpl(
        apiKeyManager = apiKeyManager,
        rawApiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() },
    )

    val chatHistoryRepository: ChatHistoryRepository =
        RoomChatHistoryRepository(database.chatMessageDao())

    val userPreferencesRepository: UserPreferencesRepository =
        DataStoreUserPreferencesRepository(appContext)
}
