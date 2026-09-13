package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStore
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val preferencesRepository by lazy {
        PreferencesRepository(this)
    }

    private val viewModel: ChatViewModel by viewModels {

        val database = AppDatabase.getInstance(this)

        val historyRepository = ChatHistoryRepository(
            dao = database.chatMessageDao()
        )

        val apiKeyStore = ApiKeyStore(this)

        val geminiRepository = GeminiRepositoryImpl(
            apiKeyStore = apiKeyStore
        )

        ChatViewModel.factory(
            repository = geminiRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            historyRepository = historyRepository,
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val windowSizeClass =   calculateWindowSizeClass(this)

            val darkMode =
                preferencesRepository.darkMode
                    .collectAsStateWithLifecycle(
                        initialValue = false
                    )

            val dynamicColors =
                preferencesRepository.dynamicColors
                    .collectAsStateWithLifecycle(
                        initialValue = true
                    )

            GeminiApiComposeStarterTheme(
                darkTheme = darkMode.value,
                dynamicColor = dynamicColors.value,
            ) {

                ChatRoute(
                    viewModel = viewModel,

                    windowSizeClass = windowSizeClass,

                    darkMode = darkMode.value,

                    dynamicColors = dynamicColors.value,

                    onDarkModeChange = { enabled ->

                        lifecycleScope.launch {
                            preferencesRepository
                                .setDarkMode(enabled)
                        }
                    },

                    onDynamicColorsChange = { enabled ->

                        lifecycleScope.launch {
                            preferencesRepository
                                .setDynamicColors(enabled)
                        }
                    },
                )
            }
        }
    }
}