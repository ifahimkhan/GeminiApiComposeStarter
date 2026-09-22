package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureApiKeyStorage = SecureApiKeyStorage(applicationContext)
        val chatDatabase = ChatDatabase.getDatabase(applicationContext)
        val userPreferencesRepository = UserPreferencesRepository(applicationContext)

        // On first launch, encrypt and store API key at rest using Android KeyStore AES-256-GCM
        if (BuildConfig.GEMINI_API_KEY.isNotBlank() && !secureApiKeyStorage.hasStoredApiKey()) {
            secureApiKeyStorage.saveApiKey(BuildConfig.GEMINI_API_KEY)
        }

        val hasKey = BuildConfig.GEMINI_API_KEY.isNotBlank() || secureApiKeyStorage.hasStoredApiKey()

        val repository = GeminiRepositoryImpl(
            secureStorage = secureApiKeyStorage,
            chatDao = chatDatabase.chatDao(),
            fallbackRawApiKey = BuildConfig.GEMINI_API_KEY,
        )

        ChatViewModel.factory(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = hasKey,
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
