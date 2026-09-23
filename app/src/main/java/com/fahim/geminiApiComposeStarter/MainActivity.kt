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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ApiKeyStorage
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels {
        val apiKey = runBlocking(Dispatchers.IO) {
            ApiKeyStorage(applicationContext).syncAndLoad(BuildConfig.GEMINI_API_KEY)
        }
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            history = RoomChatHistoryRepository(AppDatabase.get(applicationContext).chatDao()),
            hasApiKey = apiKey.isNotBlank(),
        )
    }
    private val prefs by lazy { UserPreferencesRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val darkPref by prefs.darkMode.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = darkPref ?: isSystemInDarkTheme()
            val scope = rememberCoroutineScope()

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    isDark = darkTheme,
                    onToggleDark = { scope.launch { prefs.setDarkMode(!darkTheme) } },
                )
            }
        }
    }
}

