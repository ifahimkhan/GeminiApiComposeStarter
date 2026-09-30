package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.PreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val secureApiKeyStore = SecureApiKeyStore(this)
        val preferencesRepository = PreferencesRepositoryImpl(this)
        
        val viewModel: ChatViewModel by viewModels {
            ChatViewModel.factory(
                repository = GeminiRepositoryImpl(secureApiKeyStore = secureApiKeyStore),
                chatHistoryRepository = ChatHistoryRepositoryImpl(database.chatMessageDao()),
                preferencesRepository = preferencesRepository
            )
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by preferencesRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(viewModel = viewModel, windowSizeClass = windowSizeClass)
            }
        }
    }
}

