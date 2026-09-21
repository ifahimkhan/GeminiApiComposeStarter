package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val preferences: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            preferences.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
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

        _uiState.update { it.copy(prompt = "", promptError = null, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            history.add(Author.USER, prompt)
            fetchReply(prompt)
        }
    }

    /** Re-sends the last prompt after a failure, without adding a duplicate bubble. */
    fun onRetry() {
        val state = _uiState.value
        if (state.isLoading || !state.canRetry) return
        val prompt = state.messages.last().text

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { fetchReply(prompt) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun onClearConversation() {
        viewModelScope.launch { history.clear() }
    }

    private suspend fun fetchReply(prompt: String) {
        repository.generateText(prompt).fold(
            onSuccess = { text ->
                history.add(Author.GEMINI, text)
                _uiState.update { it.copy(isLoading = false) }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Something went wrong")
                }
            },
        )
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            history: ChatHistoryRepository,
            preferences: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, preferences, hasApiKey) as T
        }
    }
}
