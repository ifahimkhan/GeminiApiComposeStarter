package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysGreeting() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(messages = emptyList(), isLoading = false),
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ask a question, generate ideas, or explore concepts with AI.").assertIsDisplayed()
    }

    @Test
    fun messages_displayedOnScreen() {
        val testMessages = listOf(
            ChatMessage(id = "1", text = "How are you?", participant = Participant.USER),
            ChatMessage(id = "2", text = "I am doing great!", participant = Participant.ASSISTANT),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(messages = testMessages),
                )
            }
        }

        composeTestRule.onNodeWithText("How are you?").assertIsDisplayed()
        composeTestRule.onNodeWithText("I am doing great!").assertIsDisplayed()
    }

    @Test
    fun loadingState_displaysThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(
                        messages = listOf(ChatMessage(id = "1", text = "Hello", participant = Participant.USER)),
                        isLoading = true,
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @Test
    fun sendButton_disabledWhenPromptIsBlank() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(prompt = "   ", isLoading = false),
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun sendButton_enabledWhenPromptIsNotBlank() {
        var sendClicked = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(prompt = "Valid prompt", isLoading = false),
                    onSend = { sendClicked = true },
                )
            }
        }

        val sendNode = composeTestRule.onNodeWithContentDescription("Send")
        sendNode.assertIsEnabled()
        sendNode.performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun voiceButton_isDisplayedAndClickable() {
        var voiceClicked = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(isLoading = false),
                    onVoiceClick = { voiceClicked = true },
                )
            }
        }

        val voiceNode = composeTestRule.onNodeWithContentDescription("Voice input")
        voiceNode.assertIsDisplayed()
        voiceNode.performClick()
        assertTrue(voiceClicked)
    }

    @Test
    fun clearChatButton_visibleWhenMessagesPresent_opensConfirmationDialog() {
        var clearConfirmed = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(
                        messages = listOf(ChatMessage(id = "1", text = "Msg", participant = Participant.USER)),
                    ),
                    onClearChat = { clearConfirmed = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear chat history").performClick()
        composeTestRule.onNodeWithText("Clear Conversation?").assertIsDisplayed()

        composeTestRule.onNodeWithText("Clear").performClick()
        assertTrue(clearConfirmed)
    }

    @Test
    fun themeSelector_isDisplayedInTopAppBar() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    uiState = ChatUiState(),
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Change theme").assertIsDisplayed()
    }
}
