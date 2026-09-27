package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: ChatUiState,
        onInputChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
        onSuggestionSelected: (String) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onInputChange = onInputChange,
                    onSend = onSend,
                    onSuggestionSelected = onSuggestionSelected,
                    onRetry = onRetry,
                )
            }
        }
    }

    @Test
    fun emptyState_showsWelcomeMessageAndSuggestions() {
        setScreen(state = ChatUiState())

        composeTestRule.onNodeWithText("Hello, how can I help?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain something").assertIsDisplayed()
    }

    @Test
    fun tappingSuggestion_sendsThePromptBehindTheLabel() {
        var selectedPrompt: String? = null
        setScreen(state = ChatUiState(), onSuggestionSelected = { selectedPrompt = it })

        composeTestRule.onNodeWithText("Help me study").performClick()

        assertEquals("Quiz me on the French Revolution", selectedPrompt)
    }

    @Test
    fun messages_areRenderedInTheList() {
        val messages = listOf(
            ChatMessage(id = 1, text = "Hi", isFromUser = true),
            ChatMessage(id = 2, text = "Hello! How can I help?", isFromUser = false),
        )
        setScreen(state = ChatUiState(messages = messages))

        composeTestRule.onNodeWithText("Hi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I help?").assertIsDisplayed()
    }

    @Test
    fun inputField_reportsTypedTextThroughCallback() {
        var latestText = ""
        setScreen(state = ChatUiState(), onInputChange = { latestText = it })

        composeTestRule.onNodeWithTag("chat_input_field").performTextInput("Hello there")

        assertEquals("Hello there", latestText)
    }

    @Test
    fun sendButton_isDisabledWhenInputIsEmpty() {
        setScreen(state = ChatUiState(inputText = ""))

        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }

    @Test
    fun errorState_showsMessageAndRetryTriggersCallback() {
        var retried = false
        setScreen(
            state = ChatUiState(errorMessage = "Something went wrong", canRetry = true),
            onRetry = { retried = true },
        )

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText(" Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun loadingState_showsExistingMessagesAndDisablesInput() {
        setScreen(
            state = ChatUiState(
                messages = listOf(ChatMessage(id = 1, text = "Hi", isFromUser = true)),
                isLoading = true,
            ),
        )

        composeTestRule.onNodeWithText("Hi").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }
}
