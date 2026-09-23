package com.example.n075nishantassgn1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.n075nishantassgn1.data.AppDatabase
import com.example.n075nishantassgn1.data.UserPreferencesRepository
import com.example.n075nishantassgn1.ui.chat.ChatScreen
import com.example.n075nishantassgn1.ui.chat.ChatViewModel
import com.example.n075nishantassgn1.ui.theme.N075nishantassgn1Theme
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        Firebase.initialize(this)

        // Enable Firebase App Check debug provider
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        val database = AppDatabase.getDatabase(this)

        val chatViewModel = ChatViewModel(
            historyDao = database.chatHistoryDao()
        )

        val preferencesRepository = UserPreferencesRepository(this)

        setContent {
            val darkMode by preferencesRepository.darkMode
                .collectAsStateWithLifecycle(
                    initialValue = false
                )
            val scope = rememberCoroutineScope()

            N075nishantassgn1Theme(
                darkTheme = darkMode
            ) {
                var messageText by remember {
                    mutableStateOf("")
                }

                ChatScreen(
                    viewModel = chatViewModel,
                    messageText = messageText,
                    onMessageTextChange = {
                        messageText = it
                    },
                    isDarkMode = darkMode,
                    onToggleDarkMode = { enabled ->
                        scope.launch {
                            preferencesRepository.setDarkMode(enabled)
                        }
                    }
                )
            }
        }
    }
}