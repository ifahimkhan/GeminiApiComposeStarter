package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyVault
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.chat.*
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {
    private val vault by lazy { ApiKeyVault(applicationContext) }
    private val viewModel: ChatViewModel by viewModels {
        val keyVault = vault
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKeyProvider = {
                keyVault.initialize(BuildConfig.GEMINI_API_KEY)
                keyVault.decrypt()
            }),
            store = RoomChatStore(ChatDatabase.getDatabase(applicationContext).chatDao()),
            preferences = DataStoreUserPreferences(applicationContext)
        )
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) {
            try { vault.initialize(BuildConfig.GEMINI_API_KEY) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Sending will surface a safe, actionable error. */ }
        }
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = uiState.darkModeOverride ?: systemDark
            GeminiApiComposeStarterTheme(darkTheme = isDark) {
                ChatRoute(viewModel, calculateWindowSizeClass(this).widthSizeClass)
            }
        }
    }
}
