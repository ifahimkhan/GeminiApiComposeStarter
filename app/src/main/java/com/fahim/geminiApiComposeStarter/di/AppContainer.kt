package com.example.geministarter.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.geministarter.BuildConfig
import com.example.geministarter.data.GeminiRepository
import com.example.geministarter.data.local.AppDatabase
import com.example.geministarter.data.prefs.UserPreferencesRepository
import com.example.geministarter.data.remote.GeminiProxyClient
import com.example.geministarter.data.remote.OkHttpGeminiProxyClient
import com.example.geministarter.security.ApiKeyManager
import com.example.geministarter.ui.chat.ChatViewModel

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
        GeminiRepository(
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
        return ChatViewModel(container.geminiRepository) as T
    }
}
