package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import com.fahim.geminiApiComposeStarter.data.local.ChatMessage
import com.fahim.geminiApiComposeStarter.data.local.MessageAuthor
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chatScreen_rendersConversationAndActions() {
        composeRule.setContent {
            GeminiApiComposeStarterTheme(dynamicColor = false) {
                ChatScreen(
                    state = ChatUiState(
                        displayName = "Student",
                        messages = listOf(
                            ChatMessage(
                                id = 1,
                                author = MessageAuthor.USER,
                                text = "Hello Gemini",
                                timestamp = 1_700_000_000_000,
                            ),
                            ChatMessage(
                                id = 2,
                                author = MessageAuthor.GEMINI,
                                text = "Hello Student",
                                timestamp = 1_700_000_001_000,
                            ),
                        ),
                    ),
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onErrorConsumed = {},
                    onOpenSettings = {},
                    onSettingsNameChange = {},
                    onDismissSettings = {},
                    onSaveSettings = {},
                    onClearHistory = {},
                )
            }
        }

        composeRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Hello Student").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }
}
