package com.fahim.geminiApiComposeStarter

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.db.AppDatabase
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "chat-database").build()
    }

    private val viewModel: ChatViewModel by lazy {
        // Retrieve the API key - in production this would be from Keystore-encrypted storage.
        // For this assignment the key flows through BuildConfig (set from gitignored local.properties).
        val apiKey = BuildConfig.GEMINI_API_KEY
        val factory = ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            chatDao = db.chatDao(),
            hasApiKey = apiKey.isNotBlank(),
        )
        ViewModelProvider(this, factory)[ChatViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkThemePref by dataStore.data
                .map { it[DARK_MODE_KEY] }
                .collectAsState(initial = null)
            val useDarkTheme = isDarkThemePref ?: isSystemInDarkTheme()

            GeminiApiComposeStarterTheme(darkTheme = useDarkTheme) {
                ChatRoute(
                    viewModel = viewModel,
                    isDarkTheme = useDarkTheme,
                    onThemeToggle = {
                        lifecycleScope.launch {
                            dataStore.edit { prefs ->
                                prefs[DARK_MODE_KEY] = !useDarkTheme
                            }
                        }
                    }
                )
            }
        }
    }
}
