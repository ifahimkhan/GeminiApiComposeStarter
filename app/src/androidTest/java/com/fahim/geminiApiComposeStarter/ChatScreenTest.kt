package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.db.ChatMessageEntity
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

        composeTestRule.onNodeWithText("Response will be displayed here!")
            .assertIsDisplayed()
    }

    @Test
    fun displaysUserAndAssistantMessages() {
        val messages = listOf(
            ChatMessageEntity(id = 1, text = "Hello Gemini", isUser = true),
            ChatMessageEntity(id = 2, text = "Hello Human!", isUser = false),
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

        composeTestRule.onNodeWithText("Enter your prompt here")
            .performTextInput("Testing prompt input")
    }
}
