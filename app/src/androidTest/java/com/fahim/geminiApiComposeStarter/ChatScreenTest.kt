package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.N088Assignment1Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun conversationShowsBothAuthorsAndSendAction() {
        var sends = 0
        composeRule.setContent {
            N088Assignment1Theme {
                ChatScreen(
                    state = ChatUiState(
                        prompt = "Another question",
                        messages = listOf(
                            ChatMessage("1", "First question", true),
                            ChatMessage("2", "First answer", false)
                        )
                    ),
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {},
                    onSend = { sends++ },
                    onErrorShown = {},
                    onCycleTheme = {},
                    onClearHistory = {},
                    onVoiceRequested = {}
                )
            }
        }
        composeRule.onNodeWithText("First question").assertExists()
        composeRule.onNodeWithText("First answer").assertExists()
        composeRule.onNodeWithText("Send").performClick()
        assertEquals(1, sends)
    }
}
