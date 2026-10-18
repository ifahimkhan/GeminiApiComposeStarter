package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val userPrefsRepository by lazy {
        UserPreferencesRepositoryImpl(applicationContext)
    }

    private val chatHistoryRepository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        ChatHistoryRepositoryImpl(database.chatMessageDao())
    }

    private val secureApiKeyStorage by lazy {
        SecureApiKeyStorage(applicationContext)
    }

    private val viewModel: ChatViewModel by viewModels {
        // Secure bootstrap: encrypt build key into Keystore-backed ciphertext on first launch.
        // Decrypt strictly into volatile memory for GeminiRepository initialization.
        val resolvedApiKey: String = runBlocking {
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            if (buildKey.isNotEmpty() && !secureApiKeyStorage.hasEncryptedKey()) {
                secureApiKeyStorage.saveApiKey(buildKey)
            }
            secureApiKeyStorage.getDecryptedApiKey() ?: buildKey
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = resolvedApiKey),
            chatHistoryRepository = chatHistoryRepository,
            userPreferencesRepository = userPrefsRepository,
            hasApiKey = resolvedApiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = uiState.isDarkMode ?: systemInDark

            GeminiApiComposeStarterTheme(darkTheme = useDarkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
