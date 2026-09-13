package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.ChatStore
import com.fahim.geminiApiComposeStarter.data.ConversationMessage
import com.fahim.geminiApiComposeStarter.data.ConversationRole
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.StoredChat
import com.fahim.geminiApiComposeStarter.data.StoredMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatStore: ChatStore,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private var storedChats: List<StoredChat> = chatStore.loadChats().ifEmpty { listOf(ChatStore.newChat()) }
    private val initialActiveChatId = chatStore.loadActiveChatId()
        ?.takeIf { activeId -> storedChats.any { it.id == activeId } }
        ?: storedChats.first().id
    private val _uiState = MutableStateFlow(storedChats.toUiState(initialActiveChatId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var nextMessageId = storedChats.maxOfOrNull { chat ->
        chat.messages.maxOfOrNull { message -> message.id } ?: -1L
    }?.plus(1L) ?: 0L

    init {
        saveChats()
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onNewChat() {
        if (_uiState.value.isLoading) return
        val newChat = ChatStore.newChat()
        storedChats = listOf(newChat) + storedChats
        _uiState.value = storedChats.toUiState(newChat.id)
        saveChats()
    }

    fun onSelectChat(chatId: String) {
        if (_uiState.value.isLoading || chatId == _uiState.value.activeChatId) return
        if (storedChats.none { it.id == chatId }) return
        _uiState.value = storedChats.toUiState(chatId)
        saveChats()
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

        val history = _uiState.value.messages.map { it.toConversationMessage() }
        val userMessage = ChatMessage(
            id = nextMessageId++,
            text = prompt,
            author = ChatAuthor.USER,
        )
        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }
        persistActiveChat()
        viewModelScope.launch {
            var streamedAnyText = false
            var failed = false
            val geminiMessageId = nextMessageId++

            repository.generateTextStream(prompt, history).collect { result ->
                result.fold(
                    onSuccess = { chunk ->
                        streamedAnyText = true
                        _uiState.update { state ->
                            val existingMessage = state.messages.firstOrNull { it.id == geminiMessageId }
                            val messages = if (existingMessage == null) {
                                state.messages + ChatMessage(
                                    id = geminiMessageId,
                                    text = chunk,
                                    author = ChatAuthor.GEMINI,
                                )
                            } else {
                                state.messages.map { message ->
                                    if (message.id == geminiMessageId) {
                                        message.copy(text = message.text + chunk)
                                    } else {
                                        message
                                    }
                                }
                            }
                            state.copy(messages = messages)
                        }
                        persistActiveChat()
                    },
                    onFailure = { error ->
                        failed = true
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Gemini request failed.",
                            )
                        }
                        persistActiveChat()
                    },
                )
            }

            if (!failed) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (streamedAnyText) null else "Empty response from Gemini",
                    )
                }
                persistActiveChat()
            }
        }
    }

    private fun persistActiveChat() {
        val state = _uiState.value
        val title = storedChats.firstOrNull { it.id == state.activeChatId }?.title
            ?.takeUnless { it == "New chat" }
            ?: state.messages.firstOrNull { it.author == ChatAuthor.USER }?.text?.toChatTitle()
            ?: "New chat"
        storedChats = storedChats.map { chat ->
            if (chat.id == state.activeChatId) {
                chat.copy(
                    title = title,
                    messages = state.messages.map { it.toStoredMessage() },
                )
            } else {
                chat
            }
        }
        _uiState.update { it.copy(chatSummaries = storedChats.toSummaries()) }
        saveChats()
    }

    private fun saveChats() {
        chatStore.saveChats(storedChats, _uiState.value.activeChatId)
    }

    private fun ChatMessage.toConversationMessage(): ConversationMessage = ConversationMessage(
        role = when (author) {
            ChatAuthor.USER -> ConversationRole.USER
            ChatAuthor.GEMINI -> ConversationRole.MODEL
        },
        text = text,
    )

    private fun ChatMessage.toStoredMessage(): StoredMessage = StoredMessage(
        id = id,
        text = text,
        role = when (author) {
            ChatAuthor.USER -> ConversationRole.USER
            ChatAuthor.GEMINI -> ConversationRole.MODEL
        },
    )

    private fun StoredMessage.toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        author = when (role) {
            ConversationRole.USER -> ChatAuthor.USER
            ConversationRole.MODEL -> ChatAuthor.GEMINI
        },
    )

    private fun List<StoredChat>.toUiState(activeChatId: String): ChatUiState {
        val activeChat = first { it.id == activeChatId }
        return ChatUiState(
            activeChatId = activeChat.id,
            chatSummaries = toSummaries(),
            messages = activeChat.messages.map { it.toChatMessage() },
        )
    }

    private fun List<StoredChat>.toSummaries(): List<ChatSummary> = map { chat ->
        ChatSummary(id = chat.id, title = chat.title)
    }

    private fun String.toChatTitle(): String {
        val normalized = trim().replace(Regex("\\s+"), " ")
        return if (normalized.length <= 32) normalized else normalized.take(29).trimEnd() + "..."
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: GeminiRepository, chatStore: ChatStore, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, chatStore, hasApiKey) as T
            }
    }
}
