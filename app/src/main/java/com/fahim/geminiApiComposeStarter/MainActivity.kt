package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.fahim.geminiApiComposeStarter.data.ChatStore
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureApiKeyStore by lazy {
        SecureApiKeyStore(this).also { it.saveFromBuildConfig(BuildConfig.GEMINI_API_KEY) }
    }

    private val viewModel: ChatViewModel by viewModels {
        val apiKey = secureApiKeyStore.getApiKey()
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            chatStore = ChatStore(this),
            hasApiKey = apiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme },
                )
            }
        }
    }
}
