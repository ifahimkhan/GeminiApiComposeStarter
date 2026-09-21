package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatRepository: ChatRepository? = null,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // If persistent ChatRepository is attached, observe database message stream
        chatRepository?.let { repo ->
            viewModelScope.launch {
                repo.messages.collect { messageList ->
                    _uiState.update { it.copy(messages = messageList) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputReceived(spokenText: String) {
        if (spokenText.isNotBlank()) {
            val currentPrompt = _uiState.value.prompt
            val combined = if (currentPrompt.isBlank()) spokenText else "$currentPrompt $spokenText"
            onPromptChange(combined)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            if (chatRepository != null) {
                chatRepository.clearHistory()
            } else {
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
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
        if (_uiState.value.isLoading) return

        // Clear prompt field and set loading
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            if (chatRepository != null) {
                val result = chatRepository.sendUserPrompt(prompt)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Something went wrong",
                            )
                        }
                    },
                )
            } else {
                // Fallback for standalone/testing without Room DB
                val userMsg = ChatMessage(
                    id = System.nanoTime(),
                    role = MessageRole.USER,
                    text = prompt,
                )
                _uiState.update { it.copy(messages = it.messages + userMsg) }

                repository.generateText(prompt).fold(
                    onSuccess = { responseText ->
                        val geminiMsg = ChatMessage(
                            id = System.nanoTime(),
                            role = MessageRole.GEMINI,
                            text = responseText,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = it.messages + geminiMsg,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Something went wrong",
                            )
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatRepository: ChatRepository? = null,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, chatRepository, hasApiKey) as T
        }
    }
}
