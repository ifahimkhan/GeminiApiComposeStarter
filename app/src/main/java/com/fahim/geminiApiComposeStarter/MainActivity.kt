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
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.ApiKeyVault
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.DataStoreThemePreferences
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomChatHistory
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.N088Assignment1Theme

class MainActivity : ComponentActivity() {
    private val vault by lazy {
        ApiKeyVault(applicationContext).also { it.storeOnFirstLaunch(BuildConfig.GEMINI_API_KEY) }
    }
    private val database by lazy {
        Room.databaseBuilder(applicationContext, ChatDatabase::class.java, "chat_history.db").build()
    }
    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            GeminiRepositoryImpl(vault),
            RoomChatHistory(database.messages()),
            DataStoreThemePreferences(applicationContext),
            vault.hasKey()
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (state.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            N088Assignment1Theme(darkTheme = dark) {
                ChatRoute(viewModel, calculateWindowSizeClass(this).widthSizeClass)
            }
        }
    }
}
