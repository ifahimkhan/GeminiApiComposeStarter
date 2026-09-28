package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.MessageSender
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Chat UI state, user actions, Room persistence, and Gemini API calls.
 */
class ChatViewModel(
    private val geminiRepository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val hasApiKey: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent chat history from Room
        chatHistoryRepository?.let { historyRepo ->
            viewModelScope.launch {
                historyRepo.getMessages()
                    .catch { e ->
                        _uiState.update {
                            it.copy(errorMessage = "Failed to load chat history: ${e.message}")
                        }
                    }
                    .collect { savedMessages ->
                        _uiState.update { it.copy(messages = savedMessages) }
                    }
            }
        }

        // Collect preferences from DataStore
        userPreferencesRepository?.let { prefsRepo ->
            viewModelScope.launch {
                prefsRepo.userPreferencesFlow.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            themeMode = prefs.themeMode,
                            userName = prefs.userName,
                        )
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(speechText: String) {
        val trimmed = speechText.trim()
        if (trimmed.isEmpty()) return
        val currentPrompt = _uiState.value.prompt.trim()
        val newPrompt = if (currentPrompt.isEmpty()) trimmed else "$currentPrompt $trimmed"
        _uiState.update { it.copy(prompt = newPrompt, promptError = null) }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }

        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }

        // Duplicate send protection: ignore if a request is already in-flight
        if (_uiState.value.isLoading) return

        executeSend(prompt)
    }

    fun onRetry() {
        val failedPrompt = _uiState.value.lastFailedPrompt ?: return
        if (_uiState.value.isLoading) return
        executeSend(failedPrompt)
    }

    private fun executeSend(prompt: String) {
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                canRetry = false,
            )
        }

        viewModelScope.launch {
            val userMessage = ChatMessage(
                text = prompt,
                sender = MessageSender.USER,
                status = MessageStatus.SUCCESS,
            )
            val loadingBotMessage = ChatMessage(
                text = "",
                sender = MessageSender.GEMINI,
                status = MessageStatus.LOADING,
            )

            val botMessageId = if (chatHistoryRepository != null) {
                chatHistoryRepository.insertMessage(userMessage)
                chatHistoryRepository.insertMessage(loadingBotMessage)
            } else {
                _uiState.update { it.copy(messages = it.messages + userMessage + loadingBotMessage) }
                0L
            }

            geminiRepository.generateText(prompt).fold(
                onSuccess = { responseText ->
                    if (chatHistoryRepository != null && botMessageId != 0L) {
                        chatHistoryRepository.updateMessage(
                            loadingBotMessage.copy(
                                id = botMessageId,
                                text = responseText,
                                status = MessageStatus.SUCCESS,
                            )
                        )
                    } else if (chatHistoryRepository == null) {
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.sender == MessageSender.GEMINI && msg.status == MessageStatus.LOADING) {
                                    msg.copy(text = responseText, status = MessageStatus.SUCCESS)
                                } else msg
                            }
                            state.copy(messages = updated)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canRetry = false,
                            lastFailedPrompt = null,
                        )
                    }
                },
                onFailure = { error ->
                    val failureMessage = error.message ?: "An unexpected error occurred."
                    if (chatHistoryRepository != null && botMessageId != 0L) {
                        chatHistoryRepository.updateMessage(
                            loadingBotMessage.copy(
                                id = botMessageId,
                                text = failureMessage,
                                status = MessageStatus.ERROR,
                            )
                        )
                    } else if (chatHistoryRepository == null) {
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.sender == MessageSender.GEMINI && msg.status == MessageStatus.LOADING) {
                                    msg.copy(text = failureMessage, status = MessageStatus.ERROR)
                                } else msg
                            }
                            state.copy(messages = updated)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = failureMessage,
                            canRetry = true,
                            lastFailedPrompt = prompt,
                        )
                    }
                },
            )
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onShowClearConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showClearConfirmDialog = show) }
    }

    fun onShowStudentInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showStudentInfoDialog = show) }
    }

    fun onClearChatHistory() {
        viewModelScope.launch {
            chatHistoryRepository?.clearHistory()
            _uiState.update {
                it.copy(
                    messages = if (chatHistoryRepository == null) emptyList() else it.messages,
                    showClearConfirmDialog = false,
                    errorMessage = null,
                    canRetry = false,
                    lastFailedPrompt = null,
                )
            }
        }
    }

    fun onThemeChange(themeMode: String) {
        viewModelScope.launch {
            userPreferencesRepository?.setThemeMode(themeMode)
            _uiState.update { it.copy(themeMode = themeMode) }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties or set the GEMINI_API_KEY environment variable and rebuild."

        fun factory(
            geminiRepository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository? = null,
            userPreferencesRepository: UserPreferencesRepository? = null,
            hasApiKey: Boolean = true,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    geminiRepository = geminiRepository,
                    chatHistoryRepository = chatHistoryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    hasApiKey = hasApiKey,
                ) as T
        }
    }
}
