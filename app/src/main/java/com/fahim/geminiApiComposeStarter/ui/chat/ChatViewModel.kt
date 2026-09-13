package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _isVoiceInputActive = MutableStateFlow(false)

    val uiState: StateFlow<ChatUiState> = combine(
        repository.getChatHistory(),
        _isLoading,
        _error,
        _isVoiceInputActive
    ) { history, isLoading, error, isVoiceActive ->
        ChatUiState(
            messages = history,
            isLoading = isLoading,
            error = error,
            isVoiceInputActive = isVoiceActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun onSend(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Save user message
            repository.saveMessage("user", trimmedPrompt)
            
            repository.generateText(trimmedPrompt).fold(
                onSuccess = { response ->
                    repository.saveMessage("model", response)
                    _isLoading.value = false
                },
                onFailure = { throwable ->
                    _error.value = throwable.message ?: "Failed to get response"
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
    
    fun setVoiceInputActive(active: Boolean) {
        _isVoiceInputActive.value = active
    }

    fun onErrorDismissed() {
        _error.value = null
    }
}
