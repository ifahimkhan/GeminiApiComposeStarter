package com.nmims.c010.gemini.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nmims.c010.gemini.data.Author
import com.nmims.c010.gemini.data.ChatMessage
import com.nmims.c010.gemini.ui.theme.GeminiTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(state: ChatUiState, onSend: () -> Unit = {}) {
        composeTestRule.setContent {
            GeminiTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onInputChange = {},
                    onSend = onSend,
                    onVoiceResult = {},
                    onRetry = {},
                    onErrorShown = {}
                )
            }
        }
    }

    @Test
    fun emptyState_showsPlaceholderPrompt() {
        setScreen(ChatUiState())
        composeTestRule.onNodeWithText("Start a conversation").assertIsDisplayed()
    }

    @Test
    fun messages_areRendered() {
        setScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(text = "Hi", author = Author.USER),
                    ChatMessage(text = "Hello!", author = Author.MODEL)
                )
            )
        )
        composeTestRule.onNodeWithText("Hi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello!").assertIsDisplayed()
    }

    @Test
    fun progressIndicator_isShownWhileLoading() {
        setScreen(
            ChatUiState(
                messages = listOf(ChatMessage(text = "Hi", author = Author.USER)),
                isLoading = true
            )
        )
        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }

    @Test
    fun sendButton_isDisabledForBlankInput() {
        setScreen(ChatUiState(input = ""))
        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }
}
