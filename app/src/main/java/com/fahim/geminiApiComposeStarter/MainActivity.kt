package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.google.ai.client.generativeai.BuildConfig

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureStorage = SecureApiKeyStorage(applicationContext)

        val existingKey = secureStorage.getApiKey()

        val apiKey = existingKey
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
                ?.also { secureStorage.saveApiKey(it) }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKey = apiKey.orEmpty(),
            ),
            hasApiKey = !apiKey.isNullOrBlank(),
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