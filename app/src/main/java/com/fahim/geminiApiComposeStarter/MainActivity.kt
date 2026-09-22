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
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        (application as GeminiApp).container.chatViewModelFactory()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Theme choice is persisted state, so it is read from the ViewModel and survives
            // process death along with the rest of the conversation.
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            GeminiApiComposeStarterTheme(
                themeMode = state.themeMode,
                dynamicColor = state.dynamicColor,
            ) {
                // Recalculated on every configuration change, so rotating the phone or unfolding
                // a foldable re-lays out the screen rather than keeping the phone layout.
                val windowSizeClass = calculateWindowSizeClass(this)
                ChatRoute(
                    viewModel = viewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
