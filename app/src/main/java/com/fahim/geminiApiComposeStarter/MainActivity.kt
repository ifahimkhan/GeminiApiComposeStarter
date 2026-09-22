package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val apiKeyStorage = ApiKeyStorage(applicationContext).apply {
            // Encrypt raw BuildConfig key at rest using Android Keystore AES-256-GCM
            initializeKeyAtRest(BuildConfig.GEMINI_API_KEY)
        }
        val decryptedKey = apiKeyStorage.getDecryptedApiKey(BuildConfig.GEMINI_API_KEY)
        val chatDao = ChatDatabase.getInstance(applicationContext).chatDao()

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = decryptedKey),
            hasApiKey = decryptedKey.isNotBlank(),
            chatDao = chatDao,
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
