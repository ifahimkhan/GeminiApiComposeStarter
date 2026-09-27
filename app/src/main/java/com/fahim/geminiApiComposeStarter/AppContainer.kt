package com.fahim.geminiApiComposeStarter

import android.content.Context
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.history.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.history.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyManager

/**
 * Small hand-rolled dependency container. The app is simple enough that a DI framework (Hilt,
 * Koin) would add more moving parts than it saves — everything here is created once, in
 * [GeminiChatApplication], and reused for the lifetime of the process.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    val chatHistoryRepository: ChatHistoryRepository =
        ChatHistoryRepositoryImpl(database.chatMessageDao())

    val userPreferencesRepository = UserPreferencesRepository(context)

    val hasApiKey: Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    // The plain key from BuildConfig is only used to seed/verify the encrypted copy; the model
    // itself is built from whatever ApiKeyManager decrypts (see ApiKeyManager for details).
    val geminiRepository: GeminiRepository = GeminiRepositoryImpl(
        apiKey = ApiKeyManager(context).getDecryptedKey(BuildConfig.GEMINI_API_KEY),
    )
}
