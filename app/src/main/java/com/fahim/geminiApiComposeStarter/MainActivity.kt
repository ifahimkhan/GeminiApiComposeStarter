package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.local.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.SecurityManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var securityManager: SecurityManager
    private lateinit var database: AppDatabase
    private lateinit var preferencesRepository: UserPreferencesRepository

    private val viewModel: ChatViewModel by viewModels {
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = {
                // Decrypt the API key from Keystore/DataStore only at the exact moment of model instantiation
                securityManager.getDecryptedApiKey()?.takeIf { it.isNotBlank() }
                    ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
            }
        )
        ChatViewModel.factory(
            repository = repository,
            chatDao = database.chatDao(),
            preferencesRepository = preferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getInstance(applicationContext)
        preferencesRepository = UserPreferencesRepository(applicationContext)
        securityManager = SecurityManager(applicationContext)

        // On initial launch, encrypt and persist the API key at rest using Android Keystore AES-256-GCM
        lifecycleScope.launch {
            if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                securityManager.ensureApiKeyStored(BuildConfig.GEMINI_API_KEY)
            }
        }

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
