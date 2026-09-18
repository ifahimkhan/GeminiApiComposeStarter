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
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureStorage = SecureApiKeyStorage(
            context = applicationContext,
            defaultFallbackKey = BuildConfig.GEMINI_API_KEY,
        )
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = GeminiRepositoryImpl(apiKeyProvider = secureStorage)

        ChatViewModel.factory(
            repository = repository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            chatDao = database.chatDao(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = state.isDarkTheme ?: isSystemDark

            GeminiApiComposeStarterTheme(darkTheme = useDarkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
