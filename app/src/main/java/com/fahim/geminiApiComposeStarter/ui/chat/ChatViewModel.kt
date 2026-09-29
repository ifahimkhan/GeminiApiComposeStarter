package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastPrompt: String? = null

    init {
        historyRepository?.let { historyRepo ->
            viewModelScope.launch {
                historyRepo.getChatHistory().collect { history ->
                    _uiState.update { state ->
                        state.copy(messages = history)
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    /**
     * Receives recognized speech and updates the prompt without auto-sending.
     */
    fun onVoiceResult(recognizedText: String) {
        val cleanSpeech = recognizedText.trim()
        if (cleanSpeech.isEmpty()) return

        val currentPrompt = _uiState.value.prompt.trim()
        val newPrompt = if (currentPrompt.isEmpty()) {
            cleanSpeech
        } else {
            "$currentPrompt $cleanSpeech"
        }
        _uiState.update { it.copy(prompt = newPrompt, promptError = null) }
    }

    /**
     * Updates the error message when speech recognition is unavailable or fails.
     */
    fun onVoiceError(message: String = "Speech recognition isn't available on this device.") {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (_uiState.value.isLoading) return

        lastPrompt = prompt
        val userMessage = ChatMessage(text = prompt, participant = Participant.USER)

        _uiState.update { state ->
            state.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                messages = if (historyRepository == null) state.messages + userMessage else state.messages,
            )
        }

        viewModelScope.launch {
            historyRepository?.insertMessage(userMessage)
            executePrompt(prompt)
        }
    }

    fun retry() {
        val promptToRetry = lastPrompt ?: return
        if (_uiState.value.isLoading) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }
        viewModelScope.launch {
            executePrompt(promptToRetry)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearChat() {
        viewModelScope.launch {
            historyRepository?.clearHistory()
            _uiState.update { state ->
                state.copy(
                    messages = emptyList(),
                    errorMessage = null,
                )
            }
        }
    }

    private suspend fun executePrompt(prompt: String) {
        repository.generateText(prompt).fold(
            onSuccess = { text ->
                val assistantMessage = ChatMessage(
                    text = text,
                    participant = Participant.ASSISTANT,
                )
                historyRepository?.insertMessage(assistantMessage)
                _uiState.update { state ->
                    state.copy(
                        messages = if (historyRepository == null) state.messages + assistantMessage else state.messages,
                        isLoading = false,
                        response = text,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Something went wrong",
                    )
                }
            },
        )
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, historyRepository) as T
        }

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository) as T
        }
    }
}
