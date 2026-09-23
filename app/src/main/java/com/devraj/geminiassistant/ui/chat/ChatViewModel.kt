package com.devraj.geminiassistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devraj.geminiassistant.data.GeminiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeGenerationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getMessagesFlow().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            repository.getSystemInstructionFlow().collect { instruction ->
                _uiState.update { it.copy(systemInstruction = instruction) }
            }
        }
        viewModelScope.launch {
            repository.getTemperatureFlow().collect { temp ->
                _uiState.update { it.copy(temperature = temp) }
            }
        }
        viewModelScope.launch {
            repository.getSelectedModelFlow().collect { model ->
                _uiState.update { it.copy(selectedModel = model) }
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onVoiceResult(spokenText: String) {
        if (spokenText.isNotBlank()) {
            _uiState.update { it.copy(prompt = spokenText, promptError = null) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open, searchQuery = if (!open) "" else it.searchQuery) }
    }

    fun setSpeakingMessageId(id: Long?) {
        _uiState.update { it.copy(speakingMessageId = id) }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _uiState.update { it.copy(isLoading = false, streamingResponse = "") }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                prompt = "",
                streamingResponse = "",
                isLoading = true,
                errorMessage = null,
                promptError = null
            )
        }

        val startTime = System.currentTimeMillis()
        activeGenerationJob = viewModelScope.launch {
            try {
                // Try streaming generation for rich real-time typing effect
                val streamFlow = repository.generateStream(prompt)
                val buffer = StringBuilder()
                streamFlow
                    .catch { error ->
                        val duration = System.currentTimeMillis() - startTime
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                streamingResponse = "",
                                lastLatencyMs = duration,
                                errorMessage = error.localizedMessage ?: "Failed to get response"
                            )
                        }
                    }
                    .collect { chunk ->
                        buffer.append(chunk)
                        _uiState.update {
                            it.copy(
                                streamingResponse = buffer.toString(),
                                response = buffer.toString()
                            )
                        }
                    }

                val duration = System.currentTimeMillis() - startTime
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        streamingResponse = "",
                        lastLatencyMs = duration
                    )
                }
            } catch (e: Exception) {
                // Fallback to standard generateText if stream is not supported by fake or network
                val result = repository.generateText(prompt)
                val duration = System.currentTimeMillis() - startTime
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            streamingResponse = "",
                            lastLatencyMs = duration,
                            errorMessage = error.localizedMessage ?: "Failed to get response"
                        )
                    }
                }.onSuccess { text ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            streamingResponse = "",
                            response = text,
                            lastLatencyMs = duration
                        )
                    }
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun saveSettings(instruction: String, temperature: Float, model: String = "gemini-3.6-flash") {
        viewModelScope.launch {
            repository.updatePreferences(instruction, temperature, model)
            _uiState.update { it.copy(isSettingsOpen = false) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Please add it to local.properties."

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}

