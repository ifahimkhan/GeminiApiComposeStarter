package com.fahim.geminiApiComposeStarter

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = ChatDatabase.getDatabase(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = BuildConfig.GEMINI_API_KEY),
            chatDao = database.chatDao(),
            settingsRepository = SettingsRepository(applicationContext),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.get(0)?.let { transcribedText ->
                viewModel.onPromptChange(transcribedText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatRoute(
                        viewModel = viewModel,
                        onVoiceInput = ::launchVoiceInput
                    )
                }
            }
        }
    }

    private fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizerLauncher.launch(intent)
    }
}
