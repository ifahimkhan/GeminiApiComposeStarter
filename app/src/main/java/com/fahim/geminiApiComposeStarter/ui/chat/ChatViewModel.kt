package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistory
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val history: ChatHistory,
    private val preferences: ThemePreferences,
    hasApiKey: Boolean,
    private val keyAvailability: Flow<Boolean> = flowOf(hasApiKey)
) : ViewModel() {
    private val configuredKey = hasApiKey
    private val _uiState = MutableStateFlow(ChatUiState(hasApiKey = hasApiKey))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.observeAll().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            preferences.mode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            keyAvailability.collect { saved ->
                _uiState.update { it.copy(hasApiKey = configuredKey || saved) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = false) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onVoiceUnavailable() {
        _uiState.update { it.copy(errorMessage = "Speech recognition is unavailable on this device.") }
    }

    fun onCycleTheme() {
        val next = when (_uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
        viewModelScope.launch { preferences.setMode(next) }
    }

    fun onClearHistory() {
        viewModelScope.launch { history.clear() }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = true) }
            return
        }
        if (_uiState.value.isLoading) return
        if (!_uiState.value.hasApiKey) {
            _uiState.update { it.copy(errorMessage = "Add GEMINI_API_KEY to local.properties and rebuild.") }
            return
        }
        val userMessage = ChatMessage(UUID.randomUUID().toString(), prompt, true)
        _uiState.update {
            it.copy(messages = it.messages + userMessage, prompt = "", isLoading = true, errorMessage = null)
        }
        viewModelScope.launch {
            try {
                history.save(userMessage)
                repository.generateText(prompt).fold(
                    onSuccess = { answer ->
                        history.save(ChatMessage(UUID.randomUUID().toString(), answer, false))
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Could not get a Gemini response. Check your connection and key.")
                        }
                    }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Could not save the conversation. Try again.")
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: GeminiRepository,
            history: ChatHistory,
            preferences: ThemePreferences,
            hasApiKey: Boolean,
            keyAvailability: Flow<Boolean> = flowOf(hasApiKey)
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, preferences, hasApiKey, keyAvailability) as T
        }
    }
}
