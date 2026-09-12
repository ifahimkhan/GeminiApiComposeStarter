package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.AppTheme
import com.fahim.geminiApiComposeStarter.data.ThemeRepository

import com.fahim.geminiApiComposeStarter.data.local.AppDatabase

class MainActivity : ComponentActivity() {

    private val securityManager by lazy { com.fahim.geminiApiComposeStarter.security.SecurityManager(applicationContext) }
    private val themeRepository by lazy { ThemeRepository(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        if (!securityManager.hasStoredKey() && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            securityManager.encryptAndStoreKey(BuildConfig.GEMINI_API_KEY)
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                messageDao = database.messageDao(),
                apiKeyProvider = { securityManager.getDecryptedKey() }
            ),
            themeRepository = themeRepository,
            hasApiKey = { securityManager.getDecryptedKey().isNotBlank() },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isDarkTheme = when (state.selectedTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            
            GeminiApiComposeStarterTheme(darkTheme = isDarkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
