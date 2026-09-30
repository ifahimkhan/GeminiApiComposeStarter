package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fahim.geminiApiComposeStarter.GeminiApp
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class ChatViewModel(
    private val gemini: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s -> _uiState.update { it.copy(settings = s) } }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(input = text) }

    /** Speech-to-text result: fill the box (or append) so the user can review before sending. */
    fun onVoiceResult(text: String) {
        if (text.isBlank()) return
        _uiState.update {
            val current = it.input.trimEnd()
            it.copy(input = if (current.isEmpty()) text else "$current $text")
        }
    }

    fun onVoiceUnavailable() = _uiState.update {
        it.copy(errorMessage = "Speech recognition isn't available on this device.", failedPrompt = null)
    }

    fun sendMessage() {
        val state = _uiState.value
        val prompt = state.input.trim()
        if (prompt.isEmpty() || state.isLoading) return

        val previous = state.messages
        _uiState.update { it.copy(input = "", isLoading = true, errorMessage = null, failedPrompt = null) }
        viewModelScope.launch {
            history.addMessage(prompt, isFromUser = true)
            requestReply(previous, prompt)
        }
    }

    fun retry() {
        val state = _uiState.value
        val prompt = state.failedPrompt ?: return
        if (state.isLoading) return

        // The user message is already saved; don't send it twice as history.
        val previous = state.messages.let { msgs ->
            val last = msgs.lastOrNull()
            if (last != null && last.isFromUser && last.text == prompt) msgs.dropLast(1) else msgs
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, failedPrompt = null) }
        viewModelScope.launch { requestReply(previous, prompt) }
    }

    private suspend fun requestReply(previous: List<ChatMessage>, prompt: String) {
        gemini.generateReply(previous, prompt)
            .onSuccess { reply ->
                history.addMessage(reply, isFromUser = false)
                _uiState.update { it.copy(isLoading = false) }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.toUserMessage(), failedPrompt = prompt)
                }
            }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null, failedPrompt = null) }

    fun clearHistory() {
        viewModelScope.launch { history.clear() }
        _uiState.update { it.copy(errorMessage = null, failedPrompt = null) }
    }

    fun setSettingsOpen(open: Boolean) = _uiState.update { it.copy(isSettingsOpen = open) }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    private fun Throwable.toUserMessage(): String {
        if (this is MissingApiKeyException) return message.orEmpty()
        if (this is IOException) return "Network error. Check your connection and try again."
        val raw = message.orEmpty()
        // The Gemini SDK wraps server errors as raw JSON; pull out the readable "message" field.
        val apiMessage = Regex("\"message\"\\s*:\\s*\"(.*?)\"").find(raw)?.groupValues?.get(1)
        return apiMessage?.takeIf { it.isNotBlank() }
            ?: raw.takeIf { it.isNotBlank() && it.length < 200 }
            ?: "Something went wrong. Please try again."
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as GeminiApp).container
                ChatViewModel(
                    gemini = container.geminiRepository,
                    history = container.chatHistoryRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
