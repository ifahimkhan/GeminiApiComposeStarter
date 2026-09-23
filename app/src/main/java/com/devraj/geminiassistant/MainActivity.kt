package com.devraj.geminiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.devraj.geminiassistant.ui.chat.ChatRoute
import com.devraj.geminiassistant.ui.chat.ChatViewModel
import com.devraj.geminiassistant.ui.theme.GeminiAssistantTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val app = application as GeminiChatApplication
        ChatViewModel.factory(
            repository = app.repository,
            hasApiKey = app.secureKeyStorage.hasApiKey(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiAssistantTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
