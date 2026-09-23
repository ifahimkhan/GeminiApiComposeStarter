package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageRole
import com.fahim.geminiApiComposeStarter.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Collect messages reactively from Room via the repository
        viewModelScope.launch {
            repository.messages.collect { entities ->
                val uiMessages = entities.map { entity ->
                    val role = try {
                        MessageRole.valueOf(entity.role)
                    } catch (_: Exception) {
                        MessageRole.MODEL
                    }
                    ChatMessage(
                        id = entity.id,
                        role = role,
                        content = entity.content,
                        timestamp = entity.timestamp,
                        isError = entity.isError,
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }

        // Collect preferences if available
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.selectedModel.collect { model ->
                    val resolvedModel = if (model.contains("2.5")) {
                        launch { prefs.setSelectedModel("gemini-3.6-flash") }
                        "gemini-3.6-flash"
                    } else {
                        model
                    }
                    _uiState.update { it.copy(selectedModel = resolvedModel) }
                }
            }
            viewModelScope.launch {
                prefs.themeMode.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSpeechResult(spokenText: String) {
        if (spokenText.isBlank()) return
        _uiState.update { current ->
            val newPrompt = if (current.prompt.isBlank()) {
                spokenText.trim()
            } else {
                "${current.prompt.trim()} ${spokenText.trim()}"
            }
            current.copy(prompt = newPrompt, promptError = null)
        }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!repository.hasApiKey()) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        // Clear input field immediately upon send
        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            val modelName = _uiState.value.selectedModel
            repository.sendMessage(prompt, modelName = modelName).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to generate response",
                        )
                    }
                },
            )
        }
    }

    fun onClearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onSelectModel(modelName: String) {
        _uiState.update { it.copy(selectedModel = modelName) }
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setSelectedModel(modelName)
            }
        }
    }

    fun onThemeModeChange(mode: String) {
        _uiState.update { it.copy(themeMode = mode) }
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setThemeMode(mode)
            }
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            userPreferencesRepository: UserPreferencesRepository? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, userPreferencesRepository) as T
        }
    }
}
