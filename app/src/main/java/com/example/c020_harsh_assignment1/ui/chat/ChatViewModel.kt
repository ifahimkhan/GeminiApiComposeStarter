package com.example.c020_harsh_assignment1.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c020_harsh_assignment1.data.GeminiRepository
import com.example.c020_harsh_assignment1.data.GeminiRepositoryImpl
import com.example.c020_harsh_assignment1.data.PreferencesRepository
import com.example.c020_harsh_assignment1.data.local.ChatDao
import com.example.c020_harsh_assignment1.data.local.ChatMessage
import com.example.c020_harsh_assignment1.security.CryptoManager
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatDao: ChatDao,
    private val preferencesRepository: PreferencesRepository,
    private val cryptoManager: CryptoManager,
    private val buildConfigApiKey: String,
    private val repositoryFactory: (String) -> GeminiRepository = { GeminiRepositoryImpl(it) }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var repository: GeminiRepository? = null

    init {
        loadMessages()
        loadPreferences()
        initializeSecurity()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatDao.getAllMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.responseStyle.collect { style ->
                _uiState.update { it.copy(responseStyle = style) }
            }
        }
    }

    private fun initializeSecurity() {
        viewModelScope.launch {
            val encryptedKey = preferencesRepository.encryptedApiKey.first()
            val apiKey = if (encryptedKey == null) {
                if (buildConfigApiKey.isNotBlank()) {
                    try {
                        val encrypted = cryptoManager.encrypt(buildConfigApiKey.toByteArray())
                        val base64Encrypted = cryptoManager.encodeToString(encrypted)
                        preferencesRepository.saveEncryptedApiKey(base64Encrypted)
                        buildConfigApiKey
                    } catch (e: Exception) {
                        // Ignore encryption errors for buildConfig key
                        ""
                    }
                } else ""
            } else {
                try {
                    val encryptedBytes = cryptoManager.decodeFromString(encryptedKey)
                    val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
                    String(decryptedBytes)
                } catch (e: Exception) {
                    // Decryption failed, likely a keystore issue or modified storage
                    ""
                }
            }

            if (apiKey.isNotBlank()) {
                repository = repositoryFactory(apiKey)
            } else {
                _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
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

    fun onVoiceResult(text: String) {
        _uiState.update {
            it.copy(
                prompt = text,
                promptError = null
            )
        }
    }

    fun onSend() {
        val prompt = _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(promptError = PromptError.EMPTY)
            }
            return
        }

        if (repository == null) {
            _uiState.update {
                it.copy(
                    errorMessage = MISSING_API_KEY_MESSAGE
                )
            }
            return
        }

        if (_uiState.value.isLoading) return

        val style = _uiState.value.responseStyle
        val enhancedPrompt = if (style != "Simple") {
            "Respond in a $style style: $prompt"
        } else prompt

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                promptError = null,
                prompt = "" // Clear input
            )
        }

        viewModelScope.launch {
            // Prepare history for Gemini (from current messages in state)
            val history = _uiState.value.messages.map {
                content(role = if (it.isFromUser) "user" else "model") {
                    text(it.text)
                }
            }

            // Save user message to Room
            val userMessage = ChatMessage(text = prompt, isFromUser = true)
            chatDao.insertMessage(userMessage)

            repository?.generateText(enhancedPrompt, history)?.fold(
                onSuccess = { text ->
                    val geminiMessage = ChatMessage(text = text, isFromUser = false)
                    chatDao.insertMessage(geminiMessage)
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went wrong",
                        )
                    }
                }
            )
        }
    }

    fun onStyleChange(style: String) {
        viewModelScope.launch {
            preferencesRepository.saveResponseStyle(style)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatDao.clearHistory()
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing or invalid. Add it to local.properties and rebuild."

        fun factory(
            chatDao: ChatDao,
            preferencesRepository: PreferencesRepository,
            cryptoManager: CryptoManager,
            buildConfigApiKey: String,
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T {
                return ChatViewModel(
                    chatDao = chatDao,
                    preferencesRepository = preferencesRepository,
                    cryptoManager = cryptoManager,
                    buildConfigApiKey = buildConfigApiKey,
                ) as T
            }
        }
    }
}
