package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            ChatDatabase::class.java, "chat-db"
        ).build()
    }

    private val userPrefs by lazy {
        com.fahim.geminiApiComposeStarter.data.UserPreferencesRepositoryImpl(applicationContext)
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = BuildConfig.GEMINI_API_KEY),
            chatDao = db.chatMessageDao(),
            userPrefs = userPrefs,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel, windowSizeClass = windowSizeClass)
            }
        }
    }
}