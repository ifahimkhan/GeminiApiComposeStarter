package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var pendingRetryPrompt: String? = null

    init {
        repository.observeConversation()
            .onEach { history ->
                _uiState.update { it.copy(messages = history) }
            }
            .launchIn(viewModelScope)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputReceived(spokenQuery: String) {
        if (spokenQuery.isBlank()) return
        _uiState.update { state ->
            val combined = if (state.prompt.isBlank()) {
                spokenQuery.trim()
            } else {
                "${state.prompt.trim()} ${spokenQuery.trim()}"
            }
            state.copy(prompt = combined, promptError = null)
        }
    }

    fun onSend() {
        val currentPrompt = _uiState.value.prompt.trim()
        if (currentPrompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        submitPrompt(currentPrompt)
    }

    fun onRetryLastPrompt() {
        val retryText = pendingRetryPrompt ?: _uiState.value.prompt.trim()
        if (retryText.isNotBlank() && hasApiKey && !_uiState.value.isLoading) {
            submitPrompt(retryText)
        }
    }

    private fun submitPrompt(promptText: String) {
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }
        viewModelScope.launch {
            repository.sendPrompt(promptText).fold(
                onSuccess = {
                    pendingRetryPrompt = null
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    pendingRetryPrompt = promptText
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to reach Gemini API",
                        )
                    }
                },
            )
        }
    }

    fun onClearConversation() {
        viewModelScope.launch {
            repository.clearConversation()
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatViewModel(repository, hasApiKey) }
        }
    }
}
