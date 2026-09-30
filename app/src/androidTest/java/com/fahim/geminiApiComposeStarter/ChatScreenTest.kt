package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatSession
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyStateDisplaysHeadlineAndSuggestions() {
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
        composeTestRule.onNodeWithText("Explain a concept").assertIsDisplayed()
    }

    @Test
    fun sendButtonIsDisabledWhenPromptIsBlank() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "   "),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun chatMessagesRenderUserAndModelBubbles() {
        val testMessages = listOf(
            ChatMessage(text = "Hello Gemini!", participant = Participant.USER),
            ChatMessage(text = "Hello user! How can I help?", participant = Participant.MODEL),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello user! How can I help?").assertIsDisplayed()
    }

    @Test
    fun thinkingIndicatorDisplaysWhenLoading() {
        val testMessages = listOf(
            ChatMessage(text = "Thinking prompt", participant = Participant.USER),
        )
        val activeId = "conv_1"

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        activeConversationId = activeId,
                        messages = testMessages,
                        loadingConversationId = activeId,
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun expandedLayoutShowsHistoryPane() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 800.dp))
        val sessions = listOf(
            ChatSession(id = "conv_1", title = "Side Pane Conversation Title"),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        activeConversationId = "conv_1",
                        conversations = sessions,
                    ),
                    windowSizeClass = windowSizeClass,
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Side Pane Conversation Title").assertIsDisplayed()
    }

    @Test
    fun snackbarDisplaysErrorMessage() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = emptyList(),
                        errorMessage = "Network connection failed",
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Network connection failed").assertIsDisplayed()
    }
}
