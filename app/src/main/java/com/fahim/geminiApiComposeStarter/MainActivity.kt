package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.db.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import com.fahim.geminiApiComposeStarter.security.KeystoreEncryption
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        // Decrypt in memory — result never stored in a field or logged
        val apiKey = KeystoreEncryption.getDecryptedApiKey(applicationContext)
        val dao = ChatDatabase.getInstance(applicationContext).chatDao()
        val prefs = UserPreferences(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(apiKey = apiKey, chatDao = dao),
            userPreferences = prefs,
            hasApiKey = apiKey.isNotBlank(),
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
