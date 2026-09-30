package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyConversation_hidesClearButton_and_showsEmptyStateContent() {
        val clearDesc = context.getString(R.string.clear_conversation)
        val placeholder = context.getString(R.string.response_placeholder)

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(clearDesc).assertDoesNotExist()
        composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
    }

    @Test
    fun nonEmptyConversation_showsClearButton_withLocalizedDescription() {
        val clearDesc = context.getString(R.string.clear_conversation)

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(text = "Hello", participant = Participant.USER),
                        ),
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(clearDesc).assertIsDisplayed()
    }

    @Test
    fun loadingState_disablesPromptSendAndMicControls() {
        val sendDesc = context.getString(R.string.send)
        val voiceDesc = context.getString(R.string.voice_input)

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        isLoading = true,
                        prompt = "Pending prompt",
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(sendDesc).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(voiceDesc).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Pending prompt").assertIsNotEnabled()
    }

    @Test
    fun responsiveConversationContainer_respects840dpMaximum_onWideConfiguration() {
        val placeholder = context.getString(R.string.response_placeholder)

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                Box(modifier = Modifier.size(width = 1200.dp, height = 800.dp)) {
                    ChatScreen(
                        state = ChatUiState(messages = emptyList()),
                        onPromptChange = {},
                        onSend = {},
                    )
                }
            }
        }

        val bounds = composeTestRule.onNodeWithText(placeholder).getUnclippedBoundsInRoot()
        assertTrue(
            "Expected width inside container <= 840.dp, but was ${bounds.width}",
            bounds.width <= 840.dp,
        )
    }
}
