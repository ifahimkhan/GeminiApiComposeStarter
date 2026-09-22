package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.MessageSender
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_rendersEmptyStateAndInputBar() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onSelectTone = {},
                    onDismissError = {},
                )
            }
        }

        // Empty state welcome text
        composeTestRule.onNodeWithText("Welcome to Gemini AI").assertIsDisplayed()
        // Input bar placeholder
        composeTestRule.onNodeWithText("Ask Gemini anything…").assertIsDisplayed()
        // Voice mic button
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        // Send button
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun chatScreen_typingInPrompt_triggersCallback() {
        var updatedPrompt = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = updatedPrompt),
                    onPromptChange = { updatedPrompt = it },
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onSelectTone = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ask Gemini anything…")
            .performTextInput("What is Compose?")

        assertEquals("What is Compose?", updatedPrompt)
    }

    @Test
    fun chatScreen_displaysUserAndGeminiChatBubbles() {
        val messages = listOf(
            ChatMessage(sender = MessageSender.USER, text = "User message text"),
            ChatMessage(sender = MessageSender.GEMINI, text = "Gemini response text"),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onSelectTone = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("User message text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini response text").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsLoadingIndicator_whenLoading() {
        val messages = listOf(
            ChatMessage(sender = MessageSender.USER, text = "Thinking question"),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages, isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearHistory = {},
                    onSelectTone = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading indicator").assertIsDisplayed()
    }

    @Test
    fun chatScreen_clickingSend_triggersOnSend() {
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Ready to send"),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                    onVoiceInput = {},
                    onClearHistory = {},
                    onSelectTone = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").performClick()
        assertTrue(sendClicked)
    }
}
