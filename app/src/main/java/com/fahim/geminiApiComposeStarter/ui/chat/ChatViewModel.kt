package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ConversationRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val conversations: ConversationRepository,
    private val preferences: UserPreferences,
    private val hasApiKey: Boolean,
    initializeKey: suspend () -> Unit = {},
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var keyReady = false

    init {
        viewModelScope.launch {
            try {
                initializeKey()
                keyReady = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError("Secure key storage could not be initialized. Restart the app to retry.")
            } finally {
                _uiState.update { it.copy(isInitializing = false) }
            }
        }
        viewModelScope.launch {
            conversations.messages.catch {
                showError("Conversation history could not be loaded. Restart the app to retry.")
            }.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            preferences.theme.catch {
                showError("Theme preferences could not be loaded.")
            }.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val state = _uiState.value
        if (state.isLoading || state.isInitializing || state.isClearing) return
        val prompt = state.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            showError(MISSING_API_KEY_MESSAGE)
            return
        }
        if (!keyReady) {
            showError("Secure key storage is unavailable. Restart the app to retry.")
            return
        }
        _uiState.update {
            it.copy(isLoading = true, prompt = "", errorMessage = null, promptError = null)
        }
        viewModelScope.launch {
            var userSaved = false
            try {
                conversations.add(ChatMessage(text = prompt, isUser = true))
                userSaved = true
                val result = repository.generateText(prompt)
                val response = result.getOrNull()
                if (response != null) {
                    conversations.add(ChatMessage(text = response, isUser = false))
                } else {
                    showError("Gemini could not respond. Check your connection, API access and quota, then try again.")
                    restorePrompt(prompt)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(
                    if (userSaved) "The response could not be completed or saved. Please try again."
                    else "Your message could not be saved. Please try again.",
                )
                restorePrompt(prompt)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun restorePrompt(prompt: String) {
        _uiState.update { if (it.prompt.isBlank()) it.copy(prompt = prompt) else it }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                preferences.setTheme(mode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError("Your theme preference could not be saved.")
            }
        }
    }

    fun requestClearHistory() {
        if (!_uiState.value.isLoading && !_uiState.value.isClearing) {
            _uiState.update { it.copy(showClearConfirmation = true) }
        }
    }

    fun dismissClearHistory() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    fun clearHistory() {
        if (_uiState.value.isLoading || _uiState.value.isClearing) return
        _uiState.update { it.copy(isClearing = true, showClearConfirmation = false) }
        viewModelScope.launch {
            try {
                conversations.clear()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError("Conversation history could not be cleared. Please try again.")
            } finally {
                _uiState.update { it.copy(isClearing = false) }
            }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            conversations: ConversationRepository,
            preferences: UserPreferences,
            hasApiKey: Boolean,
            initializeKey: suspend () -> Unit,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ChatViewModel::class.java))
                return ChatViewModel(repository, conversations, preferences, hasApiKey, initializeKey) as T
            }
        }
    }
}
