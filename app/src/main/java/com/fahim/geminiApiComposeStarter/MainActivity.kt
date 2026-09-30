package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val userPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    private val chatDao by lazy {
        ChatDatabase.getDatabase(applicationContext).chatDao()
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                chatDao = chatDao,
                userPreferencesRepository = userPreferencesRepository,
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Encrypt the API key at rest using Android Keystore AES-256-GCM on first launch
        lifecycleScope.launch {
            userPreferencesRepository.initializeKeyAtRest()
        }

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
