package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureKeyStore by lazy { SecureApiKeyStore(applicationContext) }
    private val chatDatabase by lazy { ChatDatabase.getInstance(applicationContext) }

    private val viewModel: ChatViewModel by viewModels {
        val activeKey = secureKeyStore.resolveApiKeyInMemory()
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                keyProvider = { secureKeyStore.resolveApiKeyInMemory() },
                messageDao = chatDatabase.messageDao(),
            ),
            hasApiKey = activeKey.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureKeyStore.ensureKeyEncrypted()
        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
