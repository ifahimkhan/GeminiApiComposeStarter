package com.fahim.geminiApiComposeStarter


import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import androidx.lifecycle.lifecycleScope
import com.fahim.geminiApiComposeStarter.data.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "gemini_chat_database"
        ).build()
    }

    private val viewModel: ChatViewModel by viewModels {

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                context = applicationContext,
                fallbackApiKey = BuildConfig.GEMINI_API_KEY,
            ),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            chatMessageDao = database.chatMessageDao(),
        )


    }

    private val userPreferences by lazy {
        UserPreferences(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val dynamicColor = userPreferences.dynamicColor.first()

            setContent {
                GeminiApiComposeStarterTheme(
                    dynamicColor = dynamicColor
                ) {
                    val windowSizeClass = calculateWindowSizeClass(this@MainActivity)

                    ChatRoute(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                        userPreferences = userPreferences
                    )
                }
            }
        }
    }
}
