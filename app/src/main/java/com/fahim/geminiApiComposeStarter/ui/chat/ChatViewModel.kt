package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.DisplayNameStore
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessage
import com.fahim.geminiApiComposeStarter.data.local.ChatSession
import com.fahim.geminiApiComposeStarter.data.local.MessageAuthor
import com.fahim.geminiApiComposeStarter.network.ConnectivityObserver
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val repository: GeminiRepository,
    private val chatDao: ChatDao,
    private val preferences: DisplayNameStore,
    private val connectivityObserver: ConnectivityObserver,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var initialSessionChosen = false
    private var retryRequest: RetryRequest? = null

    init {
        observeSessions()
        observeDisplayName()
        observeConnectivity()
    }

    private fun observeSessions() {
        viewModelScope.launch {

            chatDao.observeSessions().collectLatest { sessions ->

                _uiState.update {
                    it.copy(sessions = sessions)
                }

                if (!initialSessionChosen) {

                    initialSessionChosen = true

                    sessions.firstOrNull()?.let { session ->
                        selectSessionInternal(session.id)
                    }
                }
            }
        }
    }

    private fun observeDisplayName() {

        viewModelScope.launch {

            preferences.displayName.collectLatest { storedName ->

                val safeName =
                    storedName.trim()
                        .ifBlank { DEFAULT_DISPLAY_NAME }

                _uiState.update { state ->

                    state.copy(

                        displayName = safeName,

                        settingsDisplayName =
                        if (state.isSettingsOpen) {
                            state.settingsDisplayName
                        } else {
                            safeName
                        }
                    )
                }
            }
        }
    }

    private fun observeConnectivity() {

        viewModelScope.launch {

            connectivityObserver.isOnline.collectLatest { online ->

                _uiState.update {
                    it.copy(
                        isOnline = online
                    )
                }
            }
        }
    }

    fun onPromptChange(value: String) {

        _uiState.update {

            it.copy(
                prompt = value,
                promptError = null
            )
        }
    }

    fun onVoiceResult(text: String) {

        val cleanVoiceText = text.trim()

        if (cleanVoiceText.isBlank()) {
            return
        }

        _uiState.update { state ->

            val merged =
                when {

                    state.prompt.isBlank() ->
                        cleanVoiceText

                    state.prompt.endsWith(" ") ->
                        state.prompt + cleanVoiceText

                    else ->
                        state.prompt + " " + cleanVoiceText
                }

            state.copy(
                prompt = merged,
                promptError = null
            )
        }
    }

    fun onVoiceInputUnavailable() {

        _uiState.update {

            it.copy(

                errorMessage =
                "Speech input is not available on this device.",

                retryAvailable =
                retryRequest != null
            )
        }
    }

    fun onSend() {

        val prompt =
            _uiState.value.prompt.trim()

        if (prompt.isEmpty()) {

            _uiState.update {

                it.copy(
                    promptError = PromptError.EMPTY
                )
            }

            return
        }

        if (_uiState.value.isLoading) {
            return
        }

        if (!_uiState.value.isOnline) {

            _uiState.update {

                it.copy(

                    errorMessage =
                    "You are offline. Reconnect to Wi-Fi or mobile data, then send again.",

                    retryAvailable =
                    retryRequest != null
                )
            }

            return
        }

        if (!hasApiKey) {

            _uiState.update {

                it.copy(

                    errorMessage =
                    MISSING_API_KEY_MESSAGE,

                    retryAvailable = false
                )
            }

            return
        }

        _uiState.update {

            it.copy(

                isLoading = true,

                errorMessage = null,

                promptError = null,

                retryAvailable = false
            )
        }

        viewModelScope.launch {

            try {

                val sessionId =
                    ensureSession(prompt)

                withContext(Dispatchers.IO) {

                    chatDao.insertMessage(

                        ChatMessage(

                            sessionId = sessionId,

                            author =
                            MessageAuthor.USER,

                            text = prompt
                        )
                    )

                    chatDao.touchSession(

                        sessionId = sessionId,

                        updatedAt =
                        System.currentTimeMillis()
                    )
                }

                /*
                 * Clear the text only after Room has stored
                 * the user's message.
                 */
                _uiState.update {
                    it.copy(prompt = "")
                }

                generateReply(
                    prompt = prompt,
                    sessionId = sessionId
                )

            } catch (cancelled: CancellationException) {

                throw cancelled

            } catch (error: Exception) {

                retryRequest = null

                _uiState.update {

                    it.copy(

                        isLoading = false,

                        errorMessage =
                        "The message could not be saved. Please try again.",

                        retryAvailable = false
                    )
                }
            }
        }
    }

    private suspend fun ensureSession(
        firstPrompt: String
    ): Long {

        val existingId =
            _uiState.value.currentSessionId

        if (existingId != null) {
            return existingId
        }

        val now =
            System.currentTimeMillis()

        val newId =
            withContext(Dispatchers.IO) {

                chatDao.insertSession(

                    ChatSession(

                        title =
                        makeSessionTitle(firstPrompt),

                        createdAt = now,

                        updatedAt = now
                    )
                )
            }

        selectSessionInternal(newId)

        return newId
    }

    private suspend fun generateReply(
        prompt: String,
        sessionId: Long
    ) {

        val result =
            repository.generateText(prompt)

        result.fold(

            onSuccess = { reply ->

                withContext(Dispatchers.IO) {

                    chatDao.insertMessage(

                        ChatMessage(

                            sessionId = sessionId,

                            author =
                            MessageAuthor.GEMINI,

                            text = reply
                        )
                    )

                    chatDao.touchSession(

                        sessionId = sessionId,

                        updatedAt =
                        System.currentTimeMillis()
                    )
                }

                retryRequest = null

                _uiState.update {

                    it.copy(

                        isLoading = false,

                        errorMessage = null,

                        retryAvailable = false
                    )
                }
            },

            onFailure = { error ->

                retryRequest =
                    RetryRequest(

                        prompt = prompt,

                        sessionId = sessionId
                    )

                _uiState.update {

                    it.copy(

                        isLoading = false,

                        errorMessage =
                        friendlyError(error),

                        retryAvailable = true
                    )
                }
            }
        )
    }

    fun onRetry() {

        if (_uiState.value.isLoading) {
            return
        }

        val request =
            retryRequest ?: return

        if (!_uiState.value.isOnline) {

            _uiState.update {

                it.copy(

                    errorMessage =
                    "You are still offline. Reconnect before retrying.",

                    retryAvailable = true
                )
            }

            return
        }

        if (!hasApiKey) {

            _uiState.update {

                it.copy(

                    errorMessage =
                    MISSING_API_KEY_MESSAGE,

                    retryAvailable = false
                )
            }

            return
        }

        _uiState.update {

            it.copy(

                isLoading = true,

                errorMessage = null,

                retryAvailable = false
            )
        }

        viewModelScope.launch {

            generateReply(

                prompt = request.prompt,

                sessionId =
                request.sessionId
            )
        }
    }

    fun onNewChat() {

        if (_uiState.value.isLoading) {
            return
        }

        messagesJob?.cancel()

        messagesJob = null

        retryRequest = null

        _uiState.update {

            it.copy(

                currentSessionId = null,

                messages = emptyList(),

                prompt = "",

                promptError = null,

                errorMessage = null,

                retryAvailable = false
            )
        }
    }

    fun onSelectSession(
        sessionId: Long
    ) {

        if (_uiState.value.isLoading) {
            return
        }

        retryRequest = null

        selectSessionInternal(sessionId)
    }

    private fun selectSessionInternal(
        sessionId: Long
    ) {

        messagesJob?.cancel()

        _uiState.update {

            it.copy(

                currentSessionId = sessionId,

                messages = emptyList(),

                errorMessage = null,

                retryAvailable = false
            )
        }

        messagesJob =
            viewModelScope.launch {

                chatDao.observeMessages(sessionId)
                    .collectLatest { messages ->

                        _uiState.update {

                            it.copy(
                                messages = messages
                            )
                        }
                    }
            }
    }

    fun onDeleteSession(
        sessionId: Long
    ) {

        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {

            if (
                _uiState.value.currentSessionId ==
                sessionId
            ) {

                messagesJob?.cancel()

                messagesJob = null

                retryRequest = null

                _uiState.update {

                    it.copy(

                        currentSessionId = null,

                        messages = emptyList(),

                        errorMessage = null,

                        retryAvailable = false
                    )
                }
            }

            withContext(Dispatchers.IO) {

                chatDao.deleteSession(sessionId)
            }
        }
    }

    fun onClearHistory() {

        val sessionId =
            _uiState.value.currentSessionId
                ?: return

        onDeleteSession(sessionId)
    }

    fun onErrorConsumed() {

        _uiState.update {

            it.copy(
                errorMessage = null
            )
        }
    }

    fun onOpenSettings() {

        _uiState.update {

            it.copy(

                isSettingsOpen = true,

                settingsDisplayName =
                it.displayName
            )
        }
    }

    fun onSettingsNameChange(
        value: String
    ) {

        _uiState.update {

            it.copy(
                settingsDisplayName = value
            )
        }
    }

    fun onDismissSettings() {

        _uiState.update {

            it.copy(

                isSettingsOpen = false,

                settingsDisplayName =
                it.displayName
            )
        }
    }

    fun onSaveSettings() {

        val name =
            _uiState.value.settingsDisplayName
                .trim()
                .ifBlank {
                    DEFAULT_DISPLAY_NAME
                }

        viewModelScope.launch {

            preferences.setDisplayName(name)

            _uiState.update {

                it.copy(

                    isSettingsOpen = false,

                    displayName = name,

                    settingsDisplayName = name
                )
            }
        }
    }

    private fun makeSessionTitle(
        prompt: String
    ): String {

        val oneLine =
            prompt
                .replace('\n', ' ')
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        return if (
            oneLine.length <= 38
        ) {

            oneLine

        } else {

            oneLine
                .take(38)
                .trimEnd() + "…"
        }
    }

    private fun friendlyError(
        error: Throwable
    ): String {

        val message =
            buildString {

                append(
                    error.message.orEmpty()
                )

                append(' ')

                append(
                    error.cause
                        ?.message
                        .orEmpty()
                )

            }.lowercase()

        return when {

            error is SocketTimeoutException ||
                    message.contains("timeout") ||
                    message.contains("timed out") ->

                "Gemini took too long to respond. Your message is saved — tap Retry."

            message.contains("429") ||
                    message.contains("quota") ||
                    message.contains(
                        "resource_exhausted"
                    ) ||
                    message.contains(
                        "resource exhausted"
                    ) ->

                "Gemini API quota has been exceeded. Your message is saved — retry after the quota resets."

            message.contains("api key") ||
                    message.contains("401") ||
                    message.contains("403") ||
                    message.contains(
                        "unauthenticated"
                    ) ||
                    message.contains(
                        "permission denied"
                    ) ->

                "The Gemini API key was rejected. Check the key in local.properties, rebuild the app, and try again."

            message.contains("500") ||
                    message.contains("502") ||
                    message.contains("503") ||
                    message.contains("server") ||
                    message.contains(
                        "unavailable"
                    ) ->

                "Gemini is temporarily unavailable. Your message is saved — tap Retry in a moment."

            message.contains(
                "empty response"
            ) ->

                "Gemini returned an empty response. Your message is saved — tap Retry."

            else ->

                "Gemini could not answer right now. Your message is saved — tap Retry."
        }
    }

    private data class RetryRequest(
        val prompt: String,
        val sessionId: Long
    )

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(
            repository: GeminiRepository,
            chatDao: ChatDao,
            preferences: DisplayNameStore,
            connectivityObserver: ConnectivityObserver,
            hasApiKey: Boolean
        ): ViewModelProvider.Factory {

            return object :
                ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    if (
                        modelClass.isAssignableFrom(
                            ChatViewModel::class.java
                        )
                    ) {

                        return ChatViewModel(

                            repository =
                            repository,

                            chatDao =
                            chatDao,

                            preferences =
                            preferences,

                            connectivityObserver =
                            connectivityObserver,

                            hasApiKey =
                            hasApiKey

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