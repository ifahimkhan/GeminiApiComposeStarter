package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.fahim.geminiApiComposeStarter.data.AppTheme
import com.fahim.geminiApiComposeStarter.data.ThemeRepository

class ChatViewModel(
    private val repository: GeminiRepository,
    private val themeRepository: ThemeRepository,
    private val hasApiKey: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.selectedThemeFlow.collect { theme ->
                _uiState.update { it.copy(selectedTheme = theme) }
            }
        }
        viewModelScope.launch {
            repository.getMessagesFlow().collect { dbMessages ->
                _uiState.update { it.copy(messages = dbMessages) }
            }
        }
    }

    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private var lastPromptText: String = ""

    fun retryLastMessage() {
        if (lastPromptText.isBlank() || _uiState.value.isLoading) return
        val currentPrompt = lastPromptText
        
        _uiState.update { 
            it.copy(
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            repository.generateText(currentPrompt).fold(
                onSuccess = { text ->
                    val geminiMessage = ChatMessage(text = text, sender = MessageSender.GEMINI)
                    repository.saveMessage(geminiMessage)
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

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey()) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        lastPromptText = promptText
        val userMessage = ChatMessage(text = promptText, sender = MessageSender.USER)
        
        _uiState.update { 
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            repository.saveMessage(userMessage)
            repository.generateText(promptText).fold(
                onSuccess = { text ->
                    val geminiMessage = ChatMessage(text = text, sender = MessageSender.GEMINI)
                    repository.saveMessage(geminiMessage)
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

        fun factory(repository: GeminiRepository, themeRepository: ThemeRepository, hasApiKey: () -> Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, themeRepository, hasApiKey) as T
            }
    }
}
