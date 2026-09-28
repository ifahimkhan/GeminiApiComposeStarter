package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.room.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        // 1. Hardware-backed Android Keystore key management (AES-256-GCM)
        val apiKeyManager = ApiKeyManager(applicationContext)
        apiKeyManager.initializeKey(BuildConfig.GEMINI_API_KEY)

        // 2. Decrypt API key strictly in-memory for Gemini API client initialization
        val effectiveApiKey = apiKeyManager.getDecryptedApiKey()
            ?: BuildConfig.GEMINI_API_KEY

        // 3. Initialize persistent Room chat database
        val database = ChatDatabase.getDatabase(applicationContext)

        // 4. Initialize Gemini repository abstraction
        val geminiRepository = GeminiRepositoryImpl(apiKey = effectiveApiKey)

        // 5. Initialize coordinated Chat repository
        val chatRepository = ChatRepositoryImpl(
            chatDao = database.chatDao(),
            geminiRepository = geminiRepository
        )

        ChatViewModel.factory(
            chatRepository = chatRepository,
            hasApiKey = effectiveApiKey.isNotBlank()
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
