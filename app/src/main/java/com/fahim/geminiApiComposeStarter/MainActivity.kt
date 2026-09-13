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
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val secureApiKeyManager by lazy {

        SecureApiKeyManager(
            applicationContext
        )
    }

    private val chatDatabase by lazy {

        ChatDatabase.getDatabase(
            applicationContext
        )
    }

    private val userPreferencesRepository by lazy {

        UserPreferencesRepository(
            applicationContext
        )
    }

    private val viewModel:
            ChatViewModel by viewModels {

        ChatViewModel.factory(

            repository =
                GeminiRepositoryImpl(

                    apiKeyProvider = {

                        secureApiKeyManager
                            .storeApiKeyIfNeeded(
                                BuildConfig.GEMINI_API_KEY
                            )

                        secureApiKeyManager
                            .getDecryptedApiKey()
                            .orEmpty()
                    }
                ),

            chatDao =
                chatDatabase.chatDao(),

            userPreferencesRepository =
                userPreferencesRepository,

            hasApiKey =
                BuildConfig
                    .GEMINI_API_KEY
                    .isNotBlank(),
        )
    }

    @OptIn(
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        /*
         * Store encrypted API key
         * on first launch.
         */
        lifecycleScope.launch {

            secureApiKeyManager
                .storeApiKeyIfNeeded(
                    BuildConfig.GEMINI_API_KEY
                )
        }

        enableEdgeToEdge()

        setContent {

            GeminiApiComposeStarterTheme {

                /*
                 * Determines whether the device
                 * is Compact, Medium or Expanded.
                 */
                val windowSizeClass =
                    calculateWindowSizeClass(
                        this@MainActivity
                    )

                ChatRoute(
                    viewModel = viewModel,

                    windowWidthSizeClass =
                        windowSizeClass
                            .widthSizeClass,
                )
            }
        }
    }
}