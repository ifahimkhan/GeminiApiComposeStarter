package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.userPreferencesDataStore
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.security.CryptoManager
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.data.security.securityDataStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var secureKeyStorage: SecureApiKeyStorage
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: ChatViewModel by viewModels {
        val database = ChatDatabase.getDatabase(applicationContext)
        val cryptoManager = CryptoManager()
        secureKeyStorage = SecureApiKeyStorage(applicationContext.securityDataStore, cryptoManager)
        userPreferencesRepository = UserPreferencesRepository(applicationContext.userPreferencesDataStore)

        // Seamless first-launch initialization: Encrypt BuildConfig.GEMINI_API_KEY into Keystore
        lifecycleScope.launch(Dispatchers.IO) {
            if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                secureKeyStorage.initializeKey(BuildConfig.GEMINI_API_KEY)
            }
        }

        // The API key is decrypted strictly in memory at the moment of request execution
        val geminiRepository = GeminiRepositoryImpl(
            apiKeyProvider = {
                runBlocking(Dispatchers.IO) {
                    secureKeyStorage.getDecryptedApiKey()
                        ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
                        ?: ""
                }
            }
        )

        val chatRepository = ChatRepositoryImpl(
            chatDao = database.chatDao(),
            geminiRepository = geminiRepository
        )

        ChatViewModel.factory(
            chatRepository = chatRepository,
            secureKeyStorage = secureKeyStorage,
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(appTheme = state.theme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
