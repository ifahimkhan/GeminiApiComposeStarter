package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.db.ConversationMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysPlaceholderWhenMessagesEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onVoiceResult = {},
                    onSend = {},
                    onRetry = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini AI")
            .assertIsDisplayed()
    }

    @Test
    fun displaysUserAndAssistantMessages() {
        val messages = listOf(
            ConversationMessage(id = 1, content = "Hello Gemini", isUserSent = true),
            ConversationMessage(id = 2, content = "Hello Human!", isUserSent = false),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onVoiceResult = {},
                    onSend = {},
                    onRetry = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Human!").assertIsDisplayed()
    }

    @Test
    fun allowsPromptEntryInTextField() {
        var enteredText = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onVoiceResult = {},
                    onSend = {},
                    onRetry = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ask Gemini anything…")
            .performTextInput("Testing prompt input")
    }
}
