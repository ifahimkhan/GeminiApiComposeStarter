package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ChatSessionDao
import com.fahim.geminiApiComposeStarter.data.local.ChatSessionEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatMessageDao: ChatMessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentSessionJob: Job? = null

    init {
        observeSessions()
        observePreferences()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            chatSessionDao.getAllSessions().collectLatest { entities ->
                val sessions = entities.map { ChatSessionState(it.id, it.title) }
                _uiState.update { it.copy(sessions = sessions) }
                
                if (sessions.isEmpty()) {
                    createNewSession()
                } else if (_uiState.value.currentSessionId == null) {
                    selectSession(sessions.first().id)
                }
            }
        }
    }
    
    fun createNewSession() {
        val newSessionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            chatSessionDao.insertSession(ChatSessionEntity(newSessionId, "New Chat"))
            selectSession(newSessionId)
        }
    }
    
    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId, messages = emptyList(), prompt = "") }
        currentSessionJob?.cancel()
        currentSessionJob = viewModelScope.launch {
            chatMessageDao.getMessagesForSession(sessionId).collectLatest { entities ->
                val uiMessages = entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        isFromUser = entity.isFromUser,
                        timestamp = entity.timestamp,
                        status = try {
                            MessageStatus.valueOf(entity.status)
                        } catch (e: Exception) {
                            MessageStatus.SUCCESS
                        }
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.autoScrollEnabled.collectLatest { enabled ->
                _uiState.update { it.copy(autoScrollEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.isDarkMode.collectLatest { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            val currentPrompt = _uiState.value.prompt
            val newPrompt = if (currentPrompt.isBlank()) {
                recognizedText
            } else {
                "$currentPrompt $recognizedText"
            }
            _uiState.update { it.copy(prompt = newPrompt, promptError = null) }
        }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        val sessionId = _uiState.value.currentSessionId ?: return
        
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        val userMessageEntity = ChatMessageEntity(
            sessionId = sessionId,
            text = prompt,
            isFromUser = true,
            status = MessageStatus.SUCCESS.name
        )

        val historyPairs = _uiState.value.messages.map { Pair(it.text, it.isFromUser) }
        val isFirstMessage = historyPairs.isEmpty()

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            if (isFirstMessage) {
                val title = if (prompt.length > 30) prompt.take(27) + "..." else prompt
                chatSessionDao.updateSession(sessionId, title, System.currentTimeMillis())
            } else {
                chatSessionDao.updateSession(sessionId, _uiState.value.sessions.find { it.id == sessionId }?.title ?: "Chat", System.currentTimeMillis())
            }
            
            chatMessageDao.insertMessage(userMessageEntity)

            val result = if (historyPairs.isNotEmpty()) {
                repository.generateTextWithHistory(historyPairs, prompt)
            } else {
                repository.generateText(prompt)
            }

            result.fold(
                onSuccess = { responseText ->
                    val botMessageEntity = ChatMessageEntity(
                        sessionId = sessionId,
                        text = responseText,
                        isFromUser = false,
                        status = MessageStatus.SUCCESS.name
                    )
                    chatMessageDao.insertMessage(botMessageEntity)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to generate response",
                        )
                    }
                }
            )
        }
    }

    fun onClearChat() {
        val sessionId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            chatMessageDao.clearMessagesForSession(sessionId)
        }
    }
    
    fun onDeleteSession(sessionId: String) {
        viewModelScope.launch {
            chatSessionDao.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                _uiState.update { it.copy(currentSessionId = null, messages = emptyList()) }
            }
        }
    }

    fun onToggleAutoScroll() {
        val newValue = !_uiState.value.autoScrollEnabled
        viewModelScope.launch {
            userPreferencesRepository.setAutoScrollEnabled(newValue)
        }
    }

    fun onToggleDarkMode(isSystemDark: Boolean) {
        val current = _uiState.value.isDarkMode ?: isSystemDark
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(!current)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatMessageDao: ChatMessageDao,
            chatSessionDao: ChatSessionDao,
            userPreferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    chatMessageDao = chatMessageDao,
                    chatSessionDao = chatSessionDao,
                    userPreferencesRepository = userPreferencesRepository,
                    hasApiKey = hasApiKey
                ) as T
        }
    }
}
