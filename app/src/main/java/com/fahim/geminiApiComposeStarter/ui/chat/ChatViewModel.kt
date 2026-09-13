package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fahim.geminiApiComposeStarter.GeminiApp
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MISSING_API_KEY_MESSAGE
import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferences
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val GENERIC_ERROR = "Something went wrong"

class ChatViewModel(
    private val gemini: GeminiRepository,
    private val chatHistory: ChatHistoryRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** State that belongs to this screen only; the messages themselves live in Room. */
    private data class Transient(
        val prompt: String = "",
        val isLoading: Boolean = false,
        val promptError: PromptError? = null,
        val error: ErrorEvent? = null,
        val lastFailedPrompt: String? = null,
    )

    private val transient = MutableStateFlow(Transient())
    private var errorCounter = 0L

    val uiState: StateFlow<ChatUiState> = combine(
        chatHistory.messages,
        preferences.preferences,
        transient,
    ) { messages, prefs, local ->
        ChatUiState(
            prompt = local.prompt,
            messages = messages,
            isLoading = local.isLoading,
            promptError = local.promptError,
            error = local.error,
            themeMode = prefs.themeMode,
            dynamicColour = prefs.dynamicColour,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChatUiState(),
    )

    /**
     * Exposed separately so the theme at the root of the Activity only recomposes when the
     * theme actually changes, not on every keystroke in the prompt field.
     */
    val themePreferences: StateFlow<UserPreferences> = preferences.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    fun onPromptChange(value: String) {
        transient.update { it.copy(prompt = value, promptError = null) }
    }

    /** Appends a speech-to-text result so the user can review or edit it before sending. */
    fun onVoiceResult(text: String) {
        val spoken = text.trim()
        if (spoken.isEmpty()) return
        transient.update { current ->
            val merged = if (current.prompt.isBlank()) spoken else "${current.prompt.trimEnd()} $spoken"
            current.copy(prompt = merged, promptError = null)
        }
    }

    fun onSend() {
        val prompt = transient.value.prompt.trim()
        if (prompt.isEmpty()) {
            transient.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (transient.value.isLoading) return

        transient.update {
            it.copy(
                prompt = "",
                promptError = null,
                error = null,
                isLoading = true,
                lastFailedPrompt = null,
            )
        }
        viewModelScope.launch { send(prompt, recordUserTurn = true) }
    }

    /** Re-sends the prompt that failed. The user turn is already in history, so it is not re-added. */
    fun onRetry() {
        val prompt = transient.value.lastFailedPrompt ?: return
        if (transient.value.isLoading) return
        transient.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { send(prompt, recordUserTurn = false) }
    }

    fun onClearConversation() {
        viewModelScope.launch {
            chatHistory.clear()
            transient.update { it.copy(error = null, lastFailedPrompt = null) }
        }
    }

    /** For failures raised on the UI side, e.g. no speech recogniser or a denied permission. */
    fun onLocalError(message: String) {
        transient.update {
            it.copy(error = ErrorEvent(id = ++errorCounter, message = message, retryable = false))
        }
    }

    fun onErrorShown() {
        transient.update { it.copy(error = null) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun onDynamicColourChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColour(enabled) }
    }

    private suspend fun send(prompt: String, recordUserTurn: Boolean) {
        // Captured before the new turn is written so the request carries only prior context.
        val priorHistory = uiState.value.messages
        if (recordUserTurn) chatHistory.append(prompt, Author.USER)

        gemini.generateReply(priorHistory, prompt).fold(
            onSuccess = { reply ->
                chatHistory.append(reply, Author.MODEL)
                transient.update { it.copy(isLoading = false, lastFailedPrompt = null) }
            },
            onFailure = { failure ->
                transient.update {
                    it.copy(
                        isLoading = false,
                        lastFailedPrompt = prompt,
                        error = ErrorEvent(
                            id = ++errorCounter,
                            message = failure.toUserMessage(),
                            retryable = failure !is MissingApiKeyException,
                        ),
                    )
                }
            },
        )
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is MissingApiKeyException -> MISSING_API_KEY_MESSAGE
        else -> message?.takeIf { it.isNotBlank() } ?: GENERIC_ERROR
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GeminiApp
                ChatViewModel(
                    gemini = app.container.geminiRepository,
                    chatHistory = app.container.chatHistoryRepository,
                    preferences = app.container.userPreferencesRepository,
                )
            }
        }
    }
}
