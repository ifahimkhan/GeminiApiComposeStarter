package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.security.KeystoreSecurityManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val securityManager by lazy { KeystoreSecurityManager(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        val buildConfigApiKey = BuildConfig.GEMINI_API_KEY
        
        // Persist API key encrypted at rest on first launch
        if (buildConfigApiKey.isNotBlank()) {
            securityManager.storeApiKeyEncrypted(buildConfigApiKey)
        }
        
        // Decrypt key in memory only for repository initialization
        val decryptedApiKey = securityManager.getDecryptedApiKey().ifEmpty { buildConfigApiKey }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = decryptedApiKey),
            hasApiKey = decryptedApiKey.isNotBlank(),
            chatDao = database.chatDao()
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
