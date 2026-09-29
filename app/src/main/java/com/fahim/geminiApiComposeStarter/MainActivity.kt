package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureStorage by lazy { SecureKeyStorage(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        val key = secureStorage.getApiKey().ifBlank {
            BuildConfig.GEMINI_API_KEY
        }
        if (key.isNotBlank()) {
            secureStorage.storeApiKey(key)
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = key),
            hasApiKey = key.isNotBlank(),
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