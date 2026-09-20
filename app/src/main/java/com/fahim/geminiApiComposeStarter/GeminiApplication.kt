package com.fahim.geminiApiComposeStarter

import android.app.Application
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository

class GeminiApplication : Application() {

    val chatHistoryRepository: ChatHistoryRepository by lazy {
        ChatHistoryRepository(ChatDatabase.create(this).chatDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }
}
