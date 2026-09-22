package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysMessages_andPromptBar() {
        val testMessages = listOf(
            ChatMessage(text = "Hello Gemini", isUser = true),
            ChatMessage(text = "Hello! How can I help?", isUser = false)
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I help?").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysLoadingState() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }
}
