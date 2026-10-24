package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.security.EncryptedKeyDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private var hasApiKey: Boolean = false,
    private val keyDataStore: EncryptedKeyDataStore? = null,
    buildConfigApiKey: String = "",
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            (repository as? com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl)?.initializeHistory()
            repository.getChatHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }

        if (buildConfigApiKey.isNotBlank()) {
            hasApiKey = true
            viewModelScope.launch {
                keyDataStore?.saveApiKey(buildConfigApiKey)
            }
        } else if (keyDataStore != null) {
            viewModelScope.launch {
                if (keyDataStore.hasStoredKey()) {
                    hasApiKey = true
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearChat() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(messages = emptyList(), response = "") }
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

        val userMessage = ChatMessage(text = prompt, participant = Participant.USER)
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }
        viewModelScope.launch {
            repository.saveMessage(userMessage)
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    val assistantMessage = ChatMessage(text = text, participant = Participant.MODEL)
                    repository.saveMessage(assistantMessage)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = text,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "API request failed. Please check your connection and try again.",
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
            hasApiKey: Boolean = false,
            keyDataStore: EncryptedKeyDataStore? = null,
            buildConfigApiKey: String = "",
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    hasApiKey = hasApiKey || buildConfigApiKey.isNotBlank(),
                    keyDataStore = keyDataStore,
                    buildConfigApiKey = buildConfigApiKey,
                ) as T
        }
    }
}
