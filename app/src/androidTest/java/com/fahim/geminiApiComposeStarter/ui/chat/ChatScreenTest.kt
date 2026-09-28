package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.MessageSender
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_isDisplayedWhenNoMessages() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_state_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_text_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
    }

    @Test
    fun messageList_displaysUserAndGeminiMessages() {
        val messages = listOf(
            ChatMessage(
                id = 1L,
                text = "Hello, what is Jetpack Compose?",
                sender = MessageSender.USER,
                status = MessageStatus.SUCCESS,
            ),
            ChatMessage(
                id = 2L,
                text = "Jetpack Compose is Android's recommended modern toolkit for building native UI.",
                sender = MessageSender.GEMINI,
                status = MessageStatus.SUCCESS,
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

        composeTestRule.onNodeWithTag("messages_lazy_column").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello, what is Jetpack Compose?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jetpack Compose is Android's recommended modern toolkit for building native UI.").assertIsDisplayed()
    }

    @Test
    fun promptInput_triggersCallback() {
        var enteredText = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("prompt_text_field").performTextInput("Explain coroutines")
        assert(enteredText == "Explain coroutines")
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
        assert(sendClicked)
    }

    @Test
    fun retryBanner_isDisplayedWhenCanRetry() {
        var retryClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        canRetry = true,
                        lastFailedPrompt = "Failed prompt",
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onRetry = { retryClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("retry_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_button").performClick()
        assert(retryClicked)
    }

    @Test
    fun studentInfoDialog_displaysStudentDetails() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(showStudentInfoDialog = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Student Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tanmay Sharma").assertIsDisplayed()
        composeTestRule.onNodeWithText("N159").assertIsDisplayed()
    }

    @Test
    fun clearConfirmDialog_isDisplayedWhenRequested() {
        var clearConfirmed = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(showClearConfirmDialog = true),
                    onPromptChange = {},
                    onSend = {},
                    onClearChatConfirm = { clearConfirmed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Clear Chat History").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_confirm_button").performClick()
        assert(clearConfirmed)
    }
}
