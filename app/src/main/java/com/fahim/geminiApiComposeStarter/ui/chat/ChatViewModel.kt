package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemePreference
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val preferencesRepository: UserPreferencesRepository,
    private val historyRepository: ChatHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(isLoading = true)
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var historyLoaded = false

    init {
        viewModelScope.launch {
            preferencesRepository.themePreference.collect { theme ->
                _uiState.update {
                    it.copy(themePreference = theme)
                }
            }
        }

        viewModelScope.launch {
            try {
                val savedMessages = historyRepository.loadMessages()
                historyLoaded = true

                _uiState.update {
                    it.copy(
                        messages = savedMessages,
                        isLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            "Could not load chat history. Restart the app."
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update {
            it.copy(prompt = value, promptError = null)
        }
    }

    fun onErrorShown() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun onVoiceError(message: String) {
        _uiState.update {
            it.copy(errorMessage = message)
        }
    }

    fun onThemeChange(theme: ThemePreference) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveTheme(theme)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Could not save your theme preference."
                    )
                }
            }
        }
    }

    fun onSend() {
        if (_uiState.value.isLoading) return

        if (!historyLoaded) {
            _uiState.update {
                it.copy(
                    errorMessage =
                        "Chat history is unavailable. Restart the app."
                )
            }
            return
        }

        val prompt = _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(promptError = PromptError.EMPTY)
            }
            return
        }

        if (!hasApiKey) {
            _uiState.update {
                it.copy(errorMessage = MISSING_API_KEY_MESSAGE)
            }
            return
        }

        val userMessage = ChatMessage(
            text = prompt,
            isUser = true
        )

        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMessage,
                isLoading = true,
                promptError = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = try {
                repository.generateText(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure<String>(
                    IllegalStateException("Could not get a response.")
                )
            }

            val failure = result.exceptionOrNull()

            if (failure != null) {
                _uiState.update {
                    it.copy(
                        prompt = prompt,
                        isLoading = false,
                        errorMessage = failure.message
                            ?: "Could not get a response.",
                        messages = it.messages.filterNot { message ->
                            message.id == userMessage.id
                        }
                    )
                }
                return@launch
            }

            val assistantMessage = ChatMessage(
                text = result.getOrThrow(),
                isUser = false
            )

            var storageError: String? = null

            try {
                historyRepository.saveExchange(
                    userMessage = userMessage,
                    assistantMessage = assistantMessage
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                storageError =
                    "Reply received, but this exchange could not be saved."
            }

            _uiState.update {
                it.copy(
                    messages = it.messages + assistantMessage,
                    isLoading = false,
                    errorMessage = storageError
                )
            }
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            preferencesRepository: UserPreferencesRepository,
            historyRepository: ChatHistoryRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                ): T {
                    require(
                        modelClass.isAssignableFrom(ChatViewModel::class.java)
                    ) {
                        "Unknown ViewModel class."
                    }

                    return ChatViewModel(
                        repository = repository,
                        hasApiKey = hasApiKey,
                        preferencesRepository = preferencesRepository,
                        historyRepository = historyRepository
                    ) as T
                }
            }
    }
}