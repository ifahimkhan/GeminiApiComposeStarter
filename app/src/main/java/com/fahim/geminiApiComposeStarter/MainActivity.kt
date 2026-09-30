package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.di.AppContainer
import com.fahim.geminiApiComposeStarter.di.ChatViewModelFactory
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiStarterTheme

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    private val chatViewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(appContainer)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val userPrefs by appContainer.userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = null)

            GeminiStarterTheme(
                themeMode = userPrefs?.themeMode ?: ThemeMode.SYSTEM,
                useDynamicColor = userPrefs?.useDynamicColor ?: true
            ) {
                ChatScreen(viewModel = chatViewModel, windowSizeClass = windowSizeClass)
            }
        }
    }
}
