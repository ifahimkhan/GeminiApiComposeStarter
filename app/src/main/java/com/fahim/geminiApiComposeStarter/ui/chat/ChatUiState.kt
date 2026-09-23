package com.fahim.geminiApiComposeStarter.ui.chat

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.data.local.FlashcardEntity
import com.fahim.geminiApiComposeStarter.data.local.StudyPlanEntity
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import com.fahim.geminiApiComposeStarter.data.local.WeakTopicEntity
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.Conversation
import com.fahim.geminiApiComposeStarter.model.QuizAttempt
import com.fahim.geminiApiComposeStarter.model.StudyMode
import com.fahim.geminiApiComposeStarter.ui.navigation.NavigationDestination
import com.fahim.geminiApiComposeStarter.ui.voice.VoiceState

/**
 * Immutable UI state for the Gemini AI Study Workspace.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
    val activeMode: StudyMode = StudyMode.CHAT,
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String = "default",
    val currentConversationTitle: String = "Study Workspace",
    val savedNotes: List<ChatMessage> = emptyList(),
    val selectedImageBitmap: Bitmap? = null,
    val selectedImageUri: String? = null,
    val speakingMessageId: String? = null,
    val quizAttempts: List<QuizAttempt> = emptyList(),
    val showModeDialog: Boolean = false,
    val showSavedNotesDialog: Boolean = false,
    val showInsightsDialog: Boolean = false,

    // Navigation & Workspace Sections
    val selectedNavigation: NavigationDestination = NavigationDestination.DASHBOARD,

    // Streaming
    val isStreamingEnabled: Boolean = true,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",

    // Voice Assistant
    val showLiveVoiceDialog: Boolean = false,
    val voiceState: VoiceState = VoiceState.IDLE,
    val isHandsFreeVoice: Boolean = false,

    // Document attachments
    val attachedDocName: String? = null,
    val attachedDocText: String? = null,

    // Persistent Knowledge & Memory Entities
    val flashcards: List<FlashcardEntity> = emptyList(),
    val studyPlans: List<StudyPlanEntity> = emptyList(),
    val weakTopics: List<WeakTopicEntity> = emptyList(),
    val userMemories: List<UserMemoryEntity> = emptyList(),
)

enum class PromptError { EMPTY }
