package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
) {

    val state by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    ChatScreen(
        state = state,

        onPromptChange =
            viewModel::onPromptChange,

        onSend =
            viewModel::onSend,

        onErrorShown =
            viewModel::clearError,

        onConciseRepliesChange =
            viewModel::onConciseRepliesChange,

        windowWidthSizeClass =
            windowWidthSizeClass,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onErrorShown: () -> Unit,
    onConciseRepliesChange: (Boolean) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
) {

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val listState =
        rememberLazyListState()

    /*
     * Responsive width.
     *
     * Compact = phones
     * Medium = large phones / small tablets
     * Expanded = tablets / large displays
     */
    val maxContentWidth =
        when (
            windowWidthSizeClass
        ) {

            WindowWidthSizeClass.Compact ->
                600.dp

            WindowWidthSizeClass.Medium ->
                700.dp

            else ->
                840.dp
        }

    /*
     * Speech-to-text.
     */
    val speechLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartActivityForResult()
        ) { result ->

            if (
                result.resultCode ==
                Activity.RESULT_OK
            ) {

                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(
                            RecognizerIntent
                                .EXTRA_RESULTS
                        )
                        ?.firstOrNull()

                if (
                    !spokenText
                        .isNullOrBlank()
                ) {

                    onPromptChange(
                        spokenText
                    )
                }
            }
        }

    /*
     * Snackbar error handling.
     */
    LaunchedEffect(
        state.errorMessage
    ) {

        state.errorMessage
            ?.let { message ->

                snackbarHostState
                    .showSnackbar(
                        message
                    )

                onErrorShown()
            }
    }

    /*
     * Auto-scroll to latest message.
     */
    LaunchedEffect(
        state.messages.size,
        state.isLoading,
    ) {

        val totalItems =
            state.messages.size +
                    if (
                        state.isLoading
                    ) {
                        1
                    } else {
                        0
                    }

        if (
            totalItems > 0
        ) {

            listState
                .animateScrollToItem(
                    totalItems - 1
                )
        }
    }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),

        snackbarHost = {

            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        },
    ) { innerPadding ->

        /*
         * Centers the chat on tablets
         * while phones use available width.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    ),

            contentAlignment =
                Alignment.TopCenter,
        ) {

            Column(
                modifier =
                    Modifier
                        .widthIn(
                            max =
                                maxContentWidth
                        )
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(
                            horizontal =
                                16.dp
                        )
            ) {

                /*
                 * Conversation.
                 */
                LazyColumn(
                    state =
                        listState,

                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),

                    verticalArrangement =
                        Arrangement
                            .spacedBy(
                                12.dp
                            ),

                    contentPadding =
                        PaddingValues(
                            vertical =
                                16.dp
                        ),
                ) {

                    if (
                        state.messages
                            .isEmpty() &&
                        !state.isLoading
                    ) {

                        item(
                            key =
                                "empty_message"
                        ) {

                            EmptyChatMessage()
                        }
                    }

                    items(
                        items =
                            state.messages,

                        key = {
                                message ->

                            message.id
                        },
                    ) {
                            message ->

                        ChatBubble(
                            message =
                                message
                        )
                    }

                    if (
                        state.isLoading
                    ) {

                        item(
                            key =
                                "gemini_loading"
                        ) {

                            GeminiLoadingBubble()
                        }
                    }
                }

                /*
                 * Persistent DataStore setting.
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top =
                                    4.dp,

                                bottom =
                                    4.dp,
                            ),

                    verticalAlignment =
                        Alignment
                            .CenterVertically,

                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,
                ) {

                    Text(
                        text =
                            "Concise replies",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )

                    Switch(
                        checked =
                            state
                                .conciseReplies,

                        onCheckedChange =
                            onConciseRepliesChange,

                        enabled =
                            !state
                                .isLoading,
                    )
                }

                /*
                 * Input bar.
                 */
                PromptBar(
                    prompt =
                        state.prompt,

                    promptError =
                        state.promptError,

                    enabled =
                        !state.isLoading,

                    onPromptChange =
                        onPromptChange,

                    onSend =
                        onSend,

                    onVoiceClick = {

                        val intent =
                            Intent(
                                RecognizerIntent
                                    .ACTION_RECOGNIZE_SPEECH
                            ).apply {

                                putExtra(
                                    RecognizerIntent
                                        .EXTRA_LANGUAGE_MODEL,

                                    RecognizerIntent
                                        .LANGUAGE_MODEL_FREE_FORM
                                )

                                putExtra(
                                    RecognizerIntent
                                        .EXTRA_PROMPT,

                                    "Speak your message"
                                )
                            }

                        speechLauncher
                            .launch(
                                intent
                            )
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
) {

    val isUser =
        message.sender ==
                MessageSender.USER

    Row(
        modifier =
            Modifier
                .fillMaxWidth(),

        horizontalArrangement =
            if (
                isUser
            ) {

                Arrangement.End

            } else {

                Arrangement.Start
            },

        verticalAlignment =
            Alignment.Bottom,
    ) {

        /*
         * Gemini icon.
         */
        if (
            !isUser
        ) {

            Icon(
                painter =
                    painterResource(
                        R.drawable
                            .ic_assistant
                    ),

                contentDescription =
                    "Gemini",

                modifier =
                    Modifier
                        .size(
                            32.dp
                        ),

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
            )

            Spacer(
                modifier =
                    Modifier
                        .width(
                            8.dp
                        )
            )
        }

        Surface(
            shape =
                RoundedCornerShape(

                    topStart =
                        18.dp,

                    topEnd =
                        18.dp,

                    bottomStart =
                        if (
                            isUser
                        ) {
                            18.dp
                        } else {
                            4.dp
                        },

                    bottomEnd =
                        if (
                            isUser
                        ) {
                            4.dp
                        } else {
                            18.dp
                        },
                ),

            color =
                if (
                    isUser
                ) {

                    MaterialTheme
                        .colorScheme
                        .primary

                } else {

                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                },

            contentColor =
                if (
                    isUser
                ) {

                    MaterialTheme
                        .colorScheme
                        .onPrimary

                } else {

                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },

            tonalElevation =
                2.dp,

            modifier =
                Modifier
                    .widthIn(
                        max =
                            340.dp
                    ),
        ) {

            Column(
                modifier =
                    Modifier
                        .padding(
                            horizontal =
                                16.dp,

                            vertical =
                                12.dp,
                        )
            ) {

                Text(
                    text =
                        if (
                            isUser
                        ) {

                            "You"

                        } else {

                            "Gemini"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        if (
                            isUser
                        ) {

                            MaterialTheme
                                .colorScheme
                                .onPrimary
                                .copy(
                                    alpha =
                                        0.75f
                                )

                        } else {

                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                )

                Spacer(
                    modifier =
                        Modifier
                            .size(
                                4.dp
                            )
                )

                if (
                    isUser
                ) {

                    Text(
                        text =
                            message.text,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,
                    )

                } else {

                    Text(
                        text =
                            message.text
                                .toBoldAnnotatedString(),

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiLoadingBubble() {

    Row(
        modifier =
            Modifier
                .fillMaxWidth(),

        horizontalArrangement =
            Arrangement.Start,

        verticalAlignment =
            Alignment
                .CenterVertically,
    ) {

        Icon(
            painter =
                painterResource(
                    R.drawable
                        .ic_assistant
                ),

            contentDescription =
                null,

            modifier =
                Modifier
                    .size(
                        32.dp
                    ),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary,
        )

        Spacer(
            modifier =
                Modifier
                    .width(
                        8.dp
                    )
        )

        Surface(
            shape =
                RoundedCornerShape(
                    18.dp
                ),

            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,

            tonalElevation =
                2.dp,
        ) {

            Row(
                modifier =
                    Modifier
                        .padding(
                            horizontal =
                                16.dp,

                            vertical =
                                12.dp,
                        ),

                verticalAlignment =
                    Alignment
                        .CenterVertically,
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(
                                20.dp
                            ),

                    strokeWidth =
                        2.dp,
                )

                Spacer(
                    modifier =
                        Modifier
                            .width(
                                12.dp
                            )
                )

                Text(
                    text =
                        "Gemini is thinking...",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun EmptyChatMessage() {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        40.dp
                ),

        horizontalAlignment =
            Alignment
                .CenterHorizontally,
    ) {

        Icon(
            painter =
                painterResource(
                    R.drawable
                        .ic_assistant
                ),

            contentDescription =
                null,

            modifier =
                Modifier
                    .size(
                        64.dp
                    ),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary,
        )

        Spacer(
            modifier =
                Modifier
                    .size(
                        12.dp
                    )
        )

        Text(
            text =
                "Start a conversation with Gemini",

            style =
                MaterialTheme
                    .typography
                    .titleMedium,
        )

        Spacer(
            modifier =
                Modifier
                    .size(
                        4.dp
                    )
        )

        Text(
            text =
                "Type or speak a message below.",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top =
                        8.dp,

                    bottom =
                        12.dp,
                ),

        verticalAlignment =
            Alignment
                .CenterVertically,
    ) {

        OutlinedTextField(
            value =
                prompt,

            onValueChange =
                onPromptChange,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .padding(
                        end =
                            8.dp
                    ),

            label = {

                Text(
                    stringResource(
                        R.string
                            .enter_your_prompt_here
                    )
                )
            },

            minLines =
                1,

            maxLines =
                5,

            enabled =
                enabled,

            isError =
                promptError != null,

            supportingText =
                if (
                    promptError != null
                ) {

                    {

                        Text(
                            stringResource(
                                R.string
                                    .field_cannot_be_empty
                            )
                        )
                    }

                } else {

                    null
                },
        )

        /*
         * Voice button.
         */
        FilledIconButton(
            onClick =
                onVoiceClick,

            enabled =
                enabled,
        ) {

            Text(
                text =
                    "🎤"
            )
        }

        Spacer(
            modifier =
                Modifier
                    .width(
                        8.dp
                    )
        )

        /*
         * Send button.
         */
        FilledIconButton(
            onClick =
                onSend,

            enabled =
                enabled,
        ) {

            Icon(
                imageVector =
                    Icons
                        .AutoMirrored
                        .Filled
                        .Send,

                contentDescription =
                    stringResource(
                        R.string
                            .send
                    ),
            )
        }
    }
}

@Preview(
    showBackground =
        true,

    showSystemUi =
        true,
)
@Composable
private fun ChatScreenPreview() {

    GeminiApiComposeStarterTheme {

        ChatScreen(
            state =
                ChatUiState(

                    conciseReplies =
                        true,

                    messages =
                        listOf(

                            ChatMessage(
                                id =
                                    1,

                                text =
                                    "Hello Gemini!",

                                sender =
                                    MessageSender.USER,
                            ),

                            ChatMessage(
                                id =
                                    2,

                                text =
                                    "**Hello!** How can I help you today?",

                                sender =
                                    MessageSender.GEMINI,
                            ),

                            ChatMessage(
                                id =
                                    3,

                                text =
                                    "Explain Round Robin scheduling.",

                                sender =
                                    MessageSender.USER,
                            ),
                        ),
                ),

            onPromptChange =
                {},

            onSend =
                {},

            onErrorShown =
                {},

            onConciseRepliesChange =
                {},

            windowWidthSizeClass =
                WindowWidthSizeClass
                    .Compact,
        )
    }
}