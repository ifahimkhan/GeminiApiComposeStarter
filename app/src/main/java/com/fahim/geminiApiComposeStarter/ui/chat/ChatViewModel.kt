package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class ChatViewModel(private val repository: GeminiRepository, private val store: ChatStore, private val preferenceStore: PreferenceStore, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(ChatUiState(
        conversationId = saved["conversation"], prompt = saved["prompt"] ?: "",
        destination = Destination.entries.firstOrNull { it.name == saved.get<String>("destination") } ?: Destination.HOME,
        category = Category.entries.firstOrNull { it.name == saved.get<String>("category") } ?: Category.GENERAL,
    ))
    val uiState: StateFlow<ChatUiState> = mutable.asStateFlow()
    private var messageJob: Job? = null
    private var searchJob: Job? = null
    private var requestJob: Job? = null
    private var preferenceJob: Job? = null
    private var voiceOrigin = Destination.HOME
    init {
        mutable.value.conversationId?.let(::observeMessages)
        viewModelScope.launch { mutable.collect {
            saved["conversation"] = it.conversationId; saved["prompt"] = it.prompt
            saved["destination"] = it.destination.name; saved["category"] = it.category.name
        } }
        viewModelScope.launch {
            try { store.recoverInterrupted() } catch (_: Exception) { error("Could not restore your history. Please reopen the app.") }
            mutable.update { it.copy(isInitializing = false) }
        }
        viewModelScope.launch { store.conversations().catch { error("Could not load your conversations.") }.collect { value -> mutable.update { it.copy(conversations = value) } } }
        viewModelScope.launch { preferenceStore.preferences.catch { error("Could not load preferences.") }.collect { value -> if (preferenceJob?.isActive != true) mutable.update { it.copy(preferences = value) } } }
        onSearchChange("")
    }
    private fun error(message: String) { mutable.update { it.copy(errorMessage = message) } }
    fun dismissError() { mutable.update { it.copy(errorMessage = null) } }
    fun showSheet(sheet: Sheet) { mutable.update { it.copy(sheet = sheet) } }
    fun navigate(destination: Destination) {
        if (destination == Destination.VOICE) { voiceOrigin = mutable.value.destination; mutable.update { it.copy(voiceStatus = VoiceStatus.READY, transcript = "") } }
        mutable.update { it.copy(destination = destination, sheet = Sheet.NONE) }
    }
    fun back() { navigate(if (mutable.value.destination == Destination.VOICE) voiceOrigin else Destination.HOME) }
    fun onPromptChange(value: String) { mutable.update { it.copy(prompt = value.take(ConversationPromptBuilder.MAX_INPUT), promptError = null) } }
    fun newChat(category: Category = mutable.value.preferences.category, suggested: Boolean = false) {
        messageJob?.cancel()
        mutable.update { it.copy(destination = Destination.CHAT, conversationId = null, messages = emptyList(), category = category, prompt = if (suggested) category.suggestion else "", promptError = null, sheet = Sheet.NONE) }
    }
    fun selectCategory(category: Category) {
        // Existing conversation category stays fixed; selecting a new host starts a fresh conversation.
        if (mutable.value.conversationId != null) newChat(category) else mutable.update { it.copy(category = category, sheet = Sheet.NONE) }
    }
    fun openConversation(conversation: Conversation) {
        mutable.update { it.copy(destination = Destination.CHAT, conversationId = conversation.id, category = conversation.category, prompt = "", promptError = null, messages = emptyList()) }
        observeMessages(conversation.id)
    }
    private fun observeMessages(id: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            store.messages(id).catch { error("Could not load these messages.") }.collect { messages ->
                mutable.update { if (it.conversationId == id) it.copy(messages = messages) else it }
            }
        }
    }
    fun onSearchChange(value: String) {
        mutable.update { it.copy(searchQuery = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { store.conversations(value).catch { error("Search is unavailable. Try again.") }.collect { matches -> mutable.update { it.copy(searchResults = matches) } } }
    }
    fun savePreferences(value: UserPreferences) {
        val normalized = value.copy(displayName = value.displayName.take(40), modelId = GeminiModels.normalize(value.modelId))
        mutable.update { it.copy(preferences = normalized) }
        preferenceJob?.cancel()
        preferenceJob = viewModelScope.launch {
            try { preferenceStore.save(normalized) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error("Your preferences could not be saved.") }
        }
    }
    fun selectModel(id: String) {
        savePreferences(mutable.value.preferences.copy(modelId = GeminiModels.normalize(id)))
        showSheet(Sheet.NONE)
    }
    fun onVoiceStarted() { mutable.update { it.copy(voiceStatus = VoiceStatus.WAITING) } }
    fun onVoiceResult(text: String?) { mutable.update {
        if (text.isNullOrBlank()) it.copy(voiceStatus = VoiceStatus.CANCELLED)
        else it.copy(voiceStatus = VoiceStatus.TRANSCRIPT, transcript = text.take(ConversationPromptBuilder.MAX_INPUT))
    } }
    fun onVoiceUnavailable() { mutable.update { it.copy(voiceStatus = VoiceStatus.UNAVAILABLE, errorMessage = "Speech recognition is not available. You can still type your question.") } }
    fun useTranscript() { mutable.update { it.copy(destination = Destination.CHAT, prompt = it.transcript.ifBlank { it.prompt }, sheet = Sheet.NONE) } }
    fun feedback(message: ChatMessage, value: Int) { viewModelScope.launch {
        try { store.feedback(message.id, if (message.feedback == value) 0 else value) } catch (_: Exception) { error("Feedback could not be saved.") }
    } }
    fun onSend() {
        val state = mutable.value
        if (state.isLoading || state.isInitializing) return
        val prompt = state.prompt.trim()
        if (prompt.isBlank()) { mutable.update { it.copy(promptError = "Write a message first.") }; return }
        val conversationId = state.conversationId ?: UUID.randomUUID().toString()
        val turnId = UUID.randomUUID().toString()
        val user = ChatMessage(turnId, conversationId, turnId, (state.messages.maxOfOrNull { it.sequence } ?: -2) + 2, Role.USER, prompt, Delivery.PENDING)
        mutable.update { it.copy(isLoading = true, pendingTurnId = turnId, errorMessage = null, promptError = null, conversationId = conversationId) }
        requestJob = viewModelScope.launch {
            try {
                store.create(Conversation(conversationId, prompt.take(64), state.category, prompt.take(160), System.currentTimeMillis()))
                store.appendUser(user)
                mutable.update { if (it.conversationId == conversationId && it.prompt == state.prompt) it.copy(prompt = "") else it }
                if (mutable.value.conversationId == conversationId) observeMessages(conversationId)
                execute(user, state.messages, state.category, state.preferences.modelId)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error("Your message could not be saved. Please try again.")
            } finally { mutable.update { it.copy(isLoading = false, pendingTurnId = null) } }
        }
    }
    fun retry(user: ChatMessage) {
        val state = mutable.value
        if (state.isLoading || state.isInitializing || user.role != Role.USER) return
        if (state.messages.lastOrNull { it.role == Role.USER }?.id != user.id) return
        mutable.update { it.copy(isLoading = true, pendingTurnId = user.turnId, errorMessage = null) }
        requestJob = viewModelScope.launch {
            try { store.setDelivery(user.id, Delivery.PENDING); execute(user, state.messages, state.category, state.preferences.modelId) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error("Could not retry. Your conversation has been kept.") }
            finally { mutable.update { it.copy(isLoading = false, pendingTurnId = null) } }
        }
    }
    fun regenerate() { mutable.value.messages.lastOrNull { it.role == Role.USER }?.let(::retry) }
    private suspend fun execute(user: ChatMessage, history: List<ChatMessage>, category: Category, modelId: String) {
        try {
            val result = repository.generateText(ConversationPromptBuilder.build(history, user, category), GeminiModels.selected(modelId).id)
            result.fold(onSuccess = { answer -> store.complete(user, answer) }, onFailure = { failure ->
                store.setDelivery(user.id, Delivery.FAILED)
                error(failure.message ?: "Could not get an answer. Please retry.")
            })
        } catch (e: CancellationException) {
            withContext(NonCancellable) { runCatching { store.setDelivery(user.id, Delivery.INTERRUPTED) } }
            throw e
        } catch (_: Exception) {
            runCatching { store.setDelivery(user.id, Delivery.FAILED) }
            error("Could not finish this message. Your conversation has been kept; please retry.")
        }
    }
    companion object {
        fun factory(repository: GeminiRepository, store: ChatStore, preferences: PreferenceStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                ChatViewModel(repository, store, preferences, extras.createSavedStateHandle()) as T
        }
    }
}
