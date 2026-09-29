package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val hasApiKey: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        chatHistoryRepository?.let { historyRepo ->
            viewModelScope.launch {
                historyRepo.getMessages().collect { persistedMessages ->
                    _uiState.update { currentState ->
                        currentState.copy(messages = persistedMessages)
                    }
                }
            }
        }

        userPreferencesRepository?.let { prefRepo ->
            viewModelScope.launch {
                prefRepo.isDarkTheme.collect { isDark ->
                    _uiState.update { currentState ->
                        currentState.copy(isDarkTheme = isDark)
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleDarkTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = newTheme) }
        viewModelScope.launch {
            userPreferencesRepository?.setDarkTheme(newTheme)
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            chatHistoryRepository?.clearHistory()
            _uiState.update { it.copy(messages = emptyList()) }
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

        val userMessage = ChatMessage(
            text = promptText,
            role = ChatRole.USER
        )

        _uiState.update { currentState ->
            val updatedMessages = if (currentState.messages.any { it.id == userMessage.id }) {
                currentState.messages
            } else {
                currentState.messages + userMessage
            }
            currentState.copy(
                prompt = "",
                messages = updatedMessages,
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        viewModelScope.launch {
            chatHistoryRepository?.saveMessage(userMessage)
            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val assistantMessage = ChatMessage(
                        text = responseText,
                        role = ChatRole.ASSISTANT
                    )
                    _uiState.update { currentState ->
                        val updatedMessages = if (currentState.messages.any { it.id == assistantMessage.id }) {
                            currentState.messages
                        } else {
                            currentState.messages + assistantMessage
                        }
                        currentState.copy(
                            messages = updatedMessages,
                            isLoading = false
                        )
                    }
                    chatHistoryRepository?.saveMessage(assistantMessage)
                },
                onFailure = { error ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong"
                        )
                    }
                }
            )
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository? = null,
            userPreferencesRepository: UserPreferencesRepository? = null,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    chatHistoryRepository = chatHistoryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    hasApiKey = hasApiKey,
                ) as T
        }
    }
}
