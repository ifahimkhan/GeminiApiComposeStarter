package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatStorage
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.data.UserPreferences
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val secureApiKeyStore by lazy {
        SecureApiKeyStore(this).also { it.saveFromBuildConfig(BuildConfig.GEMINI_API_KEY) }
    }
    private val userPreferences by lazy { UserPreferences(this) }

    private val viewModel: ChatViewModel by viewModels {
        val apiKey = secureApiKeyStore.getApiKey()
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            chatStorage = RoomChatStorage(ChatDatabase.getInstance(this)),
            hasApiKey = apiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val savedDarkTheme by userPreferences.darkTheme.collectAsState(initial = null)
            val darkTheme = savedDarkTheme ?: systemDarkTheme

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        lifecycleScope.launch { userPreferences.saveDarkTheme(!darkTheme) }
                    },
                )
            }
        }
    }
}
