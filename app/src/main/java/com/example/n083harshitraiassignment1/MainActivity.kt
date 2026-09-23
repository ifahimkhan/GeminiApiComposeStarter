package com.example.n083harshitraiassignment1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.n083harshitraiassignment1.ui.chat.ChatScreen
import com.example.n083harshitraiassignment1.ui.chat.ChatViewModel
import com.example.n083harshitraiassignment1.ui.theme.N083HarshitRaiAssignment1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        setContent {

            N083HarshitRaiAssignment1Theme {

                val viewModel: ChatViewModel =
                    viewModel()

                ChatScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}