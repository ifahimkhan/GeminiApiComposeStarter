package com.example.c031_geminiapicompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.c031_geminiapicompose.data.GeminiRepositoryImpl
import com.example.c031_geminiapicompose.data.local.AppDatabase
import com.example.c031_geminiapicompose.data.preferences.UserPreferencesRepository
import com.example.c031_geminiapicompose.security.CryptoManager
import com.example.c031_geminiapicompose.ui.chat.ChatRoute
import com.example.c031_geminiapicompose.ui.chat.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val encryptedApiKey = CryptoManager.encrypt(BuildConfig.GEMINI_API_KEY)
        val repository = GeminiRepositoryImpl(
            encryptedApiKey = encryptedApiKey,
            chatMessageDao = database.chatMessageDao()
        )
        val userPreferencesRepository = UserPreferencesRepository(applicationContext)

        ChatViewModel.factory(
            repository = repository,
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatRoute(viewModel = viewModel)
        }
    }
}
