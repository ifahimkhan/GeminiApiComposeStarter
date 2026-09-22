package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.CryptoManager
import com.fahim.geminiApiComposeStarter.security.SecureKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var secureKeyStorage: SecureKeyStorage

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val chatHistoryRepository = ChatHistoryRepositoryImpl(database.chatMessageDao())
        val userPreferencesRepository = UserPreferencesRepositoryImpl(applicationContext)

        val geminiRepository = GeminiRepositoryImpl(
            keyProvider = {
                // Decrypt in memory only at the moment the API call / model creation happens
                secureKeyStorage.getDecryptedApiKey()
                    ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
            },
        )

        ChatViewModel.factory(
            geminiRepository = geminiRepository,
            chatHistoryRepository = chatHistoryRepository,
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Android Keystore AES-256-GCM encryption at rest on first launch
        val cryptoManager = CryptoManager()
        secureKeyStorage = SecureKeyStorage(applicationContext, cryptoManager)

        lifecycleScope.launch {
            if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                secureKeyStorage.secureKeyAtRest(BuildConfig.GEMINI_API_KEY)
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
