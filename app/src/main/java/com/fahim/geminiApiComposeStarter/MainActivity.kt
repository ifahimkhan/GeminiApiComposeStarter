package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecurityManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val chatMessageDao = database.chatMessageDao()
        preferencesRepository = PreferencesRepositoryImpl(applicationContext)

        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildConfigKey.isNotEmpty()) {
            val encrypted = SecurityManager.encrypt(buildConfigKey)
            runBlocking {
                preferencesRepository.saveEncryptedApiKey(encrypted.ciphertextBase64, encrypted.ivBase64)
            }
        }

        fun getDecryptedApiKey(): String {
            val encryptedResult = runBlocking { preferencesRepository.encryptedApiKey.firstOrNull() }
            val decrypted = if (encryptedResult != null) {
                SecurityManager.decrypt(encryptedResult)
            } else {
                ""
            }
            return decrypted.ifBlank { buildConfigKey }
        }

        val key = getDecryptedApiKey()

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(getApiKey = { getDecryptedApiKey() }),
            chatMessageDao = chatMessageDao,
            preferencesRepository = preferencesRepository,
            hasApiKey = key.isNotBlank()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(themePreference = state.themePreference) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
