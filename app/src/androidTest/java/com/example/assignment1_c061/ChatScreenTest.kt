package com.example.assignment1_c061

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.assignment1_c061.ui.chat.ChatMessage
import com.example.assignment1_c061.ui.chat.ChatScreen
import com.example.assignment1_c061.ui.chat.ChatUiState
import com.example.assignment1_c061.ui.chat.MessageSender
import com.example.assignment1_c061.ui.theme.Assignment1C061Theme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyStateDisplaysPlaceholder() {
        composeTestRule.setContent {
            Assignment1C061Theme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Response will be displayed here!").assertIsDisplayed()
    }

    @Test
    fun messagesRenderInChatBubbles() {
        val messages = listOf(
            ChatMessage(sender = MessageSender.USER, text = "User test message"),
            ChatMessage(sender = MessageSender.GEMINI, text = "Gemini test message")
        )

        composeTestRule.setContent {
            Assignment1C061Theme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("User test message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini test message").assertIsDisplayed()
    }

    @Test
    fun typingInTextFieldCallsOnPromptChange() {
        var typedText = ""

        composeTestRule.setContent {
            Assignment1C061Theme {
                ChatScreen(
                    state = ChatUiState(prompt = typedText),
                    onPromptChange = { typedText = it },
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Hello")
        assert(typedText == "Hello")
    }
}
