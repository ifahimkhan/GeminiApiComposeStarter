package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.PitwallDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyEncryptionManager
import com.fahim.geminiApiComposeStarter.data.local.ApiKeyDataStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val encryptionManager = ApiKeyEncryptionManager()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawApiKey = BuildConfig.GEMINI_API_KEY
        val dataStore = ApiKeyDataStore(this)
        val database = PitwallDatabase.getDatabase(this)
        val chatDao = database.chatDao()

        // Encrypt and store the key on first run, then decrypt in memory for use
        val decryptedKey = if (rawApiKey.isNotBlank()) {
            // Encrypt if not yet stored
            CoroutineScope(Dispatchers.IO).launch {
                val existing = dataStore.encryptedApiKeyFlow.first()
                if (existing.first.isBlank()) {
                    val (ct, iv) = encryptionManager.encryptApiKey(rawApiKey)
                    dataStore.saveEncryptedApiKey(ct, iv)
                }
            }
            rawApiKey // Use the BuildConfig key in memory for current session
        } else {
            // Try to decrypt from DataStore
            val stored = runBlocking { dataStore.encryptedApiKeyFlow.first() }
            if (stored.first.isNotBlank()) {
                encryptionManager.decryptApiKey(stored.first, stored.second)
            } else {
                ""
            }
        }

        val viewModel: ChatViewModel by viewModels {
            ChatViewModel.factory(
                repository = GeminiRepositoryImpl(
                    apiKey = decryptedKey,
                    chatDao = chatDao
                ),
                hasApiKey = decryptedKey.isNotBlank(),
            )
        }

        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
