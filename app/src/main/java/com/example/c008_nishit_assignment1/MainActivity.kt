package com.example.c008_nishit_assignment1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c008_nishit_assignment1.data.GeminiRepositoryImpl
import com.example.c008_nishit_assignment1.data.local.ChatDatabase
import com.example.c008_nishit_assignment1.data.preferences.PreferencesRepository
import com.example.c008_nishit_assignment1.data.security.ApiKeyManager
import com.example.c008_nishit_assignment1.ui.chat.ChatScreen
import com.example.c008_nishit_assignment1.ui.chat.ChatViewModel
import com.example.c008_nishit_assignment1.ui.theme.C008_Nishit_Assignment1Theme

/**
 * MainActivity initializes application dependencies and hosts the ChatScreen Composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ChatDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()
        val apiKeyManager = ApiKeyManager(applicationContext)
        val repository = GeminiRepositoryImpl(apiKeyManager)
        val preferencesRepository = PreferencesRepository(applicationContext)

        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository, chatDao, preferencesRepository) as T
            }
        }

        setContent {
            val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
            val uiState by chatViewModel.uiState.collectAsState()

            val isDarkTheme = uiState.isDarkMode ?: isSystemInDarkTheme()

            C008_Nishit_Assignment1Theme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(viewModel = chatViewModel)
                }
            }
        }
    }
}
