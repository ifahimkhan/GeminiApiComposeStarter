package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.data.UserPreferences
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val database by lazy {
        ChatDatabase.getDatabase(
            applicationContext
        )
    }

    private val userPreferences by lazy {
        UserPreferences(
            applicationContext
        )
    }

    private val secureApiKeyManager by lazy {
        SecureApiKeyManager(
            userPreferences = userPreferences
        )
    }

    private val viewModel: ChatViewModel by viewModels {

        ChatViewModel.factory(

            repository = GeminiRepositoryImpl(
                secureApiKeyManager =
                    secureApiKeyManager,

                bootstrapApiKey =
                    BuildConfig.GEMINI_API_KEY
            ),

            hasApiKey =
                BuildConfig.GEMINI_API_KEY.isNotBlank(),

            chatMessageDao =
                database.chatMessageDao(),

            userPreferences =
                userPreferences
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            GeminiApiComposeStarterTheme {

                ChatRoute(
                    viewModel = viewModel
                )
            }
        }
    }
}
