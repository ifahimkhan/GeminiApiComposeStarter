package com.silas.geminichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.silas.geminichat.ui.chat.ChatRoute
import com.silas.geminichat.ui.chat.ChatViewModel
import com.silas.geminichat.ui.theme.SilasGeminiChatTheme

/**
 * @author Silas · n091 (MBA Tech CE, E1)
 */
class MainActivity : ComponentActivity() {

    private val appContainer by lazy { (application as GeminiApplication).container }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            geminiRepository = appContainer.geminiRepository,
            chatHistoryRepository = appContainer.chatHistoryRepository,
            userPreferencesRepository = appContainer.userPreferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            SilasGeminiChatTheme {
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
