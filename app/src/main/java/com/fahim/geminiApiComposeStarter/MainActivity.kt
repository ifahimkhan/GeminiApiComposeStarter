package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val apiKey = SecureApiKeyStore(applicationContext).getOrSeedApiKey(BuildConfig.GEMINI_API_KEY)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            historyRepository = ChatHistoryRepositoryImpl(applicationContext),
            preferencesRepository = UserPreferencesRepositoryImpl(applicationContext),
            hasApiKey = apiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(darkTheme = darkModeOverride ?: isSystemInDarkTheme()) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
