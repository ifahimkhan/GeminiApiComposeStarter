package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val apiKeyStorage by lazy { SecureApiKeyStorage(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyProvider = {
                    apiKeyStorage.getApiKey() ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
                }
            ),
            apiKeyStorage = apiKeyStorage,
            hasStaticApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
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
