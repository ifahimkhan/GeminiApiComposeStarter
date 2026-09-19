package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.local.ThemeManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val themeManager by lazy { ThemeManager(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        val database = ChatDatabase.getDatabase(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = BuildConfig.GEMINI_API_KEY),
            chatDao = database.chatDao(),
            themeManager = themeManager,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            
            GeminiApiComposeStarterTheme(
                darkTheme = state.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
            ) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
