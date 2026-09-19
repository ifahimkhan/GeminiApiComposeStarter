package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val userPreferencesRepository by lazy { UserPreferencesRepository(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)

        ChatViewModel.factory(

            repository = GeminiRepositoryImpl(
                apiKey = BuildConfig.GEMINI_API_KEY,
                modelName = "gemini-3.6-flash"
            ),
            messageDao = database.messageDao(),
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            lifecycleScope.launch {
                userPreferencesRepository.saveEncryptedApiKey(BuildConfig.GEMINI_API_KEY)
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = uiState.isDarkMode

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
