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
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.local.room.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var chatDatabase: ChatDatabase

    private val viewModel: ChatViewModel by viewModels {
        apiKeyManager = ApiKeyManager()
        preferencesRepository = UserPreferencesRepository(applicationContext)
        chatDatabase = ChatDatabase.getInstance(applicationContext)

        // Seed encrypted key into DataStore on first launch if build-time key exists
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotBlank()) {
            lifecycleScope.launch {
                val currentPrefs = preferencesRepository.userPreferencesFlow.first()
                if (currentPrefs.encryptedApiKey.isNullOrEmpty()) {
                    val (cipherText, iv) = apiKeyManager.encrypt(buildKey)
                    preferencesRepository.saveEncryptedApiKey(cipherText, iv)
                }
            }
        }

        // In-memory key provider: Decrypts only at the moment GenerativeModel is instantiated
        val inMemoryKeyProvider: () -> String = {
            try {
                val prefs = runBlocking { preferencesRepository.userPreferencesFlow.first() }
                if (!prefs.encryptedApiKey.isNullOrEmpty() && !prefs.encryptedApiKeyIv.isNullOrEmpty()) {
                    apiKeyManager.decrypt(prefs.encryptedApiKey, prefs.encryptedApiKeyIv)
                } else {
                    buildKey
                }
            } catch (e: Exception) {
                buildKey
            }
        }

        val geminiRepo = GeminiRepositoryImpl(apiKeyProvider = inMemoryKeyProvider)
        val chatRepo = ChatRepositoryImpl(
            geminiRepository = geminiRepo,
            chatDao = chatDatabase.chatDao(),
        )

        ChatViewModel.factory(
            repository = chatRepo,
            hasApiKey = buildKey.isNotBlank(),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val preferences by preferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.fahim.geminiApiComposeStarter.data.local.preferences.UserPreferences()
            )

            GeminiApiComposeStarterTheme(
                darkTheme = preferences.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme(),
                dynamicColor = preferences.dynamicColorEnabled,
            ) {
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
