package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreference
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as GeminiChatApplication).container }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            geminiRepository = container.geminiRepository,
            historyRepository = container.chatHistoryRepository,
            hasApiKey = container.hasApiKey,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val themePreference by container.userPreferencesRepository.themePreference
                .collectAsState(initial = ThemePreference.SYSTEM)

            val darkTheme = when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    themePreference = themePreference,
                    onThemePreferenceChange = { preference ->
                        coroutineScope.launch {
                            container.userPreferencesRepository.setThemePreference(preference)
                        }
                    },
                )
            }
        }
    }
}
