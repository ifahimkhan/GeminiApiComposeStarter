package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.fahim.geminiApiComposeStarter.data.ChatDao
import com.fahim.geminiApiComposeStarter.data.ChatEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.util.UUID


class ChatViewModel(
    private val repository: GeminiRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val chatDao: ChatDao,
    private val hasApiKey: Boolean
) : ViewModel() {

    // ==========================================
    // UI STATE
    // ==========================================

    private val _uiState = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> =
        _uiState.asStateFlow()


    // ==========================================
    // INITIALIZATION
    // ==========================================

    init {

        // Load saved response style from DataStore
        viewModelScope.launch {

            preferencesRepository.responseStyle.collect { style ->

                _uiState.update {
                    it.copy(
                        responseStyle = style
                    )
                }
            }
        }

        // Load chat history from Room
        viewModelScope.launch {

            chatDao.observeMessages().collect { savedMessages ->

                val messages = savedMessages.map { entity ->

                    ChatMessage(
                        id = entity.messageId,
                        text = entity.text,
                        isUser = entity.isUser
                    )
                }

                _uiState.update {
                    it.copy(
                        messages = messages
                    )
                }
            }
        }
    }


    // ==========================================
    // PROMPT CHANGE
    // ==========================================

    fun onPromptChange(value: String) {

        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null
            )
        }
    }


    // ==========================================
    // RESPONSE STYLE CHANGE
    // ==========================================

    fun onResponseStyleChange(style: String) {

        _uiState.update {
            it.copy(
                responseStyle = style
            )
        }

        // Save selected style in DataStore
        viewModelScope.launch {

            preferencesRepository.setResponseStyle(style)
        }
    }


    // ==========================================
    // SEND MESSAGE
    // ==========================================

    fun onSend() {

        val currentState = _uiState.value
        val prompt = currentState.prompt.trim()

        // Check for empty prompt
        if (prompt.isEmpty()) {

            _uiState.update {
                it.copy(
                    promptError = PromptError.EMPTY
                )
            }

            return
        }

        // Check API key
        if (!hasApiKey) {

            _uiState.update {
                it.copy(
                    errorMessage = MISSING_API_KEY_MESSAGE
                )
            }

            return
        }

        // Prevent multiple simultaneous requests
        if (currentState.isLoading) {
            return
        }

        // Create user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = prompt,
            isUser = true
        )

        // Update UI immediately
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                promptError = null,
                prompt = ""
            )
        }

        // Prepare response style instruction
        val styleInstruction = when (currentState.responseStyle) {

            UserPreferencesRepository.CONCISE ->
                "Answer briefly and concisely."

            UserPreferencesRepository.DETAILED ->
                "Give a detailed explanation with examples."

            else ->
                "Give a normal, clear explanation."
        }

        // Prepare final Gemini prompt
        val finalPrompt =
            "$styleInstruction\n\nUser question:\n$prompt"

        // Save message and generate response
        viewModelScope.launch {

            try {

                // Save user message in Room
                chatDao.insertMessage(
                    ChatEntity(
                        messageId = userMessage.id,
                        text = userMessage.text,
                        isUser = true,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Generate Gemini response
                val result = repository.generateText(finalPrompt)

                result.fold(

                    onSuccess = { text ->

                        val assistantMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = text,
                            isUser = false
                        )

                        // Save Gemini response in Room
                        chatDao.insertMessage(
                            ChatEntity(
                                messageId = assistantMessage.id,
                                text = assistantMessage.text,
                                isUser = false,
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                response = text,
                                errorMessage = null
                            )
                        }
                    },

                    onFailure = { error ->

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message
                                    ?: "Something went wrong."
                            )
                        }
                    }
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error: ${e.message}"
                            ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }


    // ==========================================
    // CLEAR CHAT HISTORY
    // ==========================================

    fun clearChatHistory() {

        viewModelScope.launch {

            try {

                chatDao.deleteAllMessages()

                _uiState.update {
                    it.copy(
                        messages = emptyList(),
                        response = "",
                        errorMessage = null
                    )
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        errorMessage = "Could not clear chat history."
                    )
                }
            }
        }
    }


    // ==========================================
    // EXPORT CHAT HISTORY
    // ==========================================

    suspend fun exportChatHistory(): String {

        val messages = chatDao.getAllMessages()

        if (messages.isEmpty()) {
            return "No chat history available."
        }

        return buildString {

            appendLine("Gemini Chat History")
            appendLine("===================")
            appendLine()

            messages.forEach { message ->

                val sender = if (message.isUser) {
                    "You"
                } else {
                    "Gemini"
                }

                appendLine("$sender:")
                appendLine(message.text)
                appendLine()
            }
        }
    }


    // ==========================================
    // CONSTANTS AND VIEWMODEL FACTORY
    // ==========================================

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."


        fun factory(
            repository: GeminiRepository,
            preferencesRepository: UserPreferencesRepository,
            chatDao: ChatDao,
            hasApiKey: Boolean
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {

                        return ChatViewModel(
                            repository = repository,
                            preferencesRepository = preferencesRepository,
                            chatDao = chatDao,
                            hasApiKey = hasApiKey
                        ) as T
                    }

                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.name}"
                    )
                }
            }
        }
    }
}