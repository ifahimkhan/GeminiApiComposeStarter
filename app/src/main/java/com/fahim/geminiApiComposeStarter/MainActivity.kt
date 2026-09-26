package com.fahim.geminiApiComposeStarter

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.data.local.ChatDatabase
import com.fahim.geminiApiComposeStarter.security.ApiKeyVault
import com.fahim.geminiApiComposeStarter.ui.chat.*
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.*

class ChatApplication : Application() {
    private val vault by lazy { ApiKeyVault(this, { BuildConfig.GEMINI_API_KEY }, BuildConfig.GEMINI_KEY_VERSION) }
    val database by lazy { Room.databaseBuilder(this, ChatDatabase::class.java, "conversations.db").build() }
    val store by lazy { ConversationRepository(database) }
    val preferences by lazy { PreferencesRepository(this) }
    val gemini by lazy { GeminiRepositoryImpl(vault, BuildConfig.GEMINI_MODEL) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() { super.onCreate(); scope.launch { runCatching { vault.initialize() } } }
}
class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels {
        val graph = application as ChatApplication
        ChatViewModel.factory(graph.gemini, graph.store, graph.preferences)
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { GeminiApiComposeStarterTheme { ChatRoute(viewModel, calculateWindowSizeClass(this)) } }
    }
}
