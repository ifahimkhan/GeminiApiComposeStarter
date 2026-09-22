package com.silas.geminichat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.silas.geminichat.model.ChatMessage
import com.silas.geminichat.model.MessageRole
import com.silas.geminichat.ui.chat.ChatScreen
import com.silas.geminichat.ui.chat.ChatUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compose UI tests for the chat screen. */
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setChatScreen(state: ChatUiState, onSend: () -> Unit = {}) {
        composeRule.setContent {
            ChatScreen(
                state = state,
                windowWidthSizeClass = WindowWidthSizeClass.Compact,
                onPromptChange = {},
                onSend = onSend,
                onVoiceResult = {},
                onClearHistory = {},
                onUserNameChange = {},
            )
        }
    }

    @Test
    fun emptyStateShowsPlaceholder() {
        setChatScreen(ChatUiState())
        composeRule.onNodeWithText("Ask Gemini anything").assertIsDisplayed()
    }

    @Test
    fun messageBubblesAreDisplayed() {
        setChatScreen(
            ChatUiState(
                messages = listOf(
                    ChatMessage(id = 1, role = MessageRole.USER, content = "Hello"),
                    ChatMessage(id = 2, role = MessageRole.ASSISTANT, content = "Hi there"),
                )
            )
        )
        composeRule.onNodeWithText("Hello").assertIsDisplayed()
        composeRule.onNodeWithText("Hi there").assertIsDisplayed()
    }

    @Test
    fun sendButtonInvokesCallback() {
        var sent = false
        setChatScreen(ChatUiState(prompt = "Hello"), onSend = { sent = true })
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.runOnIdle { assertTrue(sent) }
    }
}
