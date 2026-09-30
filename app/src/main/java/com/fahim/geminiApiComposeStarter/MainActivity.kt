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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Holds the API key encrypted with an Android Keystore AES-256-GCM key
    private val apiKeyStore by lazy { SecureApiKeyStore(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyProvider = { apiKeyStore.getApiKey() }),
            history = RoomChatHistoryRepository(ChatDatabase.getInstance(this).chatDao()),
            preferences = DataStoreUserPreferencesRepository(applicationContext),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // First launch: encrypt the API key and save only the ciphertext in DataStore
        lifecycleScope.launch {
            apiKeyStore.saveIfNeeded(BuildConfig.GEMINI_API_KEY)
        }

        setContent {
            // Phone = Compact, tablet / landscape = Medium or Expanded
            val windowSizeClass = calculateWindowSizeClass(this)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // Saved dark mode choice, or the phone's setting if the user never changed it
            val darkTheme = state.darkMode ?: isSystemInDarkTheme()

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}