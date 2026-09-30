package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.MainDispatcherRule
import com.fahim.geminiApiComposeStarter.data.FakeChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeChatRepository()
        viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
    }

    @Test
    fun initialState_isCorrect() {
        val state = viewModel.uiState.value

        assertEquals("", state.prompt)
        assertEquals("", state.response)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
        assertTrue(state.messages.isEmpty())
        assertTrue(state.conversationId.isNotBlank())
        assertEquals(AppScreen.CHAT, state.currentScreen)
    }

    @Test
    fun onPromptChange_updatesPrompt_andClearsPromptError() {
        // First trigger an empty prompt error
        viewModel.onSend("")
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        // Then change prompt text
        viewModel.onPromptChange("What is Jetpack Compose?")

        assertEquals("What is Jetpack Compose?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError_andDoesNotCallRepository() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeRepository.generatedPrompts.isEmpty())
    }

    @Test
    fun onSend_withoutApiKey_setsMissingKeyError_andDoesNotCallRepository() = runTest {
        val viewModelNoKey = ChatViewModel(repository = fakeRepository, hasApiKey = false)

        viewModelNoKey.onPromptChange("Hello Gemini")
        viewModelNoKey.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModelNoKey.uiState.value.errorMessage)
        assertFalse(viewModelNoKey.uiState.value.isLoading)
        assertTrue(fakeRepository.generatedPrompts.isEmpty())
    }

    @Test
    fun onSend_withValidPrompt_savesMessages_andUpdatesResponse() = runTest {
        fakeRepository.generateTextResult = Result.success("Jetpack Compose is Android's modern toolkit.")

        viewModel.onPromptChange("What is Jetpack Compose?")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("Jetpack Compose is Android's modern toolkit.", state.response)

        // Verify user and Gemini messages were recorded in repository
        assertEquals(1, fakeRepository.generatedPrompts.size)
        assertEquals("What is Jetpack Compose?", fakeRepository.generatedPrompts[0])
        assertEquals(2, fakeRepository.savedMessages.size)
        assertEquals(Participant.USER, fakeRepository.savedMessages[0].participant)
        assertEquals("What is Jetpack Compose?", fakeRepository.savedMessages[0].text)
        assertEquals(Participant.GEMINI, fakeRepository.savedMessages[1].participant)
        assertEquals("Jetpack Compose is Android's modern toolkit.", fakeRepository.savedMessages[1].text)
    }

    @Test
    fun onSend_multiTurnConversation_passesPreviousHistoryToRepository() = runTest {
        // Turn 1
        fakeRepository.generateTextResult = Result.success("Atomic Habits is a book about tiny changes.")
        viewModel.onPromptChange("What is meant by atomic habits")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(1, fakeRepository.receivedHistories.size)
        assertTrue(fakeRepository.receivedHistories[0].isEmpty()) // First turn has no prior history

        // Turn 2
        fakeRepository.generateTextResult = Result.success("In short, tiny habits compound into big results.")
        viewModel.onPromptChange("In simple words and in short")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, fakeRepository.receivedHistories.size)
        val secondTurnHistory = fakeRepository.receivedHistories[1]
        assertEquals(2, secondTurnHistory.size)
        assertEquals("What is meant by atomic habits", secondTurnHistory[0].text)
        assertEquals(Participant.USER, secondTurnHistory[0].participant)
        assertEquals("Atomic Habits is a book about tiny changes.", secondTurnHistory[1].text)
        assertEquals(Participant.GEMINI, secondTurnHistory[1].participant)
    }

    @Test
    fun onSend_whenRepositoryFails_setsErrorMessage_andClearsLoading() = runTest {
        fakeRepository.generateTextResult = Result.failure(RuntimeException("Rate limit exceeded"))

        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Rate limit exceeded", state.errorMessage)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest {
        fakeRepository.generateTextResult = Result.failure(RuntimeException("Network error"))
        viewModel.onSend("Hello")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onRetry_resendsLastPrompt() = runTest {
        fakeRepository.generateTextResult = Result.failure(RuntimeException("Temporary glitch"))
        viewModel.onSend("Explain encryption")
        advanceUntilIdle()

        assertEquals("Temporary glitch", viewModel.uiState.value.errorMessage)

        // Now fix the repository result and retry
        fakeRepository.generateTextResult = Result.success("Encryption scrambles data into ciphertext.")
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("Encryption scrambles data into ciphertext.", state.response)
        assertEquals(2, fakeRepository.generatedPrompts.size)
    }

    @Test
    fun onNewChat_createsNewConversationId_andResetsState() = runTest {
        val initialConvId = viewModel.uiState.value.conversationId

        viewModel.onNewChat()

        val newConvId = viewModel.uiState.value.conversationId
        assertNotEquals(initialConvId, newConvId)
        assertEquals("", viewModel.uiState.value.prompt)
        assertEquals("", viewModel.uiState.value.response)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSelectConversation_switchesActiveConversation() {
        val targetConversationId = "custom-conversation-id-123"

        viewModel.onSelectConversation(targetConversationId)

        assertEquals(targetConversationId, viewModel.uiState.value.conversationId)
        assertEquals("", viewModel.uiState.value.prompt)
        assertEquals("", viewModel.uiState.value.response)
    }

    @Test
    fun onClearHistory_clearsMessagesAndStartsNewConversation() = runTest {
        // Send a message first
        viewModel.onSend("Message 1")
        advanceUntilIdle()
        assertFalse(fakeRepository.savedMessages.isEmpty())

        // Clear history
        viewModel.onClearHistory()
        advanceUntilIdle()

        assertTrue(fakeRepository.savedMessages.isEmpty())
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun navigationBetweenChatAndSettings_updatesScreenState() {
        assertEquals(AppScreen.CHAT, viewModel.uiState.value.currentScreen)

        viewModel.onNavigateToSettings()
        assertEquals(AppScreen.SETTINGS, viewModel.uiState.value.currentScreen)

        viewModel.onNavigateToChat()
        assertEquals(AppScreen.CHAT, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun settingsToggles_updateUiState() {
        val initialDark = viewModel.uiState.value.settings.isDarkTheme
        viewModel.onThemeToggle(!initialDark)
        assertEquals(!initialDark, viewModel.uiState.value.settings.isDarkTheme)

        val initialSave = viewModel.uiState.value.settings.saveHistory
        viewModel.onSaveHistoryToggle(!initialSave)
        assertEquals(!initialSave, viewModel.uiState.value.settings.saveHistory)
    }

    @Test
    fun onSuggestionClick_sendsSuggestionPromptDirectly() = runTest {
        fakeRepository.generateTextResult = Result.success("Here is a summary.")

        viewModel.onSuggestionClick("Summarize the topic")
        advanceUntilIdle()

        assertEquals(1, fakeRepository.generatedPrompts.size)
        assertEquals("Summarize the topic", fakeRepository.generatedPrompts[0])
        assertEquals("Here is a summary.", viewModel.uiState.value.response)
    }
}
