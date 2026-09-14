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
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        // Run DB and preferences creation on background thread via lazy factory
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = BuildConfig.GEMINI_API_KEY),
            chatMessageDao = AppDatabase.getInstance(applicationContext).chatMessageDao(),
            preferencesRepository = UserPreferencesRepositoryImpl(applicationContext),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            // Use system dark theme as default until DataStore emits,
            // preventing a brief white/black flash on cold start.
            val darkTheme = uiState.isDarkMode || isSystemInDarkTheme()
            GeminiApiComposeStarterTheme(
                darkTheme = darkTheme,
                dynamicColor = false
            ) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
