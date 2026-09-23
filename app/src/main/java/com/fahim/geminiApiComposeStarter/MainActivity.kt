package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.local.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        // 1. Initialize Android Keystore-backed AES-256-GCM secure storage
        val secureStorage = SecureApiKeyStorage(applicationContext)

        // Seed with BuildConfig key if provided at compile-time/local.properties
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            secureStorage.saveApiKey(BuildConfig.GEMINI_API_KEY)
        }

        // 2. Initialize Room database and User Preferences
        val database = AppDatabase.getDatabase(applicationContext)
        val preferencesRepository = UserPreferencesRepository(applicationContext)

        // 3. Initialize repository with lazy in-memory key resolution
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = { secureStorage.getDecryptedApiKey() },
            chatMessageDao = database.chatMessageDao(),
        )

        ChatViewModel.factory(
            repository = repository,
            userPreferencesRepository = preferencesRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(themeMode = uiState.themeMode) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
