package com.nmims.c010.gemini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nmims.c010.gemini.ui.chat.ChatRoute
import com.nmims.c010.gemini.ui.chat.ChatViewModel
import com.nmims.c010.gemini.ui.theme.GeminiTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GeminiTheme {
                val vm: ChatViewModel = viewModel(
                    factory = ChatViewModel.Factory(applicationContext)
                )
                ChatRoute(
                    viewModel = vm,
                    widthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
