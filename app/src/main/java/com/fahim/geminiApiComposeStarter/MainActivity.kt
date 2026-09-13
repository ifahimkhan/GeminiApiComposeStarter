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
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.EncryptedData
import com.fahim.geminiApiComposeStarter.security.KeySecurityManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val keySecurityManager by lazy { KeySecurityManager() }
    private val userPreferencesRepository by lazy { UserPreferencesRepositoryImpl(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    @Volatile
    private var inMemoryEncryptedKey: EncryptedData? = null

    private val viewModel: ChatViewModel by viewModels {
        val initialKey = BuildConfig.GEMINI_API_KEY.ifBlank {
            System.getenv("GEMINI_API_KEY") ?: ""
        }

        if (initialKey.isNotBlank()) {
            val encrypted = keySecurityManager.encrypt(initialKey)
            if (encrypted != null) {
                inMemoryEncryptedKey = encrypted
            }
        }

        // Decrypted key provider that decrypts from in-memory encrypted key or DataStore
        val apiKeyProvider: suspend () -> String? = {
            val encryptedData = inMemoryEncryptedKey ?: userPreferencesRepository.encryptedApiKeyData.firstOrNull()
            if (encryptedData != null) {
                keySecurityManager.decrypt(encryptedData)
            } else if (initialKey.isNotBlank()) {
                initialKey
            } else {
                null
            }
        }

        val hasApiKey = initialKey.isNotBlank()

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyProvider = apiKeyProvider),
            chatMessageDao = database.chatMessageDao(),
            chatSessionDao = database.chatSessionDao(),
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = hasApiKey
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val encrypted = inMemoryEncryptedKey
        if (encrypted != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferencesRepository.saveEncryptedApiKey(encrypted)
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme = state.isDarkMode ?: isSystemInDarkTheme()

            GeminiApiComposeStarterTheme(darkTheme = useDarkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
