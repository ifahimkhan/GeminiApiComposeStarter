package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.DatabaseProvider
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesDataStore
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ChatViewModel

    private var isViewModelReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeSecureApiKey()

        setContent {

            val darkMode by PreferencesDataStore
                .observeDarkMode(applicationContext)
                .collectAsStateWithLifecycle(
                    initialValue = false
                )

            GeminiApiComposeStarterTheme(
                darkTheme = darkMode
            ) {

                if (isViewModelReady) {

                    ChatRoute(
                        viewModel = viewModel,
                        darkMode = darkMode,
                        onDarkModeChange = { enabled ->

                            lifecycleScope.launch {

                                PreferencesDataStore.setDarkMode(
                                    applicationContext,
                                    enabled
                                )
                            }
                        }
                    )

                } else {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    private fun initializeSecureApiKey() {

        lifecycleScope.launch {

            val secureApiKeyManager =
                SecureApiKeyManager(applicationContext)

            /*
             * First launch:
             *
             * Read the API key from BuildConfig.
             * SecureApiKeyManager encrypts it using
             * AES-256-GCM and stores only ciphertext.
             */
            val buildConfigApiKey =
                BuildConfig.GEMINI_API_KEY.trim()

            if (buildConfigApiKey.isNotBlank()) {

                secureApiKeyManager.saveApiKey(
                    buildConfigApiKey
                )
            }

            /*
             * Retrieve the API key.
             *
             * The decrypted value exists in memory
             * and is supplied to the repository.
             */
            val secureApiKey =
                secureApiKeyManager.getApiKey()
                    .orEmpty()

            val database =
                DatabaseProvider.getDatabase(
                    applicationContext
                )

            val repository =
                GeminiRepositoryImpl(
                    apiKeyProvider = {
                        secureApiKey
                    }
                )

            val factory =
                ChatViewModel.factory(
                    repository = repository,
                    hasApiKey = secureApiKey.isNotBlank(),
                    chatMessageDao =
                        database.chatMessageDao()
                )

            viewModel =
                ViewModelProvider(
                    this@MainActivity,
                    factory
                )[ChatViewModel::class.java]

            isViewModelReady = true
        }
    }
}