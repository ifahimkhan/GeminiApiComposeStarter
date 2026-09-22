package com.vaishnavi.n085.geminichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vaishnavi.n085.geminichat.data.ApiKeyStore
import com.vaishnavi.n085.geminichat.data.GeminiRepositoryImpl
import com.vaishnavi.n085.geminichat.data.local.AppDatabase
import com.vaishnavi.n085.geminichat.data.prefs.DataStoreAppPreferences
import com.vaishnavi.n085.geminichat.ui.chat.ChatRoute
import com.vaishnavi.n085.geminichat.ui.chat.ChatViewModel
import com.vaishnavi.n085.geminichat.ui.theme.VaishnaviChatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val preferences = DataStoreAppPreferences(applicationContext)
        val apiKeyStore = ApiKeyStore(preferences)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyProvider = { apiKeyStore.getOrStoreApiKey(BuildConfig.GEMINI_API_KEY) },
            ),
            chatDao = AppDatabase.getInstance(applicationContext).chatDao(),
            preferences = preferences,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaishnaviChatTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
