package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.PreferencesManager
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {

        /*
         * ------------------------------------------------------
         * API KEY
         * ------------------------------------------------------
         */

        val apiKeyManager =
            ApiKeyManager(applicationContext)

        if (apiKeyManager.getApiKey().isBlank()) {

            apiKeyManager.saveApiKey(
                BuildConfig.GEMINI_API_KEY
            )
        }

        val apiKey =
            apiKeyManager.getApiKey()

        /*
         * ------------------------------------------------------
         * ROOM DATABASE
         * ------------------------------------------------------
         */

        val database =
            ChatDatabase.getInstance(
                applicationContext
            )

        val historyRepository =
            ChatHistoryRepository(

                messageDao =
                    database.chatMessageDao(),

                conversationDao =
                    database.conversationDao()
            )

        /*
         * ------------------------------------------------------
         * VIEW MODEL
         * ------------------------------------------------------
         */

        ChatViewModel.factory(

            repository =
                GeminiRepositoryImpl(
                    apiKey = apiKey
                ),

            hasApiKey =
                apiKey.isNotBlank(),

            historyRepository =
                historyRepository
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            /*
             * --------------------------------------------------
             * PREFERENCES / DATASTORE
             * --------------------------------------------------
             */

            val preferencesManager =
                remember {

                    PreferencesManager(
                        applicationContext
                    )
                }

            /*
             * Get saved Dark Mode value
             */

            val isDarkMode by
            preferencesManager
                .isDarkMode
                .collectAsStateWithLifecycle(
                    initialValue = false
                )

            /*
             * Coroutine scope for saving settings
             */

            val scope =
                rememberCoroutineScope()

            /*
             * --------------------------------------------------
             * THEME
             * --------------------------------------------------
             */

            GeminiApiComposeStarterTheme(

                darkTheme =
                    isDarkMode

            ) {

                /*
                 * ------------------------------------------------
                 * CHAT
                 * ------------------------------------------------
                 */

                ChatRoute(

                    viewModel =
                        viewModel,

                    isDarkMode =
                        isDarkMode,

                    onDarkModeChange = { enabled ->

                        scope.launch {

                            preferencesManager
                                .setDarkMode(enabled)
                        }
                    }
                )
            }
        }
    }
}