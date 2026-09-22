package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val userPreferencesRepository = UserPreferencesRepository(applicationContext)

        val apiKeyManager = ApiKeyManager()
        val rawApiKey = BuildConfig.GEMINI_API_KEY
        val decryptedApiKey = try {
            if (rawApiKey.isNotBlank()) {
                apiKeyManager.decrypt(rawApiKey).ifEmpty { rawApiKey }
            } else {
                ""
            }
        } catch (_: Exception) {
            rawApiKey
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKey = decryptedApiKey,
                messageDao = database.messageDao()
            ),
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = decryptedApiKey.isNotBlank(),
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
