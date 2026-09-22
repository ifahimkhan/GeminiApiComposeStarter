package com.vaishnavi.n085.geminichat.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaishnavi.n085.geminichat.data.local.Message
import com.vaishnavi.n085.geminichat.data.local.MessageRole
import com.vaishnavi.n085.geminichat.ui.theme.VaishnaviChatTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        state: ChatUiState,
        onSend: () -> Unit = {},
    ) {
        composeRule.setContent {
            VaishnaviChatTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {},
                    onVoiceInput = {},
                    onSend = onSend,
                    onClearConversation = {},
                    onUserNameChange = {},
                    onDismissError = {},
                )
            }
        }
    }

    @Test
    fun emptyConversation_showsPlaceholder() {
        setContent(ChatUiState(userName = "Vaishnavi"))
        composeRule.onNodeWithText("Start a conversation with Gemini").assertIsDisplayed()
    }

    @Test
    fun messages_areDisplayed() {
        setContent(
            ChatUiState(
                messages = listOf(
                    Message(id = 1, role = MessageRole.USER, content = "Hello!"),
                    Message(id = 2, role = MessageRole.ASSISTANT, content = "Hi there!"),
                ),
                userName = "Vaishnavi",
            )
        )
        composeRule.onNodeWithText("Hello!").assertIsDisplayed()
        composeRule.onNodeWithText("Hi there!").assertIsDisplayed()
    }

    @Test
    fun sendButton_triggersCallback() {
        var sent = false
        setContent(ChatUiState(userName = "Vaishnavi"), onSend = { sent = true })
        composeRule.onNodeWithContentDescription("Send").performClick()
        assertTrue(sent)
    }
}
