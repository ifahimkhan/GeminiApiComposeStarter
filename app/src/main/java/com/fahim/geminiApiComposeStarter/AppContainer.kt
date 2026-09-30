package com.fahim.geminiApiComposeStarter

import android.content.Context
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.DataStoreSettingsRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager

/** Tiny manual dependency-injection container (no Hilt needed for a lab). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val apiKeyManager = ApiKeyManager(appContext)

    private val database by lazy { ChatDatabase.create(appContext) }

    val geminiRepository: GeminiRepository by lazy { GeminiRepositoryImpl(apiKeyManager) }
    val chatHistoryRepository: ChatHistoryRepository by lazy {
        RoomChatHistoryRepository(database.messageDao())
    }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext) }
}
