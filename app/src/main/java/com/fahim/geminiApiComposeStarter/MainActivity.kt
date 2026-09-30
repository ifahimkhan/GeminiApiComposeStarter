package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as GeminiApp).container }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            gemini = container.geminiRepository,
            history = container.chatHistoryRepository,
            prefs = container.userPreferencesRepository,
            hasApiKey = container.hasApiKey,
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // First launch: generate the Keystore AES key and store only the ciphertext.
        lifecycleScope.launch { container.secureKeyStore.ensureEncrypted() }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val windowSizeClass = calculateWindowSizeClass(this)
            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(viewModel = viewModel, widthSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }
}
