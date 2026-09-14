package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasApiKey = apiKey.isNotBlank() && apiKey != "DEFAULT_KEY"

        val viewModel: ChatViewModel by viewModels {
            ChatViewModel.factory(
                repository = GeminiRepositoryImpl(apiKey = apiKey),
                hasApiKey = hasApiKey,
            )
        }

        setContent {
            val systemInDark = isSystemInDarkTheme()
            // Track theme state; defaults to system preference and survives configuration changes
            var isDarkTheme by rememberSaveable { mutableStateOf(systemInDark) }

            GeminiApiComposeStarterTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false // Set to false to force custom color scheme instead of wallpaper tint
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatRoute(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}
