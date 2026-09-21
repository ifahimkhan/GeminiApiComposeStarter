package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepository
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val secureKeyStorage: SecureApiKeyStorage? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    hasApiKeyInitially: Boolean = true,
) : ViewModel() {

    // Backwards-compatible constructor for testing with GeminiRepository directly
    constructor(
        repository: GeminiRepository,
        hasApiKey: Boolean = true
    ) : this(
        chatRepository = InMemoryChatRepository(repository),
        hasApiKeyInitially = hasApiKey
    )

    private val _uiState = MutableStateFlow(
        ChatUiState(hasApiKey = hasApiKeyInitially)
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent chat messages
        chatRepository.messagesStream
            .onEach { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
            .launchIn(viewModelScope)

        // Observe theme preference if available
        userPreferencesRepository?.themeFlow
            ?.onEach { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
            ?.launchIn(viewModelScope)

        // Observe API key availability
        secureKeyStorage?.hasApiKeyFlow
            ?.onEach { hasKey ->
                _uiState.update { it.copy(hasApiKey = hasKey) }
            }
            ?.launchIn(viewModelScope)
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!_uiState.value.hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        // Clear input prompt and enter loading state
        _uiState.update { it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null) }

        viewModelScope.launch {
            chatRepository.sendMessage(prompt).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to generate response."
                        )
                    }
                }
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }

    fun onThemeChange(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository?.setTheme(theme)
        }
    }

    fun onSaveApiKey(apiKey: String) {
        viewModelScope.launch {
            secureKeyStorage?.saveApiKey(apiKey)
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties or enter it in app settings."

        fun factory(
            chatRepository: ChatRepository,
            secureKeyStorage: SecureApiKeyStorage? = null,
            userPreferencesRepository: UserPreferencesRepository? = null,
            hasApiKey: Boolean = true,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    chatRepository = chatRepository,
                    secureKeyStorage = secureKeyStorage,
                    userPreferencesRepository = userPreferencesRepository,
                    hasApiKeyInitially = hasApiKey
                ) as T
        }

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}

/**
 * Fallback in-memory chat repository for ViewModel testing or direct GeminiRepository usage.
 */
class InMemoryChatRepository(
    private val geminiRepository: GeminiRepository
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messagesStream: Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(prompt: String): Result<ChatMessage> {
        val userMessage = ChatMessage(text = prompt, participant = Participant.USER)
        _messages.update { it + userMessage }

        val result = geminiRepository.generateText(prompt)
        return result.fold(
            onSuccess = { text ->
                val geminiMessage = ChatMessage(text = text, participant = Participant.GEMINI)
                _messages.update { it + geminiMessage }
                Result.success(geminiMessage)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
