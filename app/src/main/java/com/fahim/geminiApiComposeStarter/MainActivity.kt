package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val secureStore = SecureApiKeyStore(applicationContext)

        if (!secureStore.hasStoredKey() && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            secureStore.saveApiKey(BuildConfig.GEMINI_API_KEY)
        }

        val apiKey = secureStore.getApiKey().orEmpty()
        val dao = AppDatabase.getInstance(applicationContext).chatMessageDao()

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey),
            hasApiKey = apiKey.isNotBlank(),
            dao = dao,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}