package com.example.assignment1_c061

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.assignment1_c061.data.GeminiRepositoryImpl
import com.example.assignment1_c061.data.db.ChatDatabase
import com.example.assignment1_c061.data.preferences.UserPreferencesRepository
import com.example.assignment1_c061.data.security.SecureKeyStorage
import com.example.assignment1_c061.ui.chat.ChatRoute
import com.example.assignment1_c061.ui.chat.ChatViewModel
import com.example.assignment1_c061.ui.theme.Assignment1C061Theme

class MainActivity : ComponentActivity() {

    private lateinit var secureKeyStorage: SecureKeyStorage
    private lateinit var database: ChatDatabase
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKeyProvider = { secureKeyStorage.getDecryptedApiKey().ifBlank { BuildConfig.GEMINI_API_KEY } }
            ),
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank() || secureKeyStorage.getDecryptedApiKey().isNotBlank(),
            messageDao = database.chatMessageDao(),
            userPreferencesRepository = userPreferencesRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureKeyStorage = SecureKeyStorage(applicationContext)
        database = ChatDatabase.getDatabase(applicationContext)
        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            secureKeyStorage.saveEncryptedApiKey(BuildConfig.GEMINI_API_KEY)
        }

        enableEdgeToEdge()
        setContent {
            Assignment1C061Theme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
