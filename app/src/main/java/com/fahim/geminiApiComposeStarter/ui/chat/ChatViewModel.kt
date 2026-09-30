package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> = combine(
        _uiState,
        repository.messages,
        prefsRepo.preferences
    ) { state, messages, prefs ->
        state.copy(
            messages = messages,
            themeMode = prefs.themeMode,
            selectedModel = prefs.selectedModel,
            userName = prefs.preferredName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState()
    )

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun onVoiceResult(spoken: String) {
        _uiState.update { it.copy(inputText = spoken, isListening = false) }
    }

    fun onListeningStarted() {
        _uiState.update { it.copy(isListening = true) }
    }

    fun onListeningCancelled() {
        _uiState.update { it.copy(isListening = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissModelNotice() {
        _uiState.update { it.copy(modelNoticeDialogMessage = null) }
    }

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            prefsRepo.setSelectedModel(modelName)
            _uiState.update { 
                it.copy(modelNoticeDialogMessage = "Note: If you are using the free API, models like gemini-1.5-pro may have strict rate limits.")
            }
        }
    }

    fun selectThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty()) return

        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = "GEMINI_API_KEY is missing. Add it to local.properties and rebuild.") }
            return
        }

        if (_uiState.value.isSending) return

        _uiState.update { it.copy(inputText = "", isSending = true, errorMessage = null) }

        viewModelScope.launch {
            // First add user message
            repository.addMessage(prompt, isFromUser = true)
            
            val modelName = uiState.value.selectedModel
            
            repository.generateText(prompt, modelName).fold(
                onSuccess = { text ->
                    repository.addMessage(text, isFromUser = false)
                    _uiState.update { it.copy(isSending = false) }
                },
                onFailure = { error ->
                    repository.addMessage(error.message ?: "Failed to get response", isFromUser = false, isError = true)
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                }
            )
        }
    }
}
