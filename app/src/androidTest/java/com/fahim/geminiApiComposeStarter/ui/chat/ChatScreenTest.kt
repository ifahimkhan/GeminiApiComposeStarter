package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.mascot.MascotState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val conversation = listOf(
        ChatMessage(id = 1, text = "What is Kotlin?", author = Author.USER),
        ChatMessage(id = 2, text = "Kotlin is a JVM language.", author = Author.GEMINI),
    )

    private fun setScreen(
        state: ChatUiState,
        onPromptChange: (String) -> Unit = {},
        onSend: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                )
            }
        }
    }

    @Test
    fun bothSidesOfTheConversationAreRendered() {
        setScreen(ChatUiState(messages = conversation))

        composeTestRule.onNodeWithText("What is Kotlin?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin is a JVM language.").assertIsDisplayed()
    }

    @Test
    fun emptyConversationShowsThePlaceholder() {
        setScreen(ChatUiState())

        composeTestRule
            .onNodeWithText("Ask Ahum anything to start the conversation.")
            .assertIsDisplayed()
    }

    @Test
    fun typingAPromptReportsEveryChange() {
        val typed = mutableListOf<String>()
        setScreen(ChatUiState(), onPromptChange = { typed += it })

        composeTestRule.onNodeWithTag(ChatTestTags.PROMPT_FIELD).performTextInput("Hi")

        assertTrue(typed.isNotEmpty())
        assertEquals("Hi", typed.last())
    }

    @Test
    fun tappingSendInvokesTheCallback() {
        var sends = 0
        setScreen(ChatUiState(prompt = "Hello"), onSend = { sends++ })

        composeTestRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).performClick()

        assertEquals(1, sends)
    }

    @Test
    fun loadingShowsTheTypingIndicatorAndDisablesSend() {
        setScreen(
            ChatUiState(
                messages = conversation,
                isLoading = true,
                mascotState = MascotState.THINKING,
            ),
        )

        composeTestRule.onNodeWithTag(ChatTestTags.TYPING_INDICATOR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun theMascotIsShownAboveTheConversation() {
        setScreen(ChatUiState(messages = conversation))

        composeTestRule.onNodeWithTag(ChatTestTags.MASCOT).assertIsDisplayed()
    }

    @Test
    fun anEmptyFieldErrorIsSurfacedUnderTheInput() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))

        composeTestRule.onNodeWithText("Field cannot be empty").assertIsDisplayed()
    }
}
