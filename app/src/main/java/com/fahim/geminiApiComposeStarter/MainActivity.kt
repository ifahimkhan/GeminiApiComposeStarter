package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureApiKeyManager by lazy {
        SecureApiKeyManager(applicationContext)
    }

    private val appDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val chatHistoryRepository by lazy {
        ChatHistoryRepositoryImpl(appDatabase.chatMessageDao())
    }

    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(applicationContext.preferencesDataStore)
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyProvider = {
                secureApiKeyManager.getDecryptedApiKey()
            }),
            chatHistoryRepository = chatHistoryRepository,
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = secureApiKeyManager.hasApiKey() || BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Securely initialize encrypted key from build configuration if available
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            secureApiKeyManager.storeApiKey(BuildConfig.GEMINI_API_KEY)
        }
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            GeminiApiComposeStarterTheme(darkTheme = isDarkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
