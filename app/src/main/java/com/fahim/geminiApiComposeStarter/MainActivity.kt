package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            this,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKey = sharedPreferences.getString("api_key", "") ?: ""
            ),
            hasApiKey = sharedPreferences.contains("api_key")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!sharedPreferences.contains("api_key")) {
            sharedPreferences.edit()
                .putString("api_key", BuildConfig.GEMINI_API_KEY)
                .apply()
        }

        enableEdgeToEdge()
        setContent {
            val isDarkTheme = sharedPreferences.getBoolean("is_dark_theme", false)
            val darkThemeState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(isDarkTheme) }
            
            GeminiApiComposeStarterTheme(darkTheme = darkThemeState.value) {
                ChatRoute(
                    viewModel = viewModel,
                    isDarkTheme = darkThemeState.value,
                    onThemeToggle = {
                        val newValue = !darkThemeState.value
                        sharedPreferences.edit().putBoolean("is_dark_theme", newValue).apply()
                        darkThemeState.value = newValue
                    }
                )
            }
        }
    }
}