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
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.ApiKeyVault
import com.fahim.geminiApiComposeStarter.data.AiProvider
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.DataStoreModelSelectionPreferences
import com.fahim.geminiApiComposeStarter.data.DataStoreThemePreferences
import com.fahim.geminiApiComposeStarter.data.ProviderChatRepository
import com.fahim.geminiApiComposeStarter.data.RoomChatHistory
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.N088Assignment1Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vault by lazy { ApiKeyVault(applicationContext) }
    private val configuredKeys by lazy {
        mapOf(
            AiProvider.GEMINI to BuildConfig.GEMINI_API_KEY,
            AiProvider.OPENAI to BuildConfig.OPENAI_API_KEY,
            AiProvider.DEEPSEEK to BuildConfig.DEEPSEEK_API_KEY
        )
    }
    private val database by lazy {
        Room.databaseBuilder(applicationContext, ChatDatabase::class.java, "chat_history.db").build()
    }
    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            ProviderChatRepository(vault),
            RoomChatHistory(database.messages()),
            DataStoreThemePreferences(applicationContext),
            DataStoreModelSelectionPreferences(applicationContext),
            configuredKeys.filterValues { it.isNotBlank() }.keys,
            vault.availableProviders
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) {
            configuredKeys.forEach { (provider, key) -> vault.storeOnFirstLaunch(provider, key) }
        }
        setContent {
            val themeModeFlow = remember(viewModel) {
                viewModel.uiState.map { it.themeMode }.distinctUntilChanged()
            }
            val themeMode by themeModeFlow.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeMode) {
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
