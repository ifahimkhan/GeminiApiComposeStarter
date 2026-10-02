package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fahim.geminiApiComposeStarter.GeminiApp
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiException
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val gemini: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            preferences.preferences.collect { prefs -> _uiState.update { it.copy(preferences = prefs) } }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value.take(MAX_PROMPT_LENGTH), promptError = null) }
    }

    /** Appends recognised speech to whatever the user has already typed. */
    fun onVoiceInput(spoken: String) {
        if (spoken.isBlank()) return
        _uiState.update {
            val combined = listOf(it.prompt.trimEnd(), spoken.trim())
                .filter(String::isNotEmpty)
                .joinToString(" ")
            it.copy(prompt = combined.take(MAX_PROMPT_LENGTH), promptError = null)
        }
    }

    fun onSend() {
        val state = _uiState.value
        val prompt = state.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (state.isLoading) return

        val context = state.messages
        _uiState.update {
            it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null)
        }
        viewModelScope.launch {
            val userMessageId = history.add(Role.USER, prompt)
            gemini.generateText(context, prompt).fold(
                onSuccess = { text ->
                    history.add(Role.MODEL, text)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    // Roll back the unanswered turn and give the prompt back so the user can retry.
                    history.delete(userMessageId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prompt = it.prompt.ifEmpty { prompt },
                            // Only surface messages we wrote; never raw exception text.
                            errorMessage = (error as? GeminiException)?.message
                                ?: GENERIC_ERROR_MESSAGE,
                        )
                    }
                },
            )
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch { history.clear() }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColor(enabled) }
    }

    companion object {
        /** Caps request size so a pasted blob can't burn through the API quota. */
        const val MAX_PROMPT_LENGTH = 4_000
        const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again."

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GeminiApp).container
                ChatViewModel(
                    gemini = container.geminiRepository,
                    history = container.chatHistoryRepository,
                    preferences = container.userPreferencesRepository,
                )
            }
        }
    }
}
