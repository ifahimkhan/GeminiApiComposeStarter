package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.ApiKeyVault
import com.fahim.geminiApiComposeStarter.data.AppPreferences
import com.fahim.geminiApiComposeStarter.data.FileChatStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val vault = ApiKeyVault(applicationContext)
        val preferences = AppPreferences(applicationContext)

        // Auto-seed the vault with the build-time API key if the vault is empty.
        val buildTimeKey = BuildConfig.GEMINI_API_KEY
        if (buildTimeKey.isNotBlank()) {
            runBlocking {
                val stored = vault.read()
                if (stored.isBlank()) vault.save(buildTimeKey)
            }
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = { vault.read() },
                modelName = { runBlocking { preferences.values.first().modelName } }),
            hasApiKey = buildTimeKey.isNotBlank() || runBlocking { vault.read().isNotBlank() },
            storage = FileChatStorage(applicationContext),
            preferences = preferences,
            vault = vault,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            GeminiApiComposeStarterTheme(darkTheme = true, dynamicColor = false) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
