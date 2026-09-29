package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSession
import com.fahim.geminiApiComposeStarter.ui.chat.ChatContent
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_rendersEmptyState_andTopBar() {
        val uiState = ChatUiState(
            sessions = listOf(ChatSession(title = "New Chat")),
            currentSessionId = "test-session",
            messages = emptyList(),
            selectedModel = "gemini-3.8-flash",
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatContent(
                    uiState = uiState,
                    onPromptChange = {},
                    onSend = {},
                    onNewChat = {},
                    onClearChat = {},
                    onSwitchSession = {},
                    onDeleteSession = {},
                    onSelectModel = {},
                    onShowModelDialog = {},
                    onShowAttachmentPicker = {},
                    onAddAttachments = {},
                    onRemoveAttachment = {},
                )
            }
        }

        // Verify Top Bar Title
        composeTestRule.onNodeWithText("Gemini Chat").assertIsDisplayed()

        // Verify Model Selection Pill
        composeTestRule.onNodeWithText("gemini-3.8-flash").assertIsDisplayed()

        // Verify Empty State Welcome text
        composeTestRule.onNodeWithText("How can I help you today?").assertIsDisplayed()
    }

    @Test
    fun chatScreen_rendersConversationMessages() {
        val messages = listOf(
            ChatMessage(text = "Hello Gemini!", isUser = true),
            ChatMessage(text = "Hello! How can I assist you?", isUser = false),
        )
        val uiState = ChatUiState(
            sessions = listOf(ChatSession(title = "Test Session", messages = messages)),
            currentSessionId = "test-session",
            messages = messages,
            selectedModel = "gemini-3.8-flash",
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatContent(
                    uiState = uiState,
                    onPromptChange = {},
                    onSend = {},
                    onNewChat = {},
                    onClearChat = {},
                    onSwitchSession = {},
                    onDeleteSession = {},
                    onSelectModel = {},
                    onShowModelDialog = {},
                    onShowAttachmentPicker = {},
                    onAddAttachments = {},
                    onRemoveAttachment = {},
                )
            }
        }

        // Verify User message is rendered
        composeTestRule.onNodeWithText("Hello Gemini!").assertIsDisplayed()

        // Verify Gemini message is rendered
        composeTestRule.onNodeWithText("Hello! How can I assist you?").assertIsDisplayed()
    }

    @Test
    fun chatScreen_typingAndSendingTriggersCallbacks() {
        var enteredText = ""
        var sendClicked = false

        val uiState = ChatUiState(
            sessions = listOf(ChatSession(title = "New Chat")),
            currentSessionId = "test-session",
            prompt = enteredText,
            messages = emptyList(),
            selectedModel = "gemini-3.8-flash",
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatContent(
                    uiState = uiState,
                    onPromptChange = { enteredText = it },
                    onSend = { sendClicked = true },
                    onNewChat = {},
                    onClearChat = {},
                    onSwitchSession = {},
                    onDeleteSession = {},
                    onSelectModel = {},
                    onShowModelDialog = {},
                    onShowAttachmentPicker = {},
                    onAddAttachments = {},
                    onRemoveAttachment = {},
                )
            }
        }

        val inputField = composeTestRule.onNodeWithText("Ask Gemini anything...")
        inputField.performTextInput("What is Compose?")
        assertEquals("What is Compose?", enteredText)

        val sendButton = composeTestRule.onNodeWithContentDescription("Send message")
        sendButton.performClick()
        assertTrue(sendClicked)
    }
}
