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

    private val viewModel: ChatViewModel by viewModels { ChatViewModel.Factory }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Only the theme preferences are collected here, so a keystroke in the prompt
            // field does not invalidate the whole tree.
            val preferences by viewModel.themePreferences.collectAsStateWithLifecycle()

            GeminiApiComposeStarterTheme(
                themeMode = preferences.themeMode,
                dynamicColor = preferences.dynamicColour,
            ) {
                ChatRoute(
                    viewModel = viewModel,
                    // Recalculated on rotation and on foldable posture changes.
                    widthSizeClass = calculateWindowSizeClass(this).widthSizeClass,
                )
            }
        }
    }
}
