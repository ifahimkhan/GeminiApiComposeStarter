package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import androidx.compose.runtime.collectAsState
import com.fahim.geminiApiComposeStarter.data.local.UserPreferences

class MainActivity : ComponentActivity() {

    private val userPreferences by lazy {
        UserPreferences(applicationContext)
    }

    private val secureApiKeyStorage by lazy {
        SecureApiKeyStorage(applicationContext)
    }

    private val chatDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ChatDatabase::class.java,
            "chat_database"
        ).build()
    }

    private val chatHistoryRepository by lazy {
        ChatHistoryRepository(
            chatDatabase.chatMessageDao()
        )
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyProvider = {
                    secureApiKeyStorage.getApiKey()
                }
            ),
            chatHistoryRepository = chatHistoryRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeSecureApiKey()

        enableEdgeToEdge()

        setContent {
            val dynamicColorEnabled =
                userPreferences.dynamicColorEnabled.collectAsState(
                    initial = true
                )

            GeminiApiComposeStarterTheme(
                dynamicColor = dynamicColorEnabled.value
            ) {
                val windowSizeClass = calculateWindowSizeClass(this)

                ChatRoute(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    private fun initializeSecureApiKey() {
        lifecycleScope.launch {
            if (
                !secureApiKeyStorage.hasStoredApiKey() &&
                BuildConfig.GEMINI_API_KEY.isNotBlank()
            ) {
                secureApiKeyStorage.saveApiKey(
                    BuildConfig.GEMINI_API_KEY
                )
            }
        }
    }
}