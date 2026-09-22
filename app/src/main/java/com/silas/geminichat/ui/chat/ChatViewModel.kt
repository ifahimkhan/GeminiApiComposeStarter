package com.silas.geminichat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.geminichat.data.ChatHistoryRepository
import com.silas.geminichat.data.GeminiRepository
import com.silas.geminichat.data.prefs.UserPreferencesRepository
import com.silas.geminichat.model.ChatMessage
import com.silas.geminichat.model.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds all chat UI state as a [StateFlow], keeping composables stateless.
 *
 * @author Silas · n091 (MBA Tech CE, E1)
 */
class ChatViewModel(
    private val geminiRepository: GeminiRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Load persisted chat history (Room) into state.
        viewModelScope.launch {
            chatHistoryRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        // Load user preferences (Preferences DataStore).
        viewModelScope.launch {
            userPreferencesRepository.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(text: String?) {
        if (!text.isNullOrBlank()) {
            _uiState.update { it.copy(prompt = text, promptError = null) }
        }
    }

    fun onUserNameChange(name: String) {
        viewModelScope.launch { userPreferencesRepository.setUserName(name) }
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

        val userMessage = ChatMessage(role = MessageRole.USER, content = prompt)
        _uiState.update {
            it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null)
        }
        viewModelScope.launch {
            chatHistoryRepository.addMessage(userMessage)
            geminiRepository.generateText(prompt).fold(
                onSuccess = { text ->
                    val reply = ChatMessage(role = MessageRole.ASSISTANT, content = text)
                    chatHistoryRepository.addMessage(reply)
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

    fun clearHistory() {
        viewModelScope.launch { chatHistoryRepository.clearHistory() }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "Gemini API key is missing. Add GEMINI_API_KEY to local.properties and rebuild."

        fun factory(
            geminiRepository: GeminiRepository,
            chatHistoryRepository: ChatHistoryRepository,
            userPreferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean = true,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
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
