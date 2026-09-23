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
import com.fahim.geminiApiComposeStarter.security.CryptoManager
import com.fahim.geminiApiComposeStarter.security.EncryptedPayload
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val cryptoManager by lazy { CryptoManager() }
    private val preferencesRepository by lazy { UserPreferencesRepository(applicationContext) }
    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val historyRepository by lazy { ChatHistoryRepositoryImpl(database.chatDao()) }

    // In-memory holder for the Keystore AES-256-GCM encrypted payload
    @Volatile
    private var encryptedKeyPayload: EncryptedPayload? = null

    private val viewModel: ChatViewModel by viewModels {
        val rawApiKey = BuildConfig.GEMINI_API_KEY
        val hasApiKey = rawApiKey.isNotBlank()

        if (hasApiKey) {
            // Secure the API key immediately using Android Keystore AES-256-GCM
            val payload = cryptoManager.encrypt(rawApiKey)
            encryptedKeyPayload = payload
            lifecycleScope.launch {
                preferencesRepository.saveEncryptedApiKey(payload.toCombinedBase64())
            }
        }

        val geminiRepository = GeminiRepositoryImpl(
            apiKeyProvider = {
                // Decrypt via Android Keystore AES-256-GCM only in RAM when making the API call
                encryptedKeyPayload?.let { cryptoManager.decrypt(it) }.orEmpty()
            },
        )

        ChatViewModel.factory(
            repository = geminiRepository,
            hasApiKey = hasApiKey,
            historyRepository = historyRepository,
            preferencesRepository = preferencesRepository,
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
