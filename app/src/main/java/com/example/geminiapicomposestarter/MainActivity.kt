package com.example.geminiapicomposestarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.geminiapicomposestarter.ui.chat.ChatScreen
import com.example.geminiapicomposestarter.ui.chat.ChatViewModel
import com.example.geminiapicomposestarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val app by lazy { application as GeminiChatApplication }

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(
                    geminiRepository = app.geminiRepository,
                    historyRepository = app.historyRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = uiState.isDarkMode || systemDark

            GeminiApiComposeStarterTheme(darkTheme = useDarkTheme) {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
