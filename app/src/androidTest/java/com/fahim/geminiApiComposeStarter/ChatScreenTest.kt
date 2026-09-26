package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysMessagesCorrectly() {
        val testMessages = listOf(
            ChatMessage(id = 1, text = "Hello Gemini", isUser = true),
            ChatMessage(id = 2, text = "Hello Sudhanshu! How can I help?", isUser = false)
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Sudhanshu! How can I help?").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysLoadingIndicatorWhenLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }
}
