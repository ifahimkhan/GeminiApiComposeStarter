package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.db.AppDatabase
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val apiKeyManager by lazy { ApiKeyManager(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        val apiKey = apiKeyManager.getApiKey()
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyProvider = { apiKeyManager.getApiKey() },
                chatDao = database.chatDao(),
            ),
            hasApiKey = apiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKeyManager.initializeApiKeyIfNeeded()
        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
