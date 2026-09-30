package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageSender
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysWelcomeMessage() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("How can I help you today?").assertIsDisplayed()
    }

    @Test
    fun conversationState_displaysUserAndGeminiMessages() {
        val messages = listOf(
            ChatMessageEntity(id = 1, text = "Hello Gemini", sender = MessageSender.USER),
            ChatMessageEntity(id = 2, text = "Hello! How can I assist you?", sender = MessageSender.GEMINI),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I assist you?").assertIsDisplayed()
    }

    @Test
    fun promptInput_displaysCurrentPromptText() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "My test question"),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("My test question").assertIsDisplayed()
    }

    @Test
    fun loadingState_displaysThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @Test
    fun errorState_displaysEmptyPromptError() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(promptError = com.fahim.geminiApiComposeStarter.ui.chat.PromptError.EMPTY),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Please enter a prompt").assertIsDisplayed()
    }
}
