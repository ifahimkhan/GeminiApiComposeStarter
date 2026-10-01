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
    private val historyRepository: ChatHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent chat history from Room
        viewModelScope.launch {
            historyRepository.getMessages().collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }

        // Collect persistent model preference from DataStore
        viewModelScope.launch {
            preferencesRepository.selectedModelFlow.collect { model ->
                _uiState.update { it.copy(selectedModel = model) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceInputResult(spokenText: String) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { current ->
            val newPrompt = if (current.prompt.isBlank()) {
                trimmed
            } else {
                "${current.prompt} $trimmed"
            }
            current.copy(prompt = newPrompt, promptError = null)
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

        val currentModel = _uiState.value.selectedModel

        // Immediately persist user message and update UI
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            // Save user message in Room database
            historyRepository.saveUserMessage(prompt)

            // Request response from Gemini repository
            repository.generateText(prompt, currentModel).fold(
                onSuccess = { responseText ->
                    // Persist Gemini response in Room
                    historyRepository.saveGeminiMessage(responseText)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    val displayError = error.message?.takeIf { it.isNotBlank() }
                        ?: "Something went wrong communicating with Gemini."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = displayError,
                        )
                    }
                },
            )
        }
    }

    fun onModelSelected(modelName: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedModel(modelName)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            preferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    historyRepository = historyRepository,
                    preferencesRepository = preferencesRepository,
                    hasApiKey = hasApiKey,
                ) as T
        }
    }
}
