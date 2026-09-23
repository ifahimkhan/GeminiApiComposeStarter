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
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.ThemePreference
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository: GeminiRepositoryImpl by lazy {
        GeminiRepositoryImpl(
            keyStorage = ApiKeyStorage(applicationContext)
        )
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = repository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            preferencesRepository = UserPreferencesRepository(
                applicationContext
            ),
            historyRepository = ChatHistoryRepository(
                applicationContext
            )
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prepare encrypted storage without blocking the UI.
        lifecycleScope.launch {
            repository.initialize()
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val useDarkTheme = when (state.themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            GeminiApiComposeStarterTheme(
                darkTheme = useDarkTheme
            ) {
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}