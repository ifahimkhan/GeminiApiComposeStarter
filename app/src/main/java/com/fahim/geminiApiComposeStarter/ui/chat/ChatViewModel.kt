package com.fahim.geminiApiComposeStarter.ui.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.core.ai.AICore
import com.fahim.geminiApiComposeStarter.core.ai.AICoreResponse
import com.fahim.geminiApiComposeStarter.core.ai.ResponseValidator
import com.fahim.geminiApiComposeStarter.core.context.ContextEngine
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.FlashcardEntity
import com.fahim.geminiApiComposeStarter.data.local.StudyPlanEntity
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import com.fahim.geminiApiComposeStarter.data.local.WeakTopicEntity
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.repository.KnowledgeRepository
import com.fahim.geminiApiComposeStarter.data.repository.UserMemoryRepository
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.model.QuizAttempt
import com.fahim.geminiApiComposeStarter.model.StudyMode
import com.fahim.geminiApiComposeStarter.ui.navigation.NavigationDestination
import com.fahim.geminiApiComposeStarter.ui.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository?,
    private val hasApiKey: Boolean,
    private val contextEngine: ContextEngine = ContextEngine(),
    private val knowledgeRepository: KnowledgeRepository? = null,
    private val userMemoryRepository: UserMemoryRepository? = null,
    private val aiCore: AICore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageCollectionJob: Job? = null

    init {
        observeSessions()
        observeSavedNotes()
        observeQuizAttempts()
        observeKnowledge()
        observeMemories()
        loadConversationMessages("default")
    }

    private fun observeSessions() {
        chatHistoryRepository ?: return
        viewModelScope.launch {
            chatHistoryRepository.getAllConversations().collect { sessions ->
                _uiState.update { state ->
                    val currentTitle = sessions.firstOrNull { it.id == state.currentConversationId }?.title
                        ?: state.currentConversationTitle
                    state.copy(
                        conversations = sessions,
                        currentConversationTitle = currentTitle,
                    )
                }
            }
        }
    }

    private fun observeSavedNotes() {
        chatHistoryRepository ?: return
        viewModelScope.launch {
            chatHistoryRepository.getSavedMessages().collect { saved ->
                _uiState.update { it.copy(savedNotes = saved) }
            }
        }
    }

    private fun observeQuizAttempts() {
        chatHistoryRepository ?: return
        viewModelScope.launch {
            chatHistoryRepository.getAllQuizAttempts().collect { attempts ->
                _uiState.update { it.copy(quizAttempts = attempts) }
            }
        }
    }

    private fun observeKnowledge() {
        knowledgeRepository ?: return
        viewModelScope.launch {
            knowledgeRepository.allFlashcards.collect { cards ->
                _uiState.update { it.copy(flashcards = cards) }
            }
        }
        viewModelScope.launch {
            knowledgeRepository.allPlans.collect { plans ->
                _uiState.update { it.copy(studyPlans = plans) }
            }
        }
        viewModelScope.launch {
            knowledgeRepository.weakTopics.collect { topics ->
                _uiState.update { it.copy(weakTopics = topics) }
            }
        }
    }

    private fun observeMemories() {
        userMemoryRepository ?: return
        viewModelScope.launch {
            userMemoryRepository.allMemories.collect { mems ->
                _uiState.update { it.copy(userMemories = mems) }
            }
        }
    }

    fun loadConversationMessages(conversationId: String) {
        messageCollectionJob?.cancel()
        _uiState.update { it.copy(currentConversationId = conversationId) }

        chatHistoryRepository ?: return
        messageCollectionJob = viewModelScope.launch {
            chatHistoryRepository.getMessagesForConversation(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun setNavigation(destination: NavigationDestination) {
        _uiState.update { it.copy(selectedNavigation = destination) }
    }

    // ── Prompt & Inputs ──────────────────────────────────────────────────────

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun setMode(mode: StudyMode) {
        _uiState.update { it.copy(activeMode = mode, showModeDialog = false) }
    }

    fun onImageSelected(bitmap: Bitmap?, uriString: String?) {
        _uiState.update {
            it.copy(
                selectedImageBitmap = bitmap,
                selectedImageUri = uriString,
                activeMode = if (bitmap != null) StudyMode.IMAGE_ANALYSIS else it.activeMode,
            )
        }
    }

    fun onClearImage() {
        _uiState.update {
            it.copy(
                selectedImageBitmap = null,
                selectedImageUri = null,
                activeMode = if (it.activeMode == StudyMode.IMAGE_ANALYSIS) StudyMode.CHAT else it.activeMode,
            )
        }
    }

    fun onDocumentAttached(fileName: String, content: String) {
        _uiState.update {
            it.copy(
                attachedDocName = fileName,
                attachedDocText = content,
            )
        }
    }

    fun onClearDocument() {
        _uiState.update {
            it.copy(
                attachedDocName = null,
                attachedDocText = null,
            )
        }
    }

    fun onQuickAction(actionPrompt: String) {
        _uiState.update { it.copy(prompt = actionPrompt) }
        onSend()
    }

    // ── Core AI Message Generation ───────────────────────────────────────────

    fun onSend() {
        val currentPrompt = _uiState.value.prompt.trim()
        val attachedBitmap = _uiState.value.selectedImageBitmap
        val attachedUri = _uiState.value.selectedImageUri
        val attachedDocText = _uiState.value.attachedDocText

        if (currentPrompt.isEmpty() && attachedBitmap == null && attachedDocText == null) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val conversationId = _uiState.value.currentConversationId
        val activeMode = _uiState.value.activeMode

        val userMessage = ChatMessage(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = currentPrompt.ifBlank {
                if (attachedBitmap != null) "Analyze this image" else "Analyze this document"
            },
            imageUri = attachedUri,
        )

        // Clear prompt and attachments, set loading
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                prompt = "",
                selectedImageBitmap = null,
                selectedImageUri = null,
                attachedDocName = null,
                attachedDocText = null,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            chatHistoryRepository?.insertMessage(userMessage)

            val currentHistory = _uiState.value.messages.dropLast(1)

            // When AICore is present, route through intelligent orchestration
            if (aiCore != null) {
                val coreResult = aiCore.execute(
                    prompt = userMessage.content,
                    history = currentHistory,
                    mode = activeMode,
                    attachedBitmap = attachedBitmap,
                    attachedDocText = attachedDocText,
                    savedNotes = _uiState.value.savedNotes,
                    geminiRepository = repository,
                )

                when (coreResult) {
                    is AICoreResponse.Direct -> {
                        val geminiMessage = ChatMessage(
                            conversationId = conversationId,
                            role = MessageRole.GEMINI,
                            content = coreResult.message,
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + geminiMessage,
                                isLoading = false,
                            )
                        }
                        chatHistoryRepository?.insertMessage(geminiMessage)
                        maybeAutoTitle(conversationId)
                    }
                    is AICoreResponse.Success -> {
                        when (val validated = ResponseValidator.validate(coreResult.message, activeMode)) {
                            is ResponseValidator.ValidationResult.Valid -> {
                                val geminiMessage = ChatMessage(
                                    conversationId = conversationId,
                                    role = MessageRole.GEMINI,
                                    content = validated.response.rawText,
                                )
                                _uiState.update {
                                    it.copy(
                                        messages = it.messages + geminiMessage,
                                        isLoading = false,
                                    )
                                }
                                chatHistoryRepository?.insertMessage(geminiMessage)
                                maybeAutoTitle(conversationId)
                            }
                            is ResponseValidator.ValidationResult.Invalid -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = validated.userFacingReason,
                                    )
                                }
                            }
                        }
                    }
                    is AICoreResponse.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = coreResult.errorMessage,
                            )
                        }
                    }
                }
            } else {
                // Direct Repository path for backward compatibility and test fakes
                val currentMemories = userMemoryRepository?.getMemoriesSnapshot() ?: _uiState.value.userMemories
                val context = contextEngine.buildContext(
                    history = currentHistory,
                    newPrompt = userMessage.content,
                    mode = activeMode,
                    relevantSavedNotes = _uiState.value.savedNotes,
                    userMemories = currentMemories,
                )

                val apiResult = if (attachedBitmap != null) {
                    repository.generateMultimodal(
                        prompt = context.effectivePrompt,
                        imageBitmap = attachedBitmap,
                        systemInstruction = context.systemInstruction,
                    )
                } else {
                    repository.generateChat(
                        prompt = context.effectivePrompt,
                        history = context.recentMessages,
                        systemInstruction = context.systemInstruction,
                    )
                }

                apiResult.fold(
                    onSuccess = { rawText ->
                        when (val validated = ResponseValidator.validate(rawText, activeMode)) {
                            is ResponseValidator.ValidationResult.Valid -> {
                                val geminiMessage = ChatMessage(
                                    conversationId = conversationId,
                                    role = MessageRole.GEMINI,
                                    content = validated.response.rawText,
                                )
                                _uiState.update {
                                    it.copy(
                                        messages = it.messages + geminiMessage,
                                        isLoading = false,
                                    )
                                }
                                chatHistoryRepository?.insertMessage(geminiMessage)
                                maybeAutoTitle(conversationId)
                            }
                            is ResponseValidator.ValidationResult.Invalid -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = validated.userFacingReason,
                                    )
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to generate response. Check your internet connection.",
                            )
                        }
                    },
                )
            }
        }
    }

    private fun maybeAutoTitle(conversationId: String) {
        val messages = _uiState.value.messages
        if (messages.size == 2 && conversationId != "default") {
            viewModelScope.launch {
                val titlePrompt = "Generate a concise 3-4 word title for this study conversation: '${messages.first().content.take(80)}'. Return ONLY the title text."
                repository.generateText(titlePrompt).onSuccess { generatedTitle ->
                    val cleanTitle = generatedTitle.replace(Regex("[\"'\n]"), "").trim().take(30)
                    if (cleanTitle.isNotBlank()) {
                        chatHistoryRepository?.updateConversationTitle(conversationId, cleanTitle)
                        _uiState.update { it.copy(currentConversationTitle = cleanTitle) }
                    }
                }
            }
        }
    }

    fun generateOneOffPrompt(prompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.generateText(prompt).onSuccess { response ->
                onResult(response)
            }
        }
    }

    // ── Voice Assistant Actions ──────────────────────────────────────────────

    fun setShowLiveVoiceDialog(show: Boolean) {
        _uiState.update { it.copy(showLiveVoiceDialog = show) }
    }

    fun setVoiceState(state: VoiceState) {
        _uiState.update { it.copy(voiceState = state) }
    }

    fun toggleHandsFreeVoice() {
        _uiState.update { it.copy(isHandsFreeVoice = !it.isHandsFreeVoice) }
    }

    fun setHandsFreeVoice(enabled: Boolean) {
        _uiState.update { it.copy(isHandsFreeVoice = enabled) }
    }

    fun sendVoiceInput(spokenText: String, onAiResponseReady: ((String) -> Unit)? = null) {
        _uiState.update {
            it.copy(
                prompt = spokenText,
                voiceState = VoiceState.THINKING,
            )
        }
        onSend()

        // Wait for latest message to complete and provide to TTS
        viewModelScope.launch {
            while (_uiState.value.isLoading) {
                kotlinx.coroutines.delay(200)
            }
            val lastGeminiMessage = _uiState.value.messages.lastOrNull { it.role == MessageRole.GEMINI }
            if (lastGeminiMessage != null) {
                _uiState.update { it.copy(voiceState = VoiceState.SPEAKING) }
                onAiResponseReady?.invoke(lastGeminiMessage.content)
            } else {
                _uiState.update { it.copy(voiceState = VoiceState.IDLE) }
            }
        }
    }

    // ── Workflows & Knowledge Actions ────────────────────────────────────────

    fun saveFlashcards(cards: List<FlashcardEntity>) {
        viewModelScope.launch {
            knowledgeRepository?.saveFlashcards(cards)
        }
    }

    fun deleteFlashcard(id: String) {
        viewModelScope.launch {
            knowledgeRepository?.deleteFlashcard(id)
        }
    }

    fun saveStudyPlan(plan: StudyPlanEntity) {
        viewModelScope.launch {
            knowledgeRepository?.saveStudyPlan(plan)
        }
    }

    fun deleteStudyPlan(id: String) {
        viewModelScope.launch {
            knowledgeRepository?.deleteStudyPlan(id)
        }
    }

    fun addWeakTopic(topic: String, subject: String) {
        viewModelScope.launch {
            knowledgeRepository?.recordWeakTopicMistake(topic, subject)
        }
    }

    fun resolveWeakTopic(id: String) {
        viewModelScope.launch {
            knowledgeRepository?.resolveWeakTopic(id)
        }
    }

    // ── Personal AI Memory Actions ───────────────────────────────────────────

    fun addMemory(content: String, category: String) {
        viewModelScope.launch {
            userMemoryRepository?.saveMemory(content, category)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            userMemoryRepository?.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            userMemoryRepository?.clearAllMemories()
        }
    }

    // ── Settings & Workspace Actions ─────────────────────────────────────────

    fun setStreamingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isStreamingEnabled = enabled) }
    }

    fun clearAllWorkspaceData() {
        viewModelScope.launch {
            chatHistoryRepository?.clearAll()
            userMemoryRepository?.clearAllMemories()
            knowledgeRepository?.clearAll()
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    conversations = emptyList(),
                    savedNotes = emptyList(),
                    quizAttempts = emptyList(),
                    flashcards = emptyList(),
                    studyPlans = emptyList(),
                    weakTopics = emptyList(),
                    userMemories = emptyList(),
                    currentConversationTitle = "Study Workspace",
                )
            }
        }
    }

    // ── Session & History Actions ────────────────────────────────────────────

    fun onToggleSaveNote(message: ChatMessage) {
        val newSaved = !message.isSaved
        viewModelScope.launch {
            chatHistoryRepository?.setSavedStatus(message.id, newSaved)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map {
                        if (it.id == message.id) it.copy(isSaved = newSaved) else it
                    },
                )
            }
        }
    }

    fun createNewSession(mode: StudyMode = StudyMode.CHAT) {
        viewModelScope.launch {
            val title = "Study: ${mode.badgeLabel}"
            val newSession = chatHistoryRepository?.createConversation(title = title, mode = mode)
            val newId = newSession?.id ?: "default"
            loadConversationMessages(newId)
            _uiState.update {
                it.copy(
                    currentConversationTitle = title,
                    activeMode = mode,
                    selectedNavigation = NavigationDestination.CHAT,
                )
            }
        }
    }

    fun deleteSession(conversationId: String) {
        viewModelScope.launch {
            chatHistoryRepository?.deleteConversation(conversationId)
            if (_uiState.value.currentConversationId == conversationId) {
                loadConversationMessages("default")
                _uiState.update { it.copy(currentConversationTitle = "Study Workspace") }
            }
        }
    }

    fun onRecordQuizScore(topic: String, score: Int, total: Int, strong: String, revision: String) {
        val percentage = if (total > 0) (score * 100) / total else 0
        val attempt = QuizAttempt(
            conversationId = _uiState.value.currentConversationId,
            topic = topic,
            score = score,
            totalQuestions = total,
            percentage = percentage,
            strongAreas = strong,
            revisionTopics = revision,
        )
        viewModelScope.launch {
            chatHistoryRepository?.recordQuizAttempt(attempt)
        }
    }

    fun setSpeakingMessageId(messageId: String?) {
        _uiState.update { it.copy(speakingMessageId = messageId) }
    }

    fun setShowModeDialog(show: Boolean) {
        _uiState.update { it.copy(showModeDialog = show) }
    }

    fun setShowSavedNotesDialog(show: Boolean) {
        _uiState.update { it.copy(showSavedNotesDialog = show) }
    }

    fun setShowInsightsDialog(show: Boolean) {
        _uiState.update { it.copy(showInsightsDialog = show) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            chatHistoryRepository?.clearConversation(_uiState.value.currentConversationId)
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository?,
            hasApiKey: Boolean,
            knowledgeRepository: KnowledgeRepository? = null,
            userMemoryRepository: UserMemoryRepository? = null,
            aiCore: AICore? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    chatHistoryRepository = chatHistoryRepository,
                    hasApiKey = hasApiKey,
                    knowledgeRepository = knowledgeRepository,
                    userMemoryRepository = userMemoryRepository,
                    aiCore = aiCore,
                ) as T
        }
    }
}
