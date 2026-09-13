package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsMessagesAndInvokesActions() {
        var sendClicked = false
        var voiceClicked = false

        composeRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        activeChatId = "chat-1",
                        chatSummaries = listOf(ChatSummary("chat-1", "Compose")),
                        prompt = "Hello",
                        messages = listOf(
                            ChatMessage(1, "Hello", ChatAuthor.USER),
                            ChatMessage(2, "Hi from Gemini", ChatAuthor.GEMINI),
                        ),
                    ),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                    onVoiceInput = { voiceClicked = true },
                    onNewChat = {},
                    onSelectChat = {},
                    darkTheme = false,
                    onToggleTheme = {},
                )
            }
        }

        composeRule.onNodeWithText("Hello").assertIsDisplayed()
        composeRule.onNodeWithText("Hi from Gemini").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Use voice input").performClick()
        composeRule.onNodeWithContentDescription("Send").performClick()

        assertTrue(voiceClicked)
        assertTrue(sendClicked)
    }
}
