package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.ChatStorage
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

import com.fahim.geminiApiComposeStarter.security.ApiKeySecurityManager

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        // Encrypt at rest on first launch using AES-256-GCM backed by Android Keystore
        ApiKeySecurityManager.secureApiKey(applicationContext, BuildConfig.GEMINI_API_KEY)

        ChatViewModel.factory(
            application = application,
            repository = GeminiRepositoryImpl(
                apiKeyProvider = {
                    ApiKeySecurityManager.getDecryptedApiKey(applicationContext, BuildConfig.GEMINI_API_KEY)
                }
            ),
            storage = ChatStorage(applicationContext),
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
