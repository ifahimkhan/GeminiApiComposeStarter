package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.dataStore
import com.fahim.geminiApiComposeStarter.data.security.CryptoManager
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Main Activity integrating Room database, Preferences DataStore, Android Keystore
 * encryption at rest, and Material 3 Jetpack Compose UI.
 *
 * Student: Tanmay Sharma
 * Roll Number: N159
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = ChatDatabase.getDatabase(applicationContext)
        val chatHistoryRepo = RoomChatHistoryRepository(database.chatDao())
        val prefsRepo = UserPreferencesRepository(applicationContext.dataStore)
        val cryptoManager = CryptoManager()
        val secureKeyManager = SecureApiKeyManager(cryptoManager, prefsRepo)

        val geminiRepo = GeminiRepositoryImpl(
            apiKeyProvider = {
                // Decrypt strictly in-memory when the API request or GenerativeModel is created
                val decrypted = runBlocking { secureKeyManager.getDecryptedApiKey() }
                if (decrypted.isNotBlank()) decrypted else BuildConfig.GEMINI_API_KEY
            },
        )

        ChatViewModel.factory(
            geminiRepository = geminiRepo,
            chatHistoryRepository = chatHistoryRepo,
            userPreferencesRepository = prefsRepo,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Android Keystore AES-256-GCM encryption at rest on first launch
        val prefsRepo = UserPreferencesRepository(applicationContext.dataStore)
        val cryptoManager = CryptoManager()
        val secureKeyManager = SecureApiKeyManager(cryptoManager, prefsRepo)
        lifecycleScope.launch {
            secureKeyManager.initializeKeySecurity(BuildConfig.GEMINI_API_KEY)
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = when (state.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            GeminiApiComposeStarterTheme(darkTheme = isDark) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
