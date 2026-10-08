package com.n149.geminichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.n149.geminichat.ui.chat.ChatScreen
import com.n149.geminichat.ui.theme.GeminiChatTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity architecture.
 * Always forces the deep-black AMOLED theme (darkTheme = true).
 * Hilt injects dependencies; Compose handles all UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // darkTheme always true → deep-black AMOLED, dynamic colour disabled
            GeminiChatTheme(darkTheme = true) {
                val windowSizeClass = calculateWindowSizeClass(this)
                ChatScreen(windowSizeClass = windowSizeClass)
            }
        }
    }
}
