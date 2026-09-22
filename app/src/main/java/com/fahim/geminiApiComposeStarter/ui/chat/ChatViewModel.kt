package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.Sender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val history: ChatHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.observeMessages().collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
    }

    fun onPromptChange(value: String) = _uiState.update { it.copy(prompt = value, promptError = null) }

    fun onVoiceInputError() = _uiState.update { it.copy(errorMessage = VOICE_INPUT_ERROR_MESSAGE) }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }
        viewModelScope.launch {
            try {
                history.add(prompt, Sender.USER)
                repository.generateText(prompt).fold(
                    onSuccess = { response -> history.add(response, Sender.GEMINI) },
                    onFailure = { _uiState.update { it.copy(errorMessage = RESPONSE_ERROR_MESSAGE) } },
                )
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = RESPONSE_ERROR_MESSAGE) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        const val RESPONSE_ERROR_MESSAGE = "Could not get a response. Check your connection and API configuration."
        const val VOICE_INPUT_ERROR_MESSAGE = "Voice input is unavailable or no speech was recognized."

        fun factory(repository: GeminiRepository, history: ChatHistoryRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, history) as T
            }
    }
}
