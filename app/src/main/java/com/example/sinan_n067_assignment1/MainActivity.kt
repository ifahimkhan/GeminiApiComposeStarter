package com.example.sinan_n067_assignment1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sinan_n067_assignment1.ui.theme.Sinan_N067_ASSIGNMENT1Theme

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Sinan_N067_ASSIGNMENT1Theme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()

                    ChatScreen(
                        uiState = uiState,
                        onSendMessage = chatViewModel::sendMessage,
                        onDismissError = chatViewModel::dismissError
                    )
                }
            }
        }
    }
}
