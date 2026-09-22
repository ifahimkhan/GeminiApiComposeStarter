package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.AppPreferences
import com.fahim.geminiApiComposeStarter.data.ChatHistoryDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val keyManager = SecureApiKeyManager(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyProvider = { keyManager.getOrInitializeKey(BuildConfig.GEMINI_API_KEY) }),
            history = RoomChatHistoryRepository(ChatHistoryDatabase.create(applicationContext).messageDao()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            val systemDark = isSystemInDarkTheme()
            val savedDarkMode by preferences.darkMode.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            val darkTheme = savedDarkMode ?: systemDark
            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    isExpanded = calculateWindowSizeClass(this@MainActivity).widthSizeClass != WindowWidthSizeClass.Compact,
                    darkTheme = darkTheme,
                    onThemeToggle = { scope.launch { preferences.setDarkMode(!darkTheme) } },
                )
            }
        }
    }
}
