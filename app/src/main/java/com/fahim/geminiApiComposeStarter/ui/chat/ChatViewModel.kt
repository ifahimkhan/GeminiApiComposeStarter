package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ConversationEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
    private val historyRepository: ChatHistoryRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null

    private var currentConversationId: Long? = null

    init {
        observeConversations()
    }

    // ---------------------------------------------------------
    // CONVERSATIONS
    // ---------------------------------------------------------

    private fun observeConversations() {
        val history = historyRepository ?: return

        viewModelScope.launch {
            history.observeConversations().collect { conversations ->

                _uiState.update {
                    it.copy(
                        conversations = conversations
                    )
                }

                // Automatically open the newest conversation
                if (currentConversationId == null && conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
                }

                // If there are no conversations, create one
                if (conversations.isEmpty() && currentConversationId == null) {
                    createNewChat()
                }
            }
        }
    }

    fun createNewChat() {
        val history = historyRepository ?: return

        viewModelScope.launch {
            val conversationId = history.createConversation()

            currentConversationId = conversationId

            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    prompt = "",
                    errorMessage = null,
                    promptError = null
                )
            }

            observeMessages(conversationId)
        }
    }

    fun selectConversation(conversationId: Long) {

        if (currentConversationId == conversationId) {
            return
        }

        currentConversationId = conversationId

        _uiState.update {
            it.copy(
                messages = emptyList(),
                prompt = "",
                errorMessage = null,
                promptError = null
            )
        }

        observeMessages(conversationId)
    }

    private fun observeMessages(conversationId: Long) {
        val history = historyRepository ?: return

        viewModelScope.launch {

            history.observeMessages(conversationId).collect { messages ->

                // Only update the UI if this is still the selected chat
                if (currentConversationId == conversationId) {
                    _uiState.update {
                        it.copy(
                            messages = messages
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // PROMPT
    // ---------------------------------------------------------

    fun onPromptChange(value: String) {

        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null,
                errorMessage = null
            )
        }
    }

    // ---------------------------------------------------------
    // SEND MESSAGE
    // ---------------------------------------------------------

    fun onSend() {

        if (_uiState.value.isLoading) {
            return
        }

        val prompt = _uiState.value.prompt.trim()

        // Empty prompt
        if (prompt.isEmpty()) {

            _uiState.update {
                it.copy(
                    promptError = PromptError.EMPTY
                )
            }

            return
        }

        // API key missing
        if (!hasApiKey) {

            _uiState.update {
                it.copy(
                    errorMessage = MISSING_API_KEY_MESSAGE
                )
            }

            return
        }

        requestJob?.cancel()

        requestJob = viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    prompt = "",
                    promptError = null,
                    errorMessage = null
                )
            }

            val userMessage = ChatMessage(
                text = prompt,
                isUser = true
            )

            // Save user message
            historyRepository?.let { history ->

                var conversationId = currentConversationId

                // If there isn't a chat yet, create one
                if (conversationId == null) {

                    conversationId =
                        history.createConversation()

                    currentConversationId = conversationId

                    observeMessages(conversationId)
                }

                history.saveMessage(
                    conversationId = conversationId,
                    message = userMessage
                )
            }

            // Call Gemini
            repository.generateText(prompt).fold(

                onSuccess = { responseText ->

                    val geminiMessage = ChatMessage(
                        text = responseText,
                        isUser = false
                    )

                    historyRepository?.let { history ->

                        val conversationId =
                            currentConversationId

                        if (conversationId != null) {

                            history.saveMessage(
                                conversationId = conversationId,
                                message = geminiMessage
                            )
                        }
                    }

                    // If Room isn't being used, update locally
                    if (historyRepository == null) {

                        _uiState.update {
                            it.copy(
                                messages =
                                    it.messages + userMessage + geminiMessage
                            )
                        }
                    }
                },

                onFailure = { error ->

                    _uiState.update {
                        it.copy(
                            errorMessage =
                                getErrorMessage(error)
                        )
                    }
                }
            )

            _uiState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    // ---------------------------------------------------------
    // CANCEL
    // ---------------------------------------------------------

    fun cancelRequest() {

        requestJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = false
            )
        }
    }

    // ---------------------------------------------------------
    // CLEAR ERROR
    // ---------------------------------------------------------

    fun clearError() {

        _uiState.update {
            it.copy(
                errorMessage = null,
                promptError = null
            )
        }
    }

    // ---------------------------------------------------------
    // ERROR HANDLING
    // ---------------------------------------------------------

    private fun getErrorMessage(error: Throwable): String {

        val message =
            error.message.orEmpty()

        return when {

            message.contains(
                "quota",
                ignoreCase = true
            ) ->
                "Gemini API quota exceeded. Please try again later."

            message.contains(
                "401",
                ignoreCase = true
            ) ||
                    message.contains(
                        "403",
                        ignoreCase = true
                    ) ->
                "Gemini API authentication failed. Check your API key."

            message.contains(
                "network",
                ignoreCase = true
            ) ||
                    message.contains(
                        "timeout",
                        ignoreCase = true
                    ) ->
                "Network error. Please check your internet connection."

            else ->
                "Something went wrong. Please try again."
        }
    }

    // ---------------------------------------------------------
    // VIEWMODEL CLEANUP
    // ---------------------------------------------------------

    override fun onCleared() {

        requestJob?.cancel()

        super.onCleared()
    }

    // ---------------------------------------------------------
    // FACTORY
    // ---------------------------------------------------------

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
            historyRepository: ChatHistoryRepository? = null
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    return ChatViewModel(
                        repository = repository,
                        hasApiKey = hasApiKey,
                        historyRepository = historyRepository
                    ) as T
                }
            }
        }
    }
}