package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyManager

class MainActivity : ComponentActivity() {

    private val apiKeyManager by lazy {
        ApiKeyManager(applicationContext)
    }

    private val viewModel: ChatViewModel by viewModels {
        if (apiKeyManager.getApiKey().isBlank()) {
            apiKeyManager.storeApiKey(BuildConfig.GEMINI_API_KEY)
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyManager = apiKeyManager
            ),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
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
