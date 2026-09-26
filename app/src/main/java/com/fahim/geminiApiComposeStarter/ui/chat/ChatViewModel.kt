package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val darkModeOverride: StateFlow<Boolean?> = preferencesRepository.darkModeOverride
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            historyRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
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
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null)
        }

        viewModelScope.launch {
            historyRepository.addMessage(text = prompt, isFromUser = true)
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    historyRepository.addMessage(text = text, isFromUser = false)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Something went wrong")
                    }
                },
            )
        }
    }

    fun onVoiceInputUnavailable() {
        _uiState.update { it.copy(errorMessage = VOICE_INPUT_UNAVAILABLE_MESSAGE) }
    }

    fun toggleDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkModeOverride(!currentlyDark) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."
        const val VOICE_INPUT_UNAVAILABLE_MESSAGE =
            "Voice input isn't available on this device."

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            preferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, historyRepository, preferencesRepository, hasApiKey) as T
        }
    }
}
