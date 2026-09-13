package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.local.PreferencesSettingsRepository
import com.fahim.geminiApiComposeStarter.data.local.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val chatDb = ChatDatabase.getInstance(applicationContext)
        val secureStore = SecureApiKeyStore(applicationContext)
        val apiKey = secureStore.getApiKeyBlocking(BuildConfig.GEMINI_API_KEY)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            chatHistory = RoomChatHistoryRepository(
                messageDao = chatDb.chatMessageDao(),
                conversationDao = chatDb.conversationDao(),
            ),
            settings = PreferencesSettingsRepository(applicationContext),
            hasApiKey = apiKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(themeMode = state.themeMode) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}