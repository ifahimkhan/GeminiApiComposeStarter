package com.fahim.geminiApiComposeStarter

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.DataStoreSettingsRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.useDarkTheme

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = Room.databaseBuilder(applicationContext, ChatDatabase::class.java, "chat.db").build()
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = BuildConfig.GEMINI_API_KEY),
            history = RoomChatHistoryRepository(database.messageDao()),
            settings = DataStoreSettingsRepository(applicationContext.settingsDataStore),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = state.themeMode.useDarkTheme()

            // Keep status/navigation bar icon colours in step with the in-app theme choice.
            DisposableEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            GeminiApiComposeStarterTheme(themeMode = state.themeMode) {
                ChatRoute(
                    viewModel = viewModel,
                    widthSizeClass = calculateWindowSizeClass(this).widthSizeClass,
                )
            }
        }
    }
}
