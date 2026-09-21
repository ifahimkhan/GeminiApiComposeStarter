package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ImageStore
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.speech.TextSpeaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val history: ChatHistoryRepository,
    private val preferences: UserPreferencesRepository,
    private val imageStore: ImageStore,
    private val speaker: TextSpeaker,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** The in-flight Gemini request, kept so it can be cancelled. */
    private var requestJob: Job? = null

    init {
        viewModelScope.launch {
            history.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            preferences.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            speaker.speakingId.collect { id -> _uiState.update { it.copy(speakingMessageId = id) } }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSend() {
        val state = _uiState.value
        val prompt = state.prompt.trim()
        val imagePath = state.attachedImagePath
        if (prompt.isEmpty() && imagePath == null) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (state.isLoading) return

        speaker.stop()
        _uiState.update {
            it.copy(
                prompt = "",
                attachedImagePath = null,
                promptError = null,
                isLoading = true,
                errorMessage = null,
                errorIsRetryable = false,
            )
        }
        requestJob = viewModelScope.launch {
            history.add(Author.USER, prompt, imagePath)
            fetchReply(prompt, imagePath)
        }
    }

    /** Re-sends the last prompt after a failure, without adding a duplicate bubble. */
    fun onRetry() {
        val state = _uiState.value
        if (state.isLoading || !state.canRetry) return
        val last = state.messages.last()

        _uiState.update { it.copy(isLoading = true, errorMessage = null, errorIsRetryable = false) }
        requestJob = viewModelScope.launch { fetchReply(last.text, last.imagePath) }
    }

    /** Cancels the request that is waiting for Gemini. The prompt stays in the chat. */
    fun onStopGenerating() {
        requestJob?.cancel()
        requestJob = null
        _uiState.update { it.copy(isLoading = false) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null, errorIsRetryable = false) }
    }

    /** Puts the recognised speech into the prompt box so the user can review it before sending. */
    fun onVoiceResult(text: String) {
        val spoken = text.trim()
        if (spoken.isEmpty()) return
        _uiState.update { it.copy(prompt = "${it.prompt.trimEnd()} $spoken".trim(), promptError = null) }
    }

    fun onVoiceUnavailable() = showError(SPEECH_UNAVAILABLE_MESSAGE)

    /** Copies a picked or captured picture into app storage and attaches it to the next prompt. */
    fun onImageSelected(source: String) {
        viewModelScope.launch {
            val path = imageStore.import(source)
            if (path == null) {
                showError(IMAGE_LOAD_FAILED_MESSAGE)
                return@launch
            }
            val replaced = _uiState.value.attachedImagePath
            _uiState.update { it.copy(attachedImagePath = path, promptError = null) }
            replaced?.let { imageStore.delete(it) }
        }
    }

    fun onImageRemoved() {
        val path = _uiState.value.attachedImagePath ?: return
        _uiState.update { it.copy(attachedImagePath = null) }
        viewModelScope.launch { imageStore.delete(path) }
    }

    fun onCameraUnavailable() = showError(CAMERA_UNAVAILABLE_MESSAGE)

    /** Reads a reply aloud, or stops it if it is already being read. */
    fun onSpeakMessage(id: Long) {
        if (_uiState.value.speakingMessageId == id) {
            speaker.stop()
            return
        }
        val message = _uiState.value.messages.firstOrNull { it.id == id } ?: return
        if (!speaker.speak(id, message.text.toSpeakableText())) showError(TTS_UNAVAILABLE_MESSAGE)
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun onClearConversation() {
        speaker.stop()
        val imagePaths = _uiState.value.messages.mapNotNull { it.imagePath }
        viewModelScope.launch {
            history.clear()
            imagePaths.forEach { imageStore.delete(it) }
        }
    }

    override fun onCleared() {
        speaker.shutdown()
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, errorIsRetryable = false) }
    }

    private suspend fun fetchReply(prompt: String, imagePath: String?) {
        repository.generateText(prompt, imagePath).fold(
            onSuccess = { text ->
                history.add(Author.GEMINI, text)
                _uiState.update { it.copy(isLoading = false) }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Something went wrong",
                        errorIsRetryable = true,
                    )
                }
            },
        )
    }

    /** Drops the **bold** markers so they are not read out. */
    private fun String.toSpeakableText() = replace("**", "")

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."
        const val SPEECH_UNAVAILABLE_MESSAGE =
            "Speech recognition isn't available on this device."
        const val TTS_UNAVAILABLE_MESSAGE =
            "Text-to-speech isn't available on this device."
        const val IMAGE_LOAD_FAILED_MESSAGE =
            "Couldn't load that image."
        const val CAMERA_UNAVAILABLE_MESSAGE =
            "No camera app is available on this device."

        fun factory(
            repository: GeminiRepository,
            history: ChatHistoryRepository,
            preferences: UserPreferencesRepository,
            imageStore: ImageStore,
            speaker: TextSpeaker,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(repository, history, preferences, imageStore, speaker, hasApiKey) as T
        }
    }
}
