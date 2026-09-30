package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysMessagesAndPrompt() {
        var promptChanged = false
        var sent = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage("1", "Hello there", true)
                        ),
                        prompt = "Typing...",
                        userName = "TestUser"
                    ),
                    onPromptChange = { promptChanged = true },
                    onSend = { sent = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Hello there").assertExists()
        composeTestRule.onNodeWithText("Typing...").assertExists()
        composeTestRule.onNodeWithText("TestUser").assertExists()
        
        // The send button has description "Send" which maps to the auto-mirrored icon
        // Let's just test that the text field can be interacted with
        composeTestRule.onNodeWithText("Typing...").performTextInput(" more")
        assert(promptChanged)
    }
}