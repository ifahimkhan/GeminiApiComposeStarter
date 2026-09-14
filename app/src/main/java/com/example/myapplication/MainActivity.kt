package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.UserPreferencesRepositoryImpl
import com.example.myapplication.data.repository.GeminiRepositoryImpl
import com.example.myapplication.security.SecureKeyStorage
import com.example.myapplication.ui.chat.ChatScreen
import com.example.myapplication.ui.chat.ChatViewModel
import com.example.myapplication.ui.chat.ChatViewModelFactory
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val userPreferencesRepository = UserPreferencesRepositoryImpl(applicationContext)
        val secureKeyStorage = SecureKeyStorage(applicationContext)
        val geminiRepository = GeminiRepositoryImpl(database.chatMessageDao(), secureKeyStorage)

        ChatViewModelFactory(geminiRepository, userPreferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme = uiState.darkModeOverride ?: isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = useDarkTheme) {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
