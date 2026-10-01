package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.ApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var apiKeyStorage: ApiKeyStorage
    private lateinit var database: AppDatabase
    private lateinit var preferencesRepository: UserPreferencesRepository

    private val viewModel: ChatViewModel by viewModels {
        val hasKey = (BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                BuildConfig.GEMINI_API_KEY != "your_api_key_here")

        val repository = GeminiRepositoryImpl(
            apiKeyProvider = {
                // Decrypt from Android Keystore in memory only when GenerativeModel is created
                val decrypted = apiKeyStorage.getDecryptedApiKey()
                if (!decrypted.isNullOrBlank()) {
                    decrypted
                } else {
                    BuildConfig.GEMINI_API_KEY
                }
            }
        )

        ChatViewModel.factory(
            repository = repository,
            hasApiKey = hasKey,
            chatDao = database.chatMessageDao(),
            preferencesRepository = preferencesRepository
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKeyStorage = ApiKeyStorage(applicationContext)
        database = AppDatabase.getInstance(applicationContext)
        preferencesRepository = UserPreferencesRepository(applicationContext)

        // Encrypt at rest on first launch using AES-256-GCM Keystore
        lifecycleScope.launch {
            if (!apiKeyStorage.hasStoredApiKey() &&
                BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                BuildConfig.GEMINI_API_KEY != "your_api_key_here"
            ) {
                apiKeyStorage.saveEncryptedApiKey(BuildConfig.GEMINI_API_KEY)
            }
        }

        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}
