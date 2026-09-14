package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatMessageDao: ChatMessageDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean
) : ViewModel() {

    // Each session is identified by a timestamp. New chat = new sessionId.
    private var currentSessionId: Long = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Job handles for active collectors so we can cancel & restart on session switch
    private var sessionJob: kotlinx.coroutines.Job? = null

    init {
        startObservingSession()
        observeTheme()
        observeSessionHistory()
    }

    private fun startObservingSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.getMessagesForSession(currentSessionId).collect { dbMessages ->
                val displayMessages = dbMessages.map { entity ->
                    val decrypted = runCatching {
                        KeystoreManager.decrypt(entity.prompt)
                    }.getOrDefault(entity.prompt)
                    entity.copy(prompt = decrypted)
                }
                _uiState.update { it.copy(messages = displayMessages) }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                preferencesRepository.isDarkModeFlow.collect { isDark ->
                    _uiState.update { it.copy(isDarkMode = isDark) }
                }
            }
        }
    }

    private fun observeSessionHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.getAllSessionIds().collect { sessionIds ->
                val sessions = sessionIds.mapNotNull { sid ->
                    val first = runCatching {
                        chatMessageDao.getFirstMessageForSession(sid)
                    }.getOrNull()
                    if (first != null) {
                        val decrypted = runCatching {
                            KeystoreManager.decrypt(first.prompt)
                        }.getOrDefault(first.prompt)
                        ChatSession(
                            sessionId = sid,
                            previewText = decrypted.take(60).let {
                                if (decrypted.length > 60) "$it…" else it
                            }
                        )
                    } else null
                }
                _uiState.update { it.copy(chatSessions = sessions) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { it.copy(prompt = recognizedText, promptError = null) }
            onSend()
        }
    }

    fun onToggleDarkMode() {
        val newMode = !_uiState.value.isDarkMode
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { preferencesRepository.setDarkMode(newMode) }
        }
    }

    fun onToggleHistory() {
        _uiState.update { it.copy(isHistoryOpen = !it.isHistoryOpen) }
    }

    /** Switch to a past session from history. */
    fun onOpenSession(sessionId: Long) {
        currentSessionId = sessionId
        _uiState.update { it.copy(messages = emptyList(), prompt = "", isHistoryOpen = false) }
        startObservingSession()
    }

    /** Start a completely fresh chat with a new sessionId. */
    fun onNewChat() {
        currentSessionId = System.currentTimeMillis()
        _uiState.update { it.copy(messages = emptyList(), prompt = "", errorMessage = null, isHistoryOpen = false) }
        startObservingSession()
    }

    /** Delete all messages in the current chat session. */
    fun onClearHistory() {
        val sessionToDelete = currentSessionId
        // Immediately clear the UI — don't wait on the Room Flow re-emission
        _uiState.update { it.copy(messages = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.clearSession(sessionToDelete)
        }
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null, promptError = null, prompt = "") }

        viewModelScope.launch(Dispatchers.IO) {
            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    // Encrypt prompt with Android Keystore AES-256-GCM before persisting
                    val encryptedPrompt = runCatching {
                        KeystoreManager.encrypt(promptText)
                    }.getOrDefault(promptText)

                    val messageEntity = ChatMessageEntity(
                        sessionId = currentSessionId,
                        prompt = encryptedPrompt,
                        response = responseText,
                        modelName = "gemini-3.6-flash"
                    )
                    chatMessageDao.insertMessage(messageEntity)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun onRetry(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
        onSend()
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatMessageDao: ChatMessageDao,
            preferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatMessageDao, preferencesRepository, hasApiKey) as T
        }
    }
}
