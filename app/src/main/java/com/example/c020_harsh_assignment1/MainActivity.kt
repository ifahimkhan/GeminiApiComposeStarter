package com.example.c020_harsh_assignment1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.c020_harsh_assignment1.data.PreferencesRepository
import com.example.c020_harsh_assignment1.data.PreferencesRepositoryImpl
import com.example.c020_harsh_assignment1.data.local.ChatDatabase
import com.example.c020_harsh_assignment1.security.CryptoManager
import com.example.c020_harsh_assignment1.security.CryptoManagerImpl
import com.example.c020_harsh_assignment1.ui.chat.ChatRoute
import com.example.c020_harsh_assignment1.ui.chat.ChatViewModel
import com.example.c020_harsh_assignment1.ui.theme.C020_Harsh_Assignment1Theme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ChatDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()
        val preferencesRepository = PreferencesRepositoryImpl(applicationContext)
        val cryptoManager = CryptoManagerImpl()

        val viewModel: ChatViewModel by viewModels {
            ChatViewModel.factory(
                chatDao = chatDao,
                preferencesRepository = preferencesRepository,
                cryptoManager = cryptoManager,
                buildConfigApiKey = BuildConfig.GEMINI_API_KEY
            )
        }

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            C020_Harsh_Assignment1Theme {
                ChatRoute(
                    viewModel = viewModel,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
