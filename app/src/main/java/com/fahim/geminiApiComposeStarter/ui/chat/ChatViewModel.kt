package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistory
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.AiProvider
import com.fahim.geminiApiComposeStarter.data.ChatRepository
import com.fahim.geminiApiComposeStarter.data.ModelSelectionPreferences
import com.fahim.geminiApiComposeStarter.data.ProviderHttpException
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
    private val repository: ChatRepository,
    private val history: ChatHistory,
    private val preferences: ThemePreferences,
    private val modelPreferences: ModelSelectionPreferences,
    configuredProviders: Set<AiProvider>,
    private val keyAvailability: Flow<Set<AiProvider>> = flowOf(configuredProviders)
) : ViewModel() {
    private val configuredKeys = configuredProviders
    private val _uiState = MutableStateFlow(ChatUiState(availableProviders = configuredProviders))
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
                _uiState.update { it.copy(availableProviders = configuredKeys + saved) }
            }
        }
        viewModelScope.launch {
            modelPreferences.selectedProvider.collect { provider ->
                _uiState.update { it.copy(selectedProvider = provider) }
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

    fun onOpenModelSelector() {
        if (!_uiState.value.isLoading) _uiState.update { it.copy(showModelSelector = true) }
    }

    fun onCloseModelSelector() {
        _uiState.update { it.copy(showModelSelector = false) }
    }

    fun onSelectProvider(provider: AiProvider) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(selectedProvider = provider, showModelSelector = false, errorMessage = null) }
        viewModelScope.launch { modelPreferences.setProvider(provider) }
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
        val provider = _uiState.value.selectedProvider
        if (provider !in _uiState.value.availableProviders) {
            _uiState.update { it.copy(errorMessage = "Add ${provider.keyProperty} to local.properties and rebuild.") }
            return
        }
        val userMessage = ChatMessage(UUID.randomUUID().toString(), prompt, true)
        _uiState.update {
            it.copy(messages = it.messages + userMessage, prompt = "", isLoading = true, errorMessage = null)
        }
        viewModelScope.launch {
            try {
                history.save(userMessage)
                repository.generateText(provider, prompt).fold(
                    onSuccess = { answer ->
                        history.save(ChatMessage(UUID.randomUUID().toString(), answer, false))
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        val detail = when ((error as? ProviderHttpException)?.status) {
                            401 -> "Check the API key."
                            402 -> "This account needs credits."
                            429 -> "The account quota or rate limit was reached."
                            else -> "Check your connection and key."
                        }
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Could not get a response from ${provider.displayName}. $detail")
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
            repository: ChatRepository,
            history: ChatHistory,
            preferences: ThemePreferences,
            modelPreferences: ModelSelectionPreferences,
            configuredProviders: Set<AiProvider>,
            keyAvailability: Flow<Set<AiProvider>> = flowOf(configuredProviders)
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, preferences, modelPreferences, configuredProviders, keyAvailability) as T
        }
    }
}
