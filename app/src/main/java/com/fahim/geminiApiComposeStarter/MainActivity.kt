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
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val container = (application as GeminiApp).container
        ChatViewModel.factory(
            geminiRepository = container.geminiRepository,
            historyRepository = container.chatHistoryRepository,
            preferencesRepository = container.userPreferencesRepository,
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Only the theme-related part of the state drives the theme, so chat updates
            // don't recompose the theme.
            val preferences by remember {
                viewModel.uiState.map { it.preferences }.distinctUntilChanged()
            }.collectAsStateWithLifecycle(initialValue = UserPreferences())

            val darkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val windowSizeClass = calculateWindowSizeClass(this)

            GeminiApiComposeStarterTheme(darkTheme = darkTheme, dynamicColor = preferences.dynamicColor) {
                ChatRoute(viewModel = viewModel, windowSizeClass = windowSizeClass)
            }
        }
    }
}
