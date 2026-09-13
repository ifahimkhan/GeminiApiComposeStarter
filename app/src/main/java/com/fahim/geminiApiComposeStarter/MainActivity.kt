package com.fahim.geminiApiComposeStarter

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Room Database
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "gemini-db"
        ).fallbackToDestructiveMigration().build()
        val chatDao = db.chatDao()

        // 2. Encryption at Rest (Assignment Step 3)
        val rawApiKey = BuildConfig.GEMINI_API_KEY
        val prefs = getSharedPreferences("secure_settings", Context.MODE_PRIVATE)

        if (!prefs.contains("encrypted_gemini_key") && rawApiKey.isNotBlank()) {
            val encrypted = SecurityManager.encrypt(rawApiKey)
            prefs.edit().putString("encrypted_gemini_key", encrypted).apply()
        }

        // Decrypt in memory only when configuring the repository
        val encryptedKey = prefs.getString("encrypted_gemini_key", "") ?: ""
        val finalKey = if (encryptedKey.isNotBlank()) {
            SecurityManager.decrypt(encryptedKey)
        } else {
            rawApiKey
        }

        // 3. Initialize ViewModel cleanly
        val viewModel = ViewModelProvider(
            this,
            ChatViewModel.factory(
                repository = GeminiRepositoryImpl(apiKey = finalKey),
                chatDao = chatDao,
                hasApiKey = finalKey.isNotBlank()
            )
        )[ChatViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}