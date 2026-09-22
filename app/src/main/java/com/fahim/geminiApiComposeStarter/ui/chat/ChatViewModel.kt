package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.prefs.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.mascot.MascotState
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val preferences: UserPreferencesRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    /** State the ViewModel owns outright; persisted state arrives from the two repositories. */
    private data class Transient(
        val prompt: String = "",
        val isLoading: Boolean = false,
        val promptError: PromptError? = null,
        val errorMessage: String? = null,
        val mascotState: MascotState = MascotState.IDLE,
    )

    private val transient = MutableStateFlow(Transient())

    /** Drives the timed return from a reaction to IDLE, and the drift into SLEEPY. */
    private var mascotJob: Job? = null

    init {
        scheduleSleep()
    }

    val uiState: StateFlow<ChatUiState> =
        combine(history.messages, preferences.preferences, transient) { messages, prefs, local ->
            ChatUiState(
                messages = messages,
                prompt = local.prompt,
                isLoading = local.isLoading,
                promptError = local.promptError,
                errorMessage = local.errorMessage,
                mascotState = local.mascotState,
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ChatUiState(),
        )

    fun onPromptChange(value: String) {
        transient.update { it.copy(prompt = value, promptError = null) }
        // Typing wakes the character up without pulling it out of a reaction it is mid-way
        // through, and restarts the idle countdown.
        if (transient.value.mascotState == MascotState.SLEEPY) {
            setMascot(MascotState.IDLE)
        }
        scheduleSleep()
    }

    fun onSend() {
        val prompt = transient.value.prompt.trim()
        if (prompt.isEmpty()) {
            transient.update { it.copy(promptError = PromptError.EMPTY) }
            reactThenIdle(MascotState.CONFUSED, REACTION_MILLIS)
            return
        }
        if (!hasApiKey) {
            transient.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            reactThenIdle(MascotState.CONFUSED, REACTION_MILLIS)
            return
        }
        if (transient.value.isLoading) return

        transient.update {
            it.copy(prompt = "", isLoading = true, errorMessage = null, promptError = null)
        }
        setMascot(MascotState.THINKING)

        viewModelScope.launch {
            // Snapshot the conversation *before* the new prompt is stored, so it becomes the
            // replayed history rather than being sent twice.
            val priorTurns = history.messages.first()
            val isFirstTurn = priorTurns.isEmpty()
            history.append(ChatMessage(text = prompt, author = Author.USER))

            repository.generateText(prompt, priorTurns).fold(
                onSuccess = { text ->
                    history.append(ChatMessage(text = text, author = Author.GEMINI))
                    transient.update { it.copy(isLoading = false) }
                    // A long answer, or the first answer of the conversation, earns the bigger
                    // reaction; everything else gets a small happy bounce.
                    val reaction = if (isFirstTurn || text.length > LONG_RESPONSE_CHARS) {
                        MascotState.EXCITED
                    } else {
                        MascotState.HAPPY
                    }
                    reactThenIdle(reaction, REACTION_MILLIS)
                },
                onFailure = { error ->
                    transient.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                    reactThenIdle(MascotState.CONFUSED, REACTION_MILLIS)
                },
            )
        }
    }

    /** Called once the snackbar has been shown, so it is not re-shown on the next recomposition. */
    fun onErrorShown() {
        transient.update { it.copy(errorMessage = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch { history.clear() }
        reactThenIdle(MascotState.SURPRISED, REACTION_MILLIS)
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColor(enabled) }
    }

    /**
     * Sets the mascot's expression directly. Exposed so any future feature (a greeting, a
     * long-press easter egg, a tutorial step) can drive the character without going through the
     * chat flow.
     */
    fun setMascot(state: MascotState) {
        mascotJob?.cancel()
        transient.update { it.copy(mascotState = state) }
    }

    private fun reactThenIdle(state: MascotState, millis: Long) {
        mascotJob?.cancel()
        transient.update { it.copy(mascotState = state) }
        mascotJob = viewModelScope.launch {
            delay(millis)
            transient.update { it.copy(mascotState = MascotState.IDLE) }
            scheduleSleep()
        }
    }

    private fun scheduleSleep() {
        mascotJob?.cancel()
        mascotJob = viewModelScope.launch {
            delay(SLEEP_AFTER_MILLIS)
            transient.update { it.copy(mascotState = MascotState.SLEEPY) }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val REACTION_MILLIS = 2_500L
        private const val SLEEP_AFTER_MILLIS = 60_000L
        private const val LONG_RESPONSE_CHARS = 400

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            history: ChatHistoryRepository,
            preferences: UserPreferencesRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, preferences, hasApiKey) as T
        }
    }
}
