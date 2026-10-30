package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.history.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureKeyStore = SecureKeyStore(applicationContext)
        val chatDatabase = ChatDatabase.getInstance(applicationContext)
        val chatHistoryRepository = ChatHistoryRepositoryImpl(chatDatabase.chatDao())
        val userPreferencesRepository = UserPreferencesRepositoryImpl(applicationContext)

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(secureKeyStore = secureKeyStore),
            chatHistoryRepository = chatHistoryRepository,
            userPreferencesRepository = userPreferencesRepository,
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isDarkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            GeminiApiComposeStarterTheme(
                darkTheme = isDarkTheme,
                dynamicColor = uiState.dynamicColor,
            ) {
                ChatRoute(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                )
            }
        }
    }
}
