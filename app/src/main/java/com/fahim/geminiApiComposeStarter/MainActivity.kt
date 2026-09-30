package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val secureStorage = SecureApiKeyStorage(applicationContext)

        // Protect key at rest using Android Keystore + AES-256-GCM
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            secureStorage.storeEncryptedApiKey(BuildConfig.GEMINI_API_KEY)
        }

        val resolvedApiKey = BuildConfig.GEMINI_API_KEY.ifBlank {
            secureStorage.getDecryptedApiKey().orEmpty()
        }

        val geminiRepository = GeminiRepositoryImpl(apiKey = resolvedApiKey)
        val chatRepository = ChatRepositoryImpl(
            geminiRepository = geminiRepository,
            chatMessageDao = database.chatMessageDao(),
        )
        ChatViewModel.factory(
            repository = chatRepository,
            hasApiKey = resolvedApiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(darkTheme = state.settings.isDarkTheme) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
