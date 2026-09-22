package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository?,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMessages().collectLatest { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository?.lastPrompt?.collectLatest { lastPrompt ->
                if (_uiState.value.prompt.isEmpty() && lastPrompt.isNotEmpty()) {
                    _uiState.update { it.copy(prompt = lastPrompt) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
        viewModelScope.launch {
            userPreferencesRepository?.saveLastPrompt(value)
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

        val userMessage = ChatMessage(text = currentPrompt, isUser = true)
        _uiState.update { state ->
            state.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            repository.saveMessage(userMessage)
            userPreferencesRepository?.saveLastPrompt("")

            repository.generateText(currentPrompt).fold(
                onSuccess = { text ->
                    val botMessage = ChatMessage(text = text, isUser = false)
                    repository.saveMessage(botMessage)
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
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            userPreferencesRepository: UserPreferencesRepository?,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, userPreferencesRepository, hasApiKey) as T
        }
    }
}
