package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val app = application as GeminiApplication
        val apiKeyManager = ApiKeyManager(applicationContext)

        var apiKey = apiKeyManager.getApiKey()
        val configuredApiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (configuredApiKey.isNotEmpty() && apiKey != configuredApiKey) {
            apiKeyManager.saveApiKey(configuredApiKey)
            apiKey = apiKeyManager.getApiKey()
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey ?: ""),
            historyRepository = app.chatHistoryRepository,
            preferencesRepository = app.userPreferencesRepository,
            hasApiKey = !apiKey.isNullOrBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val dynamicColor by viewModel.uiState
                .map { it.dynamicColor }
                .distinctUntilChanged()
                .collectAsStateWithLifecycle(initialValue = true)
            GeminiApiComposeStarterTheme(dynamicColor = dynamicColor) {
                ChatRoute(
                    viewModel = viewModel,
                    windowSizeClass = calculateWindowSizeClass(this),
                )
            }
        }
    }
}
