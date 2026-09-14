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
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.datastore.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank()
        val apiKeyStore = ApiKeyStore(this).apply { ensureEncrypted(BuildConfig.GEMINI_API_KEY) }
        val db = AppDatabase.getInstance(this)

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyStore = apiKeyStore),
            historyRepository = ChatHistoryRepositoryImpl(db.chatMessageDao()),
            userPreferencesRepository = UserPreferencesRepositoryImpl(this),
            hasApiKey = hasApiKey,
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(dynamicColor = state.useDynamicColor) {
                ChatRoute(viewModel = viewModel, windowSizeClass = windowSizeClass)
            }
        }
    }
}
