package com.example.c001manavassignment1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001manavassignment1.data.GeminiRepository
import com.example.c001manavassignment1.data.UserPreferencesRepository
import com.example.c001manavassignment1.data.local.AppDatabase
import com.example.c001manavassignment1.security.ApiKeyManager
import com.example.c001manavassignment1.ui.chat.ChatScreen
import com.example.c001manavassignment1.ui.chat.ChatViewModel
import com.example.c001manavassignment1.ui.theme.C001manavassignment1Theme
import com.google.ai.client.generativeai.GenerativeModel

class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val apiKeyManager = ApiKeyManager(this)
        val userPreferencesRepository = UserPreferencesRepository(this)
        
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(
                        apiKeyManager = apiKeyManager,
                        userPreferencesRepository = userPreferencesRepository,
                        repositoryFactory = { apiKey ->
                            GeminiRepository(
                                generativeModel = GenerativeModel(
                                    modelName = "gemini-3.6-flash",
                                    apiKey = apiKey
                                ),
                                chatMessageDao = database.chatMessageDao(),
                                conversationDao = database.conversationDao()
                            )
                        }
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        
        val viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            C001manavassignment1Theme(darkTheme = uiState.isDarkMode) {
                ChatScreen(
                    uiState = uiState,
                    windowSizeClass = windowSizeClass,
                    onTextChange = viewModel::onInputTextChange,
                    onSend = viewModel::sendMessage,
                    onMicClick = { /* Launcher is inside ChatScreen */ },
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onDismissError = viewModel::clearError,
                    onNewChat = viewModel::startNewChat,
                    onSelectConversation = viewModel::selectConversation,
                    onDeleteConversation = viewModel::deleteConversation,
                    onError = viewModel::onError
                )
            }
        }
    }
}
