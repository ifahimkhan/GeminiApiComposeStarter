package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.SafetyBlockedException
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatViewModel(
    private val repository: GeminiRepository,
    private val apiKeyStorage: ApiKeyStorage? = null,
    hasStaticApiKey: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            isApiKeyConfigured = apiKeyStorage?.hasApiKey() == true || hasStaticApiKey
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        val error = when {
            value.length > MAX_PROMPT_LENGTH -> PromptError.TOO_LONG
            else -> null
        }
        _uiState.update { it.copy(prompt = value, promptError = error) }
    }

    fun onOpenApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    fun onDismissApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun onSaveApiKey(newKey: String) {
        val trimmed = newKey.trim()
        if (trimmed.isNotEmpty()) {
            apiKeyStorage?.saveApiKey(trimmed)
            _uiState.update {
                it.copy(
                    isApiKeyConfigured = true,
                    showApiKeyDialog = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onClearApiKey() {
        apiKeyStorage?.clearApiKey()
        _uiState.update {
            it.copy(
                isApiKeyConfigured = apiKeyStorage?.hasApiKey() == true,
                showApiKeyDialog = false
            )
        }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (prompt.length > MAX_PROMPT_LENGTH) {
            _uiState.update { it.copy(promptError = PromptError.TOO_LONG) }
            return
        }
        if (!_uiState.value.isApiKeyConfigured) {
            _uiState.update {
                it.copy(
                    errorMessage = MISSING_API_KEY_MESSAGE,
                    showApiKeyDialog = true
                )
            }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null, promptError = null) }
        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    _uiState.update { it.copy(isLoading = false, response = text) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error),
                        )
                    }
                },
            )
        }
    }

    private fun sanitizeErrorMessage(error: Throwable): String {
        return when {
            error is MissingApiKeyException ->
                "Gemini API key is not configured. Please set it in settings."
            error is SafetyBlockedException ->
                "Response was blocked by content safety filters."
            error is UnknownHostException || error is SocketTimeoutException ->
                "Unable to connect to the Gemini service. Please check your network connection."
            error.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
            error.message?.contains("API key not valid", ignoreCase = true) == true ->
                "Invalid API key. Please check your Gemini API key in settings."
            error.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ||
            error.message?.contains("429", ignoreCase = true) == true ->
                "Quota or rate limit exceeded. Please wait a moment and try again."
            else ->
                "An unexpected error occurred while communicating with Gemini. Please try again."
        }
    }

    companion object {
        const val MAX_PROMPT_LENGTH = 4000
        const val MISSING_API_KEY_MESSAGE =
            "Please configure your Gemini API key to continue."

        fun factory(
            repository: GeminiRepository,
            apiKeyStorage: ApiKeyStorage? = null,
            hasStaticApiKey: Boolean = false,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(
                    repository = repository,
                    apiKeyStorage = apiKeyStorage,
                    hasStaticApiKey = hasStaticApiKey
                ) as T
        }
    }
}
