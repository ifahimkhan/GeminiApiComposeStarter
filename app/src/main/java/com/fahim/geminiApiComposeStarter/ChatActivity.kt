package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatStore
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatAppScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatAppViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
private const val APP_TITLE = "Kshitij C056 AI chatbot gemini"

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatAppViewModel by viewModels {
        ChatAppViewModel.factory(
            store = ChatStore(applicationContext),
            repository = GeminiRepositoryImpl(BuildConfig.GEMINI_API_KEY),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isDark = state.darkMode ?: isSystemInDarkTheme()

            GeminiApiComposeStarterTheme(darkTheme = isDark) {
                ChatAppScreen(
                    state = state,
                    title = APP_TITLE,
                    isDark = isDark,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::onSend,
                    onNewChat = viewModel::startNewChat,
                    onSelectConversation = viewModel::selectConversation,
                    onDeleteConversation = viewModel::deleteConversation,
                    onDarkModeChange = viewModel::setDarkMode,
                    onVoiceText = viewModel::appendVoiceText,
                    onError = viewModel::showError,
                    onDismissError = viewModel::clearError,
                )
            }
        }
    }
}
