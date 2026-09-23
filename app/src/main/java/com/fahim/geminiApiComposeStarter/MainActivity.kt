package com.fahim.geminiApiComposeStarter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.audio.TextToSpeechManager
import com.fahim.geminiApiComposeStarter.core.ai.AICore
import com.fahim.geminiApiComposeStarter.core.ai.IntentRouter
import com.fahim.geminiApiComposeStarter.core.ai.TaskPlanner
import com.fahim.geminiApiComposeStarter.core.context.ContextEngine
import com.fahim.geminiApiComposeStarter.core.memory.MemoryEngine
import com.fahim.geminiApiComposeStarter.core.tools.ToolRegistry
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.repository.KnowledgeRepository
import com.fahim.geminiApiComposeStarter.data.repository.UserMemoryRepository
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val secureApiKeyManager = SecureApiKeyManager()
    private lateinit var textToSpeechManager: TextToSpeechManager

    private val viewModel: ChatViewModel by viewModels {
        // On first launch, store the build-time API key securely in Android Keystore.
        // On subsequent launches, retrieve the encrypted key in-memory.
        val buildConfigKey = BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotBlank() && !secureApiKeyManager.hasStoredKey(this)) {
            secureApiKeyManager.storeApiKey(this, buildConfigKey)
        }

        val apiKey = secureApiKeyManager.retrieveApiKey(this) ?: buildConfigKey
        val hasKey = apiKey.isNotBlank()

        val db = AppDatabase.getInstance(this)
        val chatHistoryRepository = ChatHistoryRepository(
            messageDao = db.chatMessageDao(),
            conversationDao = db.conversationDao(),
            quizDao = db.quizAttemptDao(),
        )
        val knowledgeRepository = KnowledgeRepository(
            chatMessageDao = db.chatMessageDao(),
            flashcardDao = db.flashcardDao(),
            studyPlanDao = db.studyPlanDao(),
            weakTopicDao = db.weakTopicDao(),
        )
        val userMemoryRepository = UserMemoryRepository(
            memoryDao = db.userMemoryDao(),
        )
        val memoryEngine = MemoryEngine(
            memoryRepository = userMemoryRepository,
        )
        val geminiRepo = GeminiRepositoryImpl(apiKey = apiKey)
        val aiCore = AICore(
            intentRouter = IntentRouter(),
            memoryEngine = memoryEngine,
            taskPlanner = TaskPlanner(),
            toolRegistry = ToolRegistry(),
            contextEngine = ContextEngine(),
        )

        ChatViewModel.factory(
            repository = geminiRepo,
            chatHistoryRepository = chatHistoryRepository,
            hasApiKey = hasKey,
            knowledgeRepository = knowledgeRepository,
            userMemoryRepository = userMemoryRepository,
            aiCore = aiCore,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        textToSpeechManager = TextToSpeechManager(this)

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel,
                    textToSpeechManager = textToSpeechManager,
                    onShareContent = { text ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Study Note"))
                    },
                    onUpdateApiKey = { newKey ->
                        secureApiKeyManager.storeApiKey(this, newKey)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeechManager.shutdown()
    }
}
