package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val context = applicationContext
        // Secure API Key Handling: Android Keystore AES-256-GCM
        val secureApiKeyStorage = SecureApiKeyStorage(context)
        secureApiKeyStorage.initializeKeyIfNeeded(BuildConfig.GEMINI_API_KEY)

        // Room Database for persistent Chat History
        val database = ChatDatabase.getDatabase(context)
        val historyRepository = ChatHistoryRepositoryImpl(database.chatMessageDao())

        // Preferences DataStore
        val preferencesRepository = UserPreferencesRepositoryImpl(context)

        // Gemini Repository with on-demand key decryption
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = { secureApiKeyStorage.getDecryptedApiKey() },
        )

        val hasApiKey = secureApiKeyStorage.hasStoredKey() || BuildConfig.GEMINI_API_KEY.isNotBlank()

        ChatViewModel.factory(
            repository = repository,
            historyRepository = historyRepository,
            preferencesRepository = preferencesRepository,
            hasApiKey = hasApiKey,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
