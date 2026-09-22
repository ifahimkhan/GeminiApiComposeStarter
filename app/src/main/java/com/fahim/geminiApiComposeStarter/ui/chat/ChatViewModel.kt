package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val chatHistoryRepository: ChatHistoryRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        chatHistoryRepository?.let { historyRepo ->
            viewModelScope.launch {
                historyRepo.getMessages().collect { savedMessages ->
                    _uiState.update { current ->
                        if (current.messages.isEmpty() && savedMessages.isNotEmpty()) {
                            current.copy(messages = savedMessages)
                        } else {
                            current
                        }
                    }
                }
            }
        }
        userPreferencesRepository?.let { prefsRepo ->
            viewModelScope.launch {
                prefsRepo.userPreferencesFlow.collect { prefs ->
                    _uiState.update { it.copy(theme = prefs.theme) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSpeechResult(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = _uiState.value.prompt.trim()
        val updated = if (current.isEmpty()) trimmed else "$current $trimmed"
        _uiState.update { it.copy(prompt = updated, promptError = null) }
    }

    fun onSpeechError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onShowThemeDialog(show: Boolean) {
        _uiState.update { it.copy(isThemeDialogOpen = show) }
    }

    fun onSetTheme(theme: AppTheme) {
        _uiState.update { it.copy(theme = theme, isThemeDialogOpen = false) }
        viewModelScope.launch {
            userPreferencesRepository?.setTheme(theme)
        }
    }

    fun onShowClearHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(isClearHistoryDialogOpen = show) }
    }

    fun onClearHistory() {
        _uiState.update { it.copy(messages = emptyList(), isClearHistoryDialogOpen = false) }
        viewModelScope.launch(ioDispatcher) {
            chatHistoryRepository?.clearHistory()
        }
    }

    fun onRetry() {
        val failedPrompt = _uiState.value.lastFailedPrompt ?: return
        if (_uiState.value.isLoading) return
        executePrompt(promptText = failedPrompt, isRetry = true)
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
        // Prevent accidental duplicate submissions
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(text = prompt, participant = Participant.USER)
        _uiState.update {
            it.copy(
                prompt = "",
                promptError = null,
                errorMessage = null,
                canRetry = false,
                lastFailedPrompt = null,
                messages = it.messages + userMessage,
            )
        }

        viewModelScope.launch(ioDispatcher) {
            chatHistoryRepository?.saveMessage(userMessage)
        }

        executePrompt(promptText = prompt, isRetry = false)
    }

    private fun executePrompt(promptText: String, isRetry: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                promptError = null,
                canRetry = false,
            )
        }

        viewModelScope.launch {
            repository.generateText(promptText).fold(
                onSuccess = { text ->
                    val modelMessage = ChatMessage(text = text, participant = Participant.MODEL)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = text,
                            messages = it.messages + modelMessage,
                            lastFailedPrompt = null,
                            canRetry = false,
                        )
                    }
                    viewModelScope.launch(ioDispatcher) {
                        chatHistoryRepository?.saveMessage(modelMessage)
                    }
                },
                onFailure = { error ->
                    val friendlyMessage = error.message?.takeIf { it.isNotBlank() }
                        ?: "Failed to communicate with Gemini. Please try again."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyMessage,
                            lastFailedPrompt = promptText,
                            canRetry = true,
                        )
                    }
                },
            )
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            chatHistoryRepository: ChatHistoryRepository? = null,
            userPreferencesRepository: UserPreferencesRepository? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    hasApiKey = hasApiKey,
                    chatHistoryRepository = chatHistoryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    ioDispatcher = ioDispatcher,
                ) as T
        }
    }
}
