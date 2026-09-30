package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreference
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.ApiKeyCryptoManager
import com.fahim.geminiApiComposeStarter.security.ApiKeyStorage
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyProvider
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val userPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    private val chatDatabase by lazy {
        ChatDatabase.getInstance(applicationContext)
    }

    private val chatHistoryRepository by lazy {
        ChatHistoryRepository(
            chatDatabase.chatMessageDao()
        )
    }

    private val viewModel: ChatViewModel by viewModels {

        val apiKeyStorage =
            ApiKeyStorage(applicationContext)

        val cryptoManager =
            ApiKeyCryptoManager()

        val secureApiKeyProvider =
            SecureApiKeyProvider(
                storage = apiKeyStorage,
                cryptoManager = cryptoManager
            )

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                secureApiKeyProvider = secureApiKeyProvider,
                buildConfigApiKey = BuildConfig.GEMINI_API_KEY
            ),
            historyRepository = chatHistoryRepository,
            hasApiKey =
                BuildConfig.GEMINI_API_KEY.isNotBlank()
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val windowSizeClass =
                calculateWindowSizeClass(this)

            val themePreference by
            userPreferencesRepository
                .themePreference
                .collectAsStateWithLifecycle(
                    initialValue = ThemePreference.SYSTEM
                )

            val darkTheme =
                when (themePreference) {

                    ThemePreference.SYSTEM ->
                        isSystemInDarkTheme()

                    ThemePreference.LIGHT ->
                        false

                    ThemePreference.DARK ->
                        true
                }

            GeminiApiComposeStarterTheme(
                darkTheme = darkTheme
            ) {

                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass =
                        windowSizeClass.widthSizeClass,
                    themePreference =
                        themePreference,
                    onThemePreferenceChange = { preference ->

                        lifecycleScope.launch {
                            userPreferencesRepository
                                .setThemePreference(
                                    preference
                                )
                        }
                    }
                )
            }
        }
    }
}