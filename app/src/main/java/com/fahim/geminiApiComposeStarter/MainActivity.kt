package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.UserPreferences
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyVault
import com.fahim.geminiApiComposeStarter.network.NetworkMonitor
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val database by lazy {
        ChatDatabase.getInstance(applicationContext)
    }

    private val preferences by lazy {
        UserPreferences(applicationContext)
    }

    private val networkMonitor by lazy {
        NetworkMonitor(applicationContext)
    }

    private val apiKeyVault by lazy {
        ApiKeyVault(applicationContext).also { vault ->
            vault.provision(
                BuildConfig.GEMINI_API_KEY
            )
        }
    }

    private val geminiRepository by lazy {
        GeminiRepositoryImpl(
            apiKeyProvider = {
                apiKeyVault.readApiKey()
            }
        )
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = geminiRepository,
            chatDao = database.chatDao(),
            preferences = preferences,
            connectivityObserver = networkMonitor,
            hasApiKey = apiKeyVault.hasApiKey()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel
                )
            }
        }
    }
}