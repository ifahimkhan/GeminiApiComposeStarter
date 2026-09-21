package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.ChatRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize API Key Manager (AES-256-GCM Keystore encryption at rest)
        val apiKeyManager = ApiKeyManager(applicationContext)
        lifecycleScope.launch {
            apiKeyManager.initializeKey(BuildConfig.GEMINI_API_KEY)
        }

        // 2. Initialize Room database for persistent chat history across app restarts
        val database = ChatDatabase.getDatabase(applicationContext)
        val chatMessageDao = database.chatMessageDao()

        // 3. Initialize Gemini Repository with on-demand in-memory decryption
        val geminiRepository = GeminiRepositoryImpl(
            apiKeyProvider = {
                val decrypted = apiKeyManager.getDecryptedApiKey()
                if (decrypted.isNotBlank()) decrypted else BuildConfig.GEMINI_API_KEY
            },
        )

        // 4. Coordinated chat repository
        val chatRepository = ChatRepository(
            geminiRepository = geminiRepository,
            chatMessageDao = chatMessageDao,
        )

        // 5. ViewModel
        val viewModel = ViewModelProvider(
            this,
            ChatViewModel.factory(
                repository = geminiRepository,
                chatRepository = chatRepository,
                hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            ),
        )[ChatViewModel::class.java]

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
