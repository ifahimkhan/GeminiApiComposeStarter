package com.fahim.geminiApiComposeStarter

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository

import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel

import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme


class MainActivity : ComponentActivity() {

    // Secure API Key Storage
    private val secureApiKeyStorage by lazy {
        SecureApiKeyStorage(applicationContext)
    }

    // DataStore Preferences
    private val preferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    // Room Database
    private val chatDao by lazy {
        ChatDatabase
            .getInstance(applicationContext)
            .chatDao()
    }

    // Retrieve API key from encrypted storage
    private val apiKey: String by lazy {

        val storedKey = secureApiKeyStorage.getApiKey()

        if (!storedKey.isNullOrBlank()) {
            storedKey
        } else {

            val initialKey = BuildConfig.GEMINI_API_KEY

            if (initialKey.isNotBlank()) {
                secureApiKeyStorage.saveApiKey(initialKey)
            }

            initialKey
        }
    }

    // Chat ViewModel
    private val viewModel: ChatViewModel by viewModels {

        ChatViewModel.factory(

            repository = GeminiRepositoryImpl(
                apiKey = apiKey
            ),

            preferencesRepository = preferencesRepository,

            chatDao = chatDao,

            hasApiKey = apiKey.isNotBlank()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            // Get system theme OUTSIDE rememberSaveable
            val systemDarkTheme = isSystemInDarkTheme()

            // Store current theme choice
            var isDarkTheme by rememberSaveable {
                mutableStateOf(systemDarkTheme)
            }

            GeminiApiComposeStarterTheme(
                darkTheme = isDarkTheme
            ) {

                ChatRoute(
                    viewModel = viewModel,

                    isDarkTheme = isDarkTheme,

                    onThemeToggle = {
                        isDarkTheme = !isDarkTheme
                    }
                )
            }
        }
    }
}