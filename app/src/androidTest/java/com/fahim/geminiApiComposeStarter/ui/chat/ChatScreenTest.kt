package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun chatScreen_rendersSuccessfully() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        // Verify root chat screen and top app bar title are displayed
        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_CHAT_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini AI Chat").assertIsDisplayed()
        // Verify empty state is displayed
        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_EMPTY_VIEW).assertIsDisplayed()
    }

    @Test
    fun inputField_isVisibleAndAcceptsInput() {
        var updatedPrompt = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = updatedPrompt),
                    onPromptChange = { updatedPrompt = it },
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        val inputNode = composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_INPUT_FIELD)
        inputNode.assertIsDisplayed()
        inputNode.performTextInput("Hello Gemini")
        assertEquals("Hello Gemini", updatedPrompt)
    }

    @Test
    fun sendButton_isAvailableAndEnabledWhenTextPresent() {
        var sendClicked = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Explain quantum computing"),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        val sendButton = composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_SEND_BUTTON)
        sendButton.assertIsDisplayed()
        sendButton.assertIsEnabled()
        sendButton.performClick()
        assertTrue("Send callback should be triggered on click", sendClicked)
    }

    @Test
    fun sendButton_isDisabledWhenPromptIsEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = ""),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_SEND_BUTTON)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun messages_areDisplayedInConversationList() {
        val messages = listOf(
            ChatMessage(text = "What is Jetpack Compose?", participant = Participant.USER),
            ChatMessage(text = "Compose is a modern UI toolkit for Android.", participant = Participant.MODEL),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        // Conversation list is displayed
        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_CONVERSATION_LIST).assertIsDisplayed()

        // User message and label are displayed
        composeTestRule.onNodeWithText("What is Jetpack Compose?").assertIsDisplayed()
        composeTestRule.onNodeWithText("User").assertIsDisplayed()

        // Gemini message and label are displayed
        composeTestRule.onNodeWithText("Compose is a modern UI toolkit for Android.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini").assertIsDisplayed()
    }

    @Test
    fun loadingState_isDisplayedWhenAppropriate() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(text = "Tell me a story", participant = Participant.USER)
                        ),
                        isLoading = true,
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        // Verify loading indicator bubble and "Thinking..." text are displayed
        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_LOADING_INDICATOR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Thinking...").assertIsDisplayed()
    }

    @Test
    fun errorState_isDisplayedWhenAppropriate_withRetryAndDismiss() {
        var retryClicked = false
        var dismissClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        errorMessage = "Rate limit exceeded. Please try again.",
                        canRetry = true,
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = { retryClicked = true },
                    onDismissError = { dismissClicked = true },
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        // Verify error card and error text are displayed
        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_ERROR_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText("Rate limit exceeded. Please try again.").assertIsDisplayed()

        // Verify Retry button is displayed and triggers callback
        val retryButton = composeTestRule.onNodeWithText("Retry")
        retryButton.assertIsDisplayed()
        retryButton.performClick()
        assertTrue("Retry callback should be called", retryClicked)

        // Verify Dismiss button is displayed and triggers callback
        val dismissButton = composeTestRule.onNodeWithText("Dismiss")
        dismissButton.assertIsDisplayed()
        dismissButton.performClick()
        assertTrue("Dismiss callback should be called", dismissClicked)
    }

    @Test
    fun voiceInputButton_isPresentAndEnabled() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        // Verify voice-input button is present by tag and content description
        composeTestRule.onNodeWithTag(ChatScreenTestTags.TAG_MIC_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()

        composeTestRule.onNodeWithContentDescription("Voice input")
            .assertIsDisplayed()
    }

    @Test
    fun promptError_displaysSupportingErrorMessage() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(promptError = PromptError.EMPTY),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = {},
                    onDismissError = {},
                    onSpeechResult = {},
                    onSpeechError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Field cannot be empty").assertIsDisplayed()
    }
}
