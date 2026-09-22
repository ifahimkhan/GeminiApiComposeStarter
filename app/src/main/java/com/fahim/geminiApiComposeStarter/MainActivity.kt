package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.layout.rememberWindowSizeClass
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureApiKeyManager = SecureApiKeyManager(applicationContext)
        secureApiKeyManager.initializeFromBuildConfig(BuildConfig.GEMINI_API_KEY)

        val chatDatabase = ChatDatabase.getDatabase(applicationContext)
        val chatHistoryRepository = ChatHistoryRepositoryImpl(chatDatabase.chatMessageDao())
        val userPreferencesRepository = UserPreferencesRepositoryImpl(applicationContext)

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyProvider = secureApiKeyManager),
            hasApiKey = secureApiKeyManager.hasApiKey() || BuildConfig.GEMINI_API_KEY.isNotBlank(),
            chatHistoryRepository = chatHistoryRepository,
            userPreferencesRepository = userPreferencesRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = rememberWindowSizeClass()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isDarkTheme = when (state.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            GeminiApiComposeStarterTheme(darkTheme = isDarkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                )
            }
        }
    }
}
