package com.example.c210_utkarshtanna_mad_assignment1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.c210_utkarshtanna_mad_assignment1.ui.ChatScreen
import com.example.c210_utkarshtanna_mad_assignment1.ui.theme.ChatAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChatAppTheme {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
