package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels { ChatViewModel.Factory }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            GeminiTheme(
                themeMode = uiState.settings.themeMode,
                dynamicColor = uiState.settings.dynamicColor,
            ) {
                ChatScreen(
                    uiState = uiState,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage,
                    onVoiceResult = viewModel::onVoiceResult,
                    onVoiceUnavailable = viewModel::onVoiceUnavailable,
                    onRetry = viewModel::retry,
                    onDismissError = viewModel::dismissError,
                    onClearHistory = viewModel::clearHistory,
                    onSettingsOpenChange = viewModel::setSettingsOpen,
                    onThemeModeChange = viewModel::setThemeMode,
                    onDynamicColorChange = viewModel::setDynamicColor,
                )
            }
        }
    }
}
