package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val historyRepository: ChatHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            configurationError = if (hasApiKey) null else MISSING_API_KEY_MESSAGE,
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Prompt behind the last failed request, so the snackbar action can resend it. */
    private var lastFailedPrompt: String? = null

    init {
        viewModelScope.launch {
            historyRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        autoSendVoice = prefs.autoSendVoice,
                        dynamicColor = prefs.dynamicColor,
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null,
            )
        }
    }

    fun onVoiceResult(spokenText: String) {
        val prompt = spokenText.trim()
        if (prompt.isEmpty()) return

        _uiState.update {
            it.copy(
                prompt = prompt,
                promptError = null,
            )
        }

        if (_uiState.value.autoSendVoice) {
            onSend()
        }
    }

    fun onSpeechUnavailable() {
        _uiState.update {
            it.copy(
                errorMessage = SPEECH_UNAVAILABLE_MESSAGE,
                errorCanRetry = false,
            )
        }
    }

    fun onMicPermissionDenied() {
        _uiState.update {
            it.copy(
                errorMessage = MIC_PERMISSION_DENIED_MESSAGE,
                errorCanRetry = false,
            )
        }
    }

    fun onToggleAutoSendVoice() {
        viewModelScope.launch {
            preferencesRepository.setAutoSendVoice(!_uiState.value.autoSendVoice)
        }
    }

    fun onToggleDynamicColor() {
        viewModelScope.launch {
            preferencesRepository.setDynamicColor(!_uiState.value.dynamicColor)
        }
    }

    fun onClearHistory() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            historyRepository.clear()
            lastFailedPrompt = null
            _uiState.update { it.copy(errorMessage = null, promptError = null) }
        }
    }

    fun onSend() {
        val state = _uiState.value
        val prompt = state.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }

        if (!state.canSend) return

        val userMessage = ChatMessage(
            id = newMessageId(),
            text = prompt,
            isUser = true,
            createdAt = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
                messages = it.messages + userMessage,
                errorCanRetry = false,
            )
        }

        viewModelScope.launch {
            historyRepository.save(userMessage)
            generate(prompt)
        }
    }

    /** Resends the prompt that failed, without adding a second copy of the user's message. */
    fun onRetry() {
        val prompt = lastFailedPrompt ?: return
        if (!_uiState.value.canSend) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                errorCanRetry = false,
            )
        }

        generate(prompt)
    }

    /** Called once the snackbar has shown the message so it is not displayed twice. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null, errorCanRetry = false) }
    }

    private fun generate(prompt: String) {
        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    lastFailedPrompt = null

                    val geminiMessage = ChatMessage(
                        id = newMessageId(),
                        text = text,
                        isUser = false,
                        createdAt = System.currentTimeMillis(),
                    )
                    historyRepository.save(geminiMessage)

                    _uiState.update { it.copy(isLoading = false) }
                },

                onFailure = { error ->
                    lastFailedPrompt = prompt

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: GENERIC_ERROR_MESSAGE,
                            errorCanRetry = true,
                        )
                    }
                },
            )
        }
    }

    private fun newMessageId(): String = UUID.randomUUID().toString()

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again."

        const val SPEECH_UNAVAILABLE_MESSAGE =
            "Speech recognition is not available on this device."

        const val MIC_PERMISSION_DENIED_MESSAGE =
            "Microphone permission is required for voice input."

        fun factory(
            repository: GeminiRepository,
            historyRepository: ChatHistoryRepository,
            preferencesRepository: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T =
                ChatViewModel(
                    repository,
                    historyRepository,
                    preferencesRepository,
                    hasApiKey,
                ) as T
        }
    }
}
