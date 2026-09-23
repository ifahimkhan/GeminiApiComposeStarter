package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.MessageParticipant
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyChat_showsWelcomePlaceholder() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini AI Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start a conversation by typing a prompt or using voice input.").assertIsDisplayed()
    }

    @Test
    fun chatMessages_renderedInLazyColumn() {
        val messages = listOf(
            ChatMessageUiModel(
                id = 1L,
                text = "Hello Gemini!",
                participant = MessageParticipant.USER,
            ),
            ChatMessageUiModel(
                id = 2L,
                text = "Hello! How can I help you today?",
                participant = MessageParticipant.MODEL,
            ),
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

        composeTestRule.onNodeWithTag("chat_message_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Gemini!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I help you today?").assertIsDisplayed()
    }

    @Test
    fun promptInput_triggersOnPromptChange() {
        var updatedPrompt = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = ""),
                    onPromptChange = { updatedPrompt = it },
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("prompt_text_field").performTextInput("Testing Compose")
        assertEquals("Testing Compose", updatedPrompt)
    }

    @Test
    fun sendButton_triggersOnSend() {
        var sendClicked = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Hello"),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("send_button").performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }

    @Test
    fun clearChatButton_visibleAndClickableWhenMessagesExist() {
        var clearChatClicked = false
        val messages = listOf(
            ChatMessageUiModel(
                id = 1L,
                text = "Hello",
                participant = MessageParticipant.USER,
            ),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = { clearChatClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear chat history").performClick()
        assertTrue(clearChatClicked)
    }
}
