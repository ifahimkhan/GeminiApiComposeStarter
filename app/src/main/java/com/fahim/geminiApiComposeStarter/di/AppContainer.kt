package com.fahim.geminiApiComposeStarter.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.remote.GeminiProxyClient
import com.fahim.geminiApiComposeStarter.data.remote.OkHttpGeminiProxyClient
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel

/**
 * Small, dependency-free composition root. A real project would reach for
 * Hilt/Koin here (the lab's architecture diagram mentions Koin) — this
 * keeps the sample buildable without adding a DI framework dependency.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val apiKeyManager: ApiKeyManager by lazy { ApiKeyManager(appContext) }
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }
    val geminiProxyClient: GeminiProxyClient by lazy {
        OkHttpGeminiProxyClient(BuildConfig.GEMINI_PROXY_URL)
    }
    val geminiRepository: GeminiRepository by lazy {
        com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl(
            chatDao = database.chatDao(),
            apiKeyManager = apiKeyManager,
            proxyClient = geminiProxyClient
        )
    }
}

class ChatViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(ChatViewModel::class.java))
        return ChatViewModel(
            container.geminiRepository,
            container.userPreferencesRepository,
            BuildConfig.GEMINI_API_KEY.isNotBlank()
        ) as T
    }
}
