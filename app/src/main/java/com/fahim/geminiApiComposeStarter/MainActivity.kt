package com.fahim.geminiApiComposeStarter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.security.SecureKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
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

    private val viewModel: ChatViewModel by viewModels {
        val decryptedKey = secureStorage.getDecryptedApiKey().ifBlank { BuildConfig.GEMINI_API_KEY }
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = decryptedKey),
            chatDao = database,
            hasApiKey = decryptedKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
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
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Device without speech recognizer service
                        }
                    }
                )
            }
        }
    }
}
