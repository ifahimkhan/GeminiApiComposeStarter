package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeChatHistoryRepository(
        initialMessages: List<ChatMessage> = emptyList(),
    ) : ChatHistoryRepository {
        val messagesFlow = MutableStateFlow(initialMessages)
        val savedMessages = initialMessages.toMutableList()
        var clearCalled = false

        override fun getMessages(): Flow<List<ChatMessage>> = messagesFlow

        override suspend fun saveMessage(message: ChatMessage): Result<Unit> {
            savedMessages.add(message)
            messagesFlow.value = savedMessages.toList()
            return Result.success(Unit)
        }

        override suspend fun clearHistory(): Result<Unit> {
            clearCalled = true
            savedMessages.clear()
            messagesFlow.value = emptyList()
            return Result.success(Unit)
        }
    }

    private class FakeUserPreferencesRepository(
        initialTheme: AppTheme = AppTheme.SYSTEM,
    ) : UserPreferencesRepository {
        val preferencesFlow = MutableStateFlow(UserPreferences(theme = initialTheme))
        var lastSavedTheme: AppTheme? = null

        override val userPreferencesFlow: Flow<UserPreferences> = preferencesFlow

        override suspend fun setTheme(theme: AppTheme) {
            lastSavedTheme = theme
            preferencesFlow.value = preferencesFlow.value.copy(theme = theme)
        }

        override suspend fun setAutoScroll(enabled: Boolean) {
            preferencesFlow.value = preferencesFlow.value.copy(autoScroll = enabled)
        }
    }

    private class FakeGeminiRepository(
        var result: Result<String> = Result.success("Sample response"),
    ) : GeminiRepository {
        var callCount = 0
        var lastPrompt: String? = null

        override suspend fun generateText(prompt: String): Result<String> {
            callCount++
            lastPrompt = prompt
            return result
        }
    }

    @Test
    fun initialChatUiState_hasExpectedDefaults() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        val initialState = viewModel.uiState.value
        assertTrue("Messages should be empty initially", initialState.messages.isEmpty())
        assertEquals("", initialState.prompt)
        assertEquals("", initialState.response)
        assertFalse("isLoading should be false initially", initialState.isLoading)
        assertNull("promptError should be null initially", initialState.promptError)
        assertNull("errorMessage should be null initially", initialState.errorMessage)
        assertFalse("canRetry should be false initially", initialState.canRetry)
        assertNull("lastFailedPrompt should be null initially", initialState.lastFailedPrompt)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsEmptyPromptError() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun onSend_withWhitespaceOnlyPrompt_setsEmptyPromptError() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("   \t\n  ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = false)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun onSend_userMessageAddedImmediatelyAndClearsPrompt() = runTest {
        val repository = FakeGeminiRepository(Result.success("Response"))
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("What is Jetpack Compose?")
        viewModel.onSend()

        // Verify prompt is cleared
        assertEquals("", viewModel.uiState.value.prompt)

        // Verify user message was added
        val firstMessage = viewModel.uiState.value.messages.first()
        assertEquals("What is Jetpack Compose?", firstMessage.text)
        assertEquals(Participant.USER, firstMessage.participant)
    }

    @Test
    fun onSend_loadingStateWhileGeminiResponds_andClearsWhenComplete() = runTest {
        val deferredResponse = kotlinx.coroutines.CompletableDeferred<Result<String>>()
        val controllableRepo = object : GeminiRepository {
            var callCount = 0
            override suspend fun generateText(prompt: String): Result<String> {
                callCount++
                return deferredResponse.await()
            }
        }

        val viewModel = ChatViewModel(repository = controllableRepo, hasApiKey = true)

        viewModel.onPromptChange("Calculate pi")
        viewModel.onSend()

        // While Gemini API call is in-flight:
        assertTrue("isLoading must be true while Gemini responds", viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Calculate pi", viewModel.uiState.value.messages.first().text)
        assertEquals(1, controllableRepo.callCount)

        // Now resolve the deferred response
        deferredResponse.complete(Result.success("3.14159..."))

        // After completion:
        assertFalse("isLoading must be false after Gemini responds", viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("3.14159...", viewModel.uiState.value.response)
        assertEquals("3.14159...", viewModel.uiState.value.messages[1].text)
        assertEquals(Participant.MODEL, viewModel.uiState.value.messages[1].participant)
    }

    @Test
    fun onSend_successfulGeminiResponse_updatesResponseAndAppendsModelMessage() = runTest {
        val repository = FakeGeminiRepository(Result.success("Hello from Gemini"))
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Hello", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("Hello from Gemini", messages[1].text)
        assertEquals(Participant.MODEL, messages[1].participant)
        assertEquals("Hello from Gemini", viewModel.uiState.value.response)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_geminiApiFailure_updatesErrorMessageAndEnablesRetry() = runTest {
        val repository = FakeGeminiRepository(Result.failure(RuntimeException("Rate limit exceeded")))
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("Explain AI")
        viewModel.onSend()

        assertEquals("Rate limit exceeded", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.canRetry)
        assertEquals("Explain AI", viewModel.uiState.value.lastFailedPrompt)
        assertEquals(1, repository.callCount)

        // Only the user message should be present, no model message added on error
        val messages = viewModel.uiState.value.messages
        assertEquals(1, messages.size)
        assertEquals("Explain AI", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
    }

    @Test
    fun chatHistory_multiTurnConversation_preservesOrderingAndTurnCount() = runTest {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        // Turn 1
        repository.result = Result.success("Turn 1 response")
        viewModel.onPromptChange("Turn 1 question")
        viewModel.onSend()

        // Turn 2
        repository.result = Result.success("Turn 2 response")
        viewModel.onPromptChange("Turn 2 question")
        viewModel.onSend()

        // Turn 3
        repository.result = Result.success("Turn 3 response")
        viewModel.onPromptChange("Turn 3 question")
        viewModel.onSend()

        val messages = viewModel.uiState.value.messages
        assertEquals(6, messages.size)
        assertEquals("Turn 1 question", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("Turn 1 response", messages[1].text)
        assertEquals(Participant.MODEL, messages[1].participant)
        assertEquals("Turn 2 question", messages[2].text)
        assertEquals(Participant.USER, messages[2].participant)
        assertEquals("Turn 2 response", messages[3].text)
        assertEquals(Participant.MODEL, messages[3].participant)
        assertEquals("Turn 3 question", messages[4].text)
        assertEquals(Participant.USER, messages[4].participant)
        assertEquals("Turn 3 response", messages[5].text)
        assertEquals(Participant.MODEL, messages[5].participant)
    }

    @Test
    fun onRetry_reExecutesLastFailedPrompt() = runTest {
        val repository = FakeGeminiRepository(Result.failure(RuntimeException("First failure")))
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("Important question")
        viewModel.onSend()
        assertEquals(1, repository.callCount)
        assertEquals("First failure", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.canRetry)

        // Make subsequent call succeed
        repository.result = Result.success("Recovered response")
        viewModel.onRetry()

        assertEquals(2, repository.callCount)
        assertEquals("Important question", repository.lastPrompt)
        assertEquals("Recovered response", viewModel.uiState.value.response)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.canRetry)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest {
        val repository = FakeGeminiRepository(Result.failure(RuntimeException("Some error")))
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("Prompt")
        viewModel.onSend()
        assertEquals("Some error", viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_whileLoading_preventsDuplicateSubmission() = runTest {
        val deferredResponse = kotlinx.coroutines.CompletableDeferred<Result<String>>()
        val controllableRepo = object : GeminiRepository {
            var callCount = 0
            override suspend fun generateText(prompt: String): Result<String> {
                callCount++
                return deferredResponse.await()
            }
        }

        val viewModel = ChatViewModel(repository = controllableRepo, hasApiKey = true)

        viewModel.onPromptChange("First")
        viewModel.onSend()
        assertEquals(1, controllableRepo.callCount)
        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt second send while loading
        viewModel.onPromptChange("Second")
        viewModel.onSend()

        // Still only 1 call
        assertEquals(1, controllableRepo.callCount)

        deferredResponse.complete(Result.success("Done"))
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSpeechResult_whenPromptEmpty_setsPrompt() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onSpeechResult("What is machine learning?")
        assertEquals("What is machine learning?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSpeechResult_whenPromptNotEmpty_appendsPrompt() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("Hello")
        viewModel.onSpeechResult("world")
        assertEquals("Hello world", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSpeechResult_blankText_doesNotChangePrompt() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onPromptChange("Hello")
        viewModel.onSpeechResult("   ")
        assertEquals("Hello", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSpeechError_updatesErrorMessage() {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = repository, hasApiKey = true)

        viewModel.onSpeechError("Microphone permission denied")
        assertEquals("Microphone permission denied", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun init_restoresSavedMessagesFromChatHistoryRepository() = runTest {
        val saved = listOf(
            ChatMessage(id = "old-1", text = "Old user prompt", participant = Participant.USER),
            ChatMessage(id = "old-2", text = "Old gemini response", participant = Participant.MODEL),
        )
        val historyRepo = FakeChatHistoryRepository(initialMessages = saved)
        val geminiRepo = FakeGeminiRepository()

        val viewModel = ChatViewModel(
            repository = geminiRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepo,
        )

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Old user prompt", messages[0].text)
        assertEquals("Old gemini response", messages[1].text)
    }

    @Test
    fun onSend_savesUserAndModelMessagesToChatHistoryRepository() = runTest {
        val historyRepo = FakeChatHistoryRepository()
        val geminiRepo = FakeGeminiRepository(Result.success("Persisted answer"))

        val viewModel = ChatViewModel(
            repository = geminiRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepo,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        viewModel.onPromptChange("Persist me")
        viewModel.onSend()

        assertEquals(2, historyRepo.savedMessages.size)
        assertEquals("Persist me", historyRepo.savedMessages[0].text)
        assertEquals(Participant.USER, historyRepo.savedMessages[0].participant)
        assertEquals("Persisted answer", historyRepo.savedMessages[1].text)
        assertEquals(Participant.MODEL, historyRepo.savedMessages[1].participant)
    }

    @Test
    fun onClearHistory_clearsMessagesAndCallsRepository() = runTest {
        val saved = listOf(
            ChatMessage(id = "msg-1", text = "Message to clear", participant = Participant.USER)
        )
        val historyRepo = FakeChatHistoryRepository(initialMessages = saved)
        val geminiRepo = FakeGeminiRepository()

        val viewModel = ChatViewModel(
            repository = geminiRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepo,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        assertEquals(1, viewModel.uiState.value.messages.size)
        viewModel.onClearHistory()

        assertTrue("UI state messages should be empty after clearing", viewModel.uiState.value.messages.isEmpty())
        assertTrue("Repository clearHistory should be invoked", historyRepo.clearCalled)
        assertFalse(viewModel.uiState.value.isClearHistoryDialogOpen)
    }

    @Test
    fun onSetTheme_updatesStateAndPersistsPreference() = runTest {
        val prefsRepo = FakeUserPreferencesRepository(initialTheme = AppTheme.SYSTEM)
        val geminiRepo = FakeGeminiRepository()

        val viewModel = ChatViewModel(
            repository = geminiRepo,
            hasApiKey = true,
            userPreferencesRepository = prefsRepo,
        )

        assertEquals(AppTheme.SYSTEM, viewModel.uiState.value.theme)

        viewModel.onSetTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, viewModel.uiState.value.theme)
        assertEquals(AppTheme.DARK, prefsRepo.lastSavedTheme)
        assertFalse(viewModel.uiState.value.isThemeDialogOpen)
    }

    @Test
    fun themeAndClearHistoryDialogs_toggleStateAppropriately() {
        val geminiRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = geminiRepo, hasApiKey = true)

        assertFalse(viewModel.uiState.value.isThemeDialogOpen)
        viewModel.onShowThemeDialog(true)
        assertTrue(viewModel.uiState.value.isThemeDialogOpen)
        viewModel.onShowThemeDialog(false)
        assertFalse(viewModel.uiState.value.isThemeDialogOpen)

        assertFalse(viewModel.uiState.value.isClearHistoryDialogOpen)
        viewModel.onShowClearHistoryDialog(true)
        assertTrue(viewModel.uiState.value.isClearHistoryDialogOpen)
        viewModel.onShowClearHistoryDialog(false)
        assertFalse(viewModel.uiState.value.isClearHistoryDialogOpen)
    }
}
