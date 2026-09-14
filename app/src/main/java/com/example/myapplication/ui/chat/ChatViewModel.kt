package com.example.myapplication.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.UserPreferencesRepository
import com.example.myapplication.data.repository.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getChatHistory(),
                userPreferencesRepository.autoScrollEnabled,
                userPreferencesRepository.darkModeOverride
            ) { history, autoScroll, darkMode ->
                Triple(history, autoScroll, darkMode)
            }.collect { (history, autoScroll, darkMode) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = history,
                        autoScrollEnabled = autoScroll,
                        darkModeOverride = darkMode
                    )
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun onSpeechRecognized(text: String) {
        if (text.isNotBlank()) {
            _uiState.update { currentState ->
                val currentInput = currentState.inputText
                val updatedInput = if (currentInput.isBlank()) text else "$currentInput $text"
                currentState.copy(inputText = updatedInput)
            }
        }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.sendMessage(prompt)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, inputText = "") }
            } else {
                val userFriendlyMessage = "Failed to connect to Gemini API. Please check your internet connection and local.properties setup."
                _uiState.update { it.copy(isLoading = false, errorMessage = userFriendlyMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setDarkMode(dark: Boolean?) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(dark)
        }
    }
}
