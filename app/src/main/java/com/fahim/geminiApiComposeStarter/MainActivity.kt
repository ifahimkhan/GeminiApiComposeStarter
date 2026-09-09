package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ChatViewModel

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val secureApiKeyStore = SecureApiKeyStore(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)

        lifecycleScope.launch {
            val buildTimeApiKey = BuildConfig.GEMINI_API_KEY.trim()

            if (buildTimeApiKey.isNotBlank()) {
                secureApiKeyStore.saveApiKey(buildTimeApiKey)
            }

            val apiKey = secureApiKeyStore.getApiKey()

            Log.d(
                "GeminiDebug",
                "API key loaded: ${!apiKey.isNullOrBlank()}, length=${apiKey?.length ?: 0}"
            )

            val repository = GeminiRepositoryImpl(
                apiKey = apiKey.orEmpty()
            )

            viewModel = ViewModelProvider(
                this@MainActivity,
                ChatViewModel.factory(
                    repository = repository,
                    chatMessageDao = database.chatMessageDao(),
                    hasApiKey = !apiKey.isNullOrBlank()
                )
            )[ChatViewModel::class.java]

            setContent {
                GeminiApiComposeStarterTheme {
                    val windowSizeClass = calculateWindowSizeClass(this@MainActivity)

                    ChatRoute(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                    )
                }
            }
        }
    }
}