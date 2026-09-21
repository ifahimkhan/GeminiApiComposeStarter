package com.fahim.geminiApiComposeStarter

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.FileImageStore
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyStore
import com.fahim.geminiApiComposeStarter.speech.AndroidTextSpeaker
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val imageStore = FileImageStore(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyProvider = ApiKeyStore(applicationContext, BuildConfig.GEMINI_API_KEY)::getApiKey,
                imageStore = imageStore,
            ),
            history = RoomChatHistoryRepository(AppDatabase.create(applicationContext).chatDao()),
            preferences = DataStoreUserPreferencesRepository(applicationContext),
            imageStore = imageStore,
            speaker = AndroidTextSpeaker(applicationContext),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep status/navigation bar icons legible when the in-app theme differs from the system's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                    ) { darkTheme },
                )
                onDispose {}
            }

            GeminiApiComposeStarterTheme(darkTheme = darkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    widthSizeClass = calculateWindowSizeClass(this).widthSizeClass,
                )
            }
        }
    }
}
