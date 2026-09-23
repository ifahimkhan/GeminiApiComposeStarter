package com.rannbir.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rannbir.geminiApiComposeStarter.ui.chat.ChatRoute
import com.rannbir.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.rannbir.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val app = application as GeminiChatApp
        ChatViewModel.factory(
            repository = app.repository,
            preferencesRepository = app.preferencesRepository,
            hasApiKey = app.hasApiKey,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
