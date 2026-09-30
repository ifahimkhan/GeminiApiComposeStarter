package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeGeminiRepository

    @Before
    fun setUp() {
        repository = FakeGeminiRepository()
    }

    @Test
    fun emptyPromptDoesNotSend() {
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
        )
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(0, repository.generateCallCount)
    }

    @Test
    fun missingApiKeySetsErrorMessageAndDoesNotSend() {
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = false,
        )
        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals(0, repository.generateCallCount)
    }

    @Test
    fun historySentToGenerateTextExcludesCurrentPrompt() = runTest {
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
        )
        // First message
        viewModel.onPromptChange("First question")
        viewModel.onSend()

        assertEquals(0, repository.lastReceivedHistory.size) // First message has no prior history

        // Second message
        viewModel.onPromptChange("Second question")
        viewModel.onSend()

        assertEquals(2, repository.lastReceivedHistory.size) // Prior turn: First question + response
        assertEquals("First question", repository.lastReceivedHistory[0].text)
        assertEquals("Fake response to: First question", repository.lastReceivedHistory[1].text)
        assertTrue(repository.lastReceivedHistory.none { it.text == "Second question" })
    }

    @Test
    fun retryDoesNotCreateSecondUserMessage() = runTest {
        repository.shouldFail = true
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
        )

        viewModel.onPromptChange("Failing prompt")
        viewModel.onSend()

        // 1 user message saved, 0 model messages
        val messagesAfterFail = viewModel.uiState.value.messages
        assertEquals(1, messagesAfterFail.size)
        assertEquals("Failing prompt", messagesAfterFail[0].text)

        // On retry
        repository.shouldFail = false
        viewModel.onRetry()

        val messagesAfterRetry = viewModel.uiState.value.messages
        assertEquals(2, messagesAfterRetry.size) // Still 1 user message + 1 model message
        assertEquals(1, messagesAfterRetry.count { it.participant == Participant.USER })
        assertEquals("Failing prompt", messagesAfterRetry[0].text)
        assertEquals("Fake response to: Failing prompt", messagesAfterRetry[1].text)
    }

    @Test
    fun onNewChatCreatesNewConversationAndResetsMessages() = runTest {
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
        )
        viewModel.onPromptChange("First chat")
        viewModel.onSend()

        val oldId = viewModel.uiState.value.activeConversationId
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onNewChat()

        val newId = viewModel.uiState.value.activeConversationId
        assertTrue(oldId != newId)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSelectConversationSwitchesActiveConversationHistory() = runTest {
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
        )
        viewModel.onPromptChange("Message in chat 1")
        viewModel.onSend()

        val chat1Id = viewModel.uiState.value.activeConversationId

        viewModel.onNewChat()
        viewModel.onPromptChange("Message in chat 2")
        viewModel.onSend()

        viewModel.onSelectConversation(chat1Id)

        assertEquals(chat1Id, viewModel.uiState.value.activeConversationId)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Message in chat 1", viewModel.uiState.value.messages[0].text)
    }

    @Test
    fun onClearChatDeletesOnlyActiveConversation() = runTest {
        val viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
        )
        viewModel.onPromptChange("Chat A")
        viewModel.onSend()
        val chatAId = viewModel.uiState.value.activeConversationId

        viewModel.onNewChat()
        viewModel.onPromptChange("Chat B")
        viewModel.onSend()

        viewModel.onSelectConversation(chatAId)
        viewModel.onClearChat()

        val remainingMessages = repository.messages.value
        assertEquals(2, remainingMessages.size)
        assertEquals("Chat B", remainingMessages[0].text)
    }
}
