package com.fahim.geminiApiComposeStarter

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore
import com.fahim.geminiApiComposeStarter.data.UserPreferencesImpl

import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel

import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme


class MainActivity :
    ComponentActivity() {


    private val viewModel:
            ChatViewModel by viewModels {

        ChatViewModel.factory(

            repository =
                GeminiRepositoryImpl(

                    context =
                        applicationContext,

                    secureApiKeyStore =
                        SecureApiKeyStore(
                            applicationContext
                        )
                ),

            userPreferences =
                UserPreferencesImpl(
                    applicationContext
                ),

            hasApiKey =
                BuildConfig.GEMINI_API_KEY.isNotBlank()
        )
    }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContent {

            GeminiApiComposeStarterTheme {

                ChatScreen(
                    viewModel =
                        viewModel
                )
            }
        }
    }
}