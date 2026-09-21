package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val store: ChatStore,
    private val preferences: UserPreferences,
) : ViewModel() {
    private val state = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = state.asStateFlow()
    init {
        viewModelScope.launch {
            try { store.messages.collect { messages -> state.update { it.copy(messages = messages, historyLoaded = true) } } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { reportError("Could not load chat history. Restart the app to try again.") }
        }
        viewModelScope.launch {
            try { preferences.compactMessages.collect { value -> state.update { it.copy(compactMessages = value) } } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { reportError("Could not load your preferences.") }
        }
        viewModelScope.launch {
            try { preferences.darkMode.collect { value -> state.update { it.copy(darkModeOverride = value) } } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* non-critical, fall back to system theme */ }
        }
    }
    fun updateInput(value: String) { state.update { it.copy(input = value) } }
    fun clearError() { state.update { it.copy(errorMessage = null) } }
    fun reportError(message: String) { state.update { it.copy(errorMessage = message) } }
    fun setCompactMessages(value: Boolean) {
        viewModelScope.launch {
            try { preferences.setCompactMessages(value) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { reportError("Could not save your preference.") }
        }
    }
    fun toggleDarkMode() {
        val current = state.value.darkModeOverride
        val next = when (current) {
            null -> true   // system → dark
            true -> false  // dark → light
            false -> null  // light → system
        }
        viewModelScope.launch {
            try { preferences.setDarkMode(next) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* non-critical */ }
        }
    }
    fun sendMessage() {
        val current = state.value
        val prompt = current.input.trim()
        if (prompt.isEmpty() || current.isLoading || !current.historyLoaded) return
        state.update { it.copy(input = "", isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                store.insert(ChatMessageEntity(text = prompt, isFromUser = true))
                val reply = repository.generateText(prompt).getOrThrow()
                store.insert(ChatMessageEntity(text = reply, isFromUser = false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(
                    input = it.input.ifBlank { prompt },
                    errorMessage = e.message ?: "Unable to reach Gemini. Check your API key and connection."
                ) }
            } finally { state.update { it.copy(isLoading = false) } }
        }
    }
    fun clearHistory() {
        if (state.value.isLoading) return
        viewModelScope.launch {
            try { store.clear() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { reportError("Could not clear chat history.") }
        }
    }
    companion object {
        fun factory(repository: GeminiRepository, store: ChatStore, preferences: UserPreferences) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChatViewModel::class.java))
                    return ChatViewModel(repository, store, preferences) as T
                }
            }
    }
}
