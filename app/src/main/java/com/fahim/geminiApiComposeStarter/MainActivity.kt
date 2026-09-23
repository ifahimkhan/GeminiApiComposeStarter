package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureStorage = SecureApiKeyStorage(applicationContext)
        val decryptedKey = secureStorage.getDecryptedApiKey().orEmpty()
        val database = ChatDatabase.getInstance(applicationContext)
        val themePreferencesRepository = ThemePreferencesRepositoryImpl(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = decryptedKey),
            hasApiKey = decryptedKey.isNotBlank(),
            chatMessageDao = database.chatMessageDao(),
            themePreferencesRepository = themePreferencesRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            val secureStorage = SecureApiKeyStorage(applicationContext)
            secureStorage.storeApiKey(BuildConfig.GEMINI_API_KEY)
        }

        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(darkTheme = state.isDarkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
