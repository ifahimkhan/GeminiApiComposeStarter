package com.fahim.geminiApiComposeStarter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.SecureKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.components.VoiceInputHelper
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureStorage by lazy {
        SecureKeyStorage(applicationContext).apply {
            initializeIfNeeded(BuildConfig.GEMINI_API_KEY)
        }
    }

    private val database by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val userPreferences by lazy {
        UserPreferencesRepository(applicationContext)
    }

    private val viewModel: ChatViewModel by viewModels {
        val decryptedKey = secureStorage.getDecryptedApiKey().ifBlank { BuildConfig.GEMINI_API_KEY }
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = decryptedKey),
            chatDao = database,
            userPreferences = userPreferences,
            hasApiKey = decryptedKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(darkTheme = state.isDarkTheme) {
                val voiceLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val spokenText = result.data
                            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            ?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            viewModel.onPromptChange(spokenText)
                        }
                    }
                }

                ChatRoute(
                    viewModel = viewModel,
                    onVoiceInputClick = {
                        val intent = VoiceInputHelper.createSpeechIntent(this@MainActivity)
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Speech recognition is not available on this device.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}
