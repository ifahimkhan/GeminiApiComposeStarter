package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.AppDatabase
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.ApiKeyVault
import com.fahim.geminiApiComposeStarter.data.AppPreferences
import com.fahim.geminiApiComposeStarter.data.FileChatStorage
import com.fahim.geminiApiComposeStarter.data.GeneratedImageStore
import com.fahim.geminiApiComposeStarter.data.RoomChatStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val vault = ApiKeyVault(applicationContext)
        val preferences = AppPreferences(applicationContext)

        // Auto-seed the vault with the build-time API key if the vault is empty.
        val buildTimeKey = BuildConfig.GEMINI_API_KEY
        if (buildTimeKey.isNotBlank()) {
            runBlocking {
                val stored = vault.read()
                if (stored.isBlank()) vault.save(buildTimeKey)
            }
        }

        // Room database as the primary storage.
        val db = AppDatabase.getInstance(applicationContext)
        val roomStorage = RoomChatStorage(db)

        // One-time migration: if the old JSON file exists, load it and seed Room, then delete it.
        runBlocking {
            val legacyFile = File(applicationContext.noBackupFilesDir, "conversations.json")
            if (legacyFile.exists()) {
                try {
                    val legacyStorage = FileChatStorage(applicationContext)
                    val legacyConversations = legacyStorage.load()
                    if (legacyConversations.isNotEmpty()) {
                        roomStorage.save(legacyConversations)
                    }
                } catch (_: Exception) { /* ignore migration failures */ } finally {
                    legacyFile.delete()
                }
            }
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(applicationContext, apiKey = { vault.read() },
                modelName = { runBlocking { preferences.values.first().modelName } }),
            hasApiKey = buildTimeKey.isNotBlank() || runBlocking { vault.read().isNotBlank() },
            storage = roomStorage,
            preferences = preferences,
            vault = vault,
            imageStore = GeneratedImageStore(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always dark status bar / nav bar to match our default dark theme.
        // The system bars won't visually conflict with either mode since both use transparent bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            // ChatRoute manages its own GeminiApiComposeStarterTheme driven by state.darkMode.
            // No hardcoded darkTheme here — the ViewModel preference controls it.
            ChatRoute(viewModel = viewModel)
        }
    }
}

